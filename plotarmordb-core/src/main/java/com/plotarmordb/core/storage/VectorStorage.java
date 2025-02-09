package com.plotarmordb.core.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plotarmordb.core.model.Vector;
import com.plotarmordb.core.config.StorageConfig;
import com.plotarmordb.core.exception.StorageException;

import com.plotarmordb.core.search.VectorMath;
import org.rocksdb.*;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class VectorStorage implements AutoCloseable {
    private RocksDB db;
    private final ObjectMapper objectMapper;
    private final StorageConfig config;
    private final ReadWriteLock lock;
    private final Options options;
    private final WriteOptions writeOptions;
    private final ReadOptions readOptions;

    public VectorStorage(StorageConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.lock = new ReentrantReadWriteLock();
        this.options = createOptions();
        this.writeOptions = new WriteOptions().setSync(true);
        this.readOptions = new ReadOptions().setVerifyChecksums(true);
        initialize();
    }

    private Options createOptions() {
        Options options = new Options()
                .setCreateIfMissing(true)
                .setWriteBufferSize(config.getWriteBufferSize())
                .setMaxBackgroundCompactions(config.getMaxBackgroundJobs())
                .setMaxBackgroundFlushes(config.getMaxBackgroundJobs())
                .setLevelZeroFileNumCompactionTrigger(4)
                .setLevelZeroSlowdownWritesTrigger(8)
                .setLevelZeroStopWritesTrigger(12);

        if (config.isCompressionEnabled()) {
            options.setCompressionType(CompressionType.LZ4_COMPRESSION);
        }

        return options;
    }

    private void initialize() {
        try {
            RocksDB.loadLibrary();
            Files.createDirectories(Path.of(config.getDbPath()));

            lock.writeLock().lock();
            try {
                db = RocksDB.open(options, config.getDbPath());
            } finally {
                lock.writeLock().unlock();
            }
        } catch (Exception e) {
            throw new StorageException("Failed to initialize storage", e);
        }
    }

    public void store(Vector vector) {
        validateVector(vector);

        if(vector.getValues().length < 10000)
            addPadding(vector);

        lock.writeLock().lock();
        try {
            byte[] key = vector.getId().getBytes();
            byte[] value = objectMapper.writeValueAsBytes(vector);
            db.put(writeOptions, key, value);
        } catch (Exception e) {
            throw new StorageException("Failed to store vector: " + vector.getId(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void addPadding(Vector vector) {
        float[] embedding = new float[10000];
        int index = 0;
        for (float value : vector.getValues()) {
            if (index < 10000) {
                embedding[index++] = value;
            }
        }
        // Normalize the embedding vector
        VectorMath.normalizeVector(embedding);
        vector.setValues(embedding);
    }

    public void storeBatch(List<Vector> vectors) {
        if (vectors == null || vectors.isEmpty()) {
            return;
        }

        lock.writeLock().lock();
        try (WriteBatch batch = new WriteBatch()) {
            for (Vector vector : vectors) {
                validateVector(vector);
                if(vector.getValues().length < 10000)
                    addPadding(vector);
                byte[] key = vector.getId().getBytes();
                byte[] value = objectMapper.writeValueAsBytes(vector);
                batch.put(key, value);
            }
            db.write(writeOptions, batch);
        } catch (Exception e) {
            throw new StorageException("Failed to store vector batch", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<Vector> retrieve(String id) {
        if (id == null) {
            return Optional.empty();
        }

        lock.readLock().lock();
        try {
            byte[] key = id.getBytes();
            byte[] value = db.get(readOptions, key);

            if (value == null) {
                return Optional.empty();
            }

            return Optional.of(objectMapper.readValue(value, Vector.class));
        } catch (Exception e) {
            throw new StorageException("Failed to retrieve vector: " + id, e);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Vector> retrieveBatch(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        lock.readLock().lock();
        try {
            List<Vector> results = new ArrayList<>();
            for (String id : ids) {
                Optional<Vector> vector = retrieve(id);
                vector.ifPresent(results::add);
            }
            return results;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void delete(String id) {
        if (id == null) {
            return;
        }

        lock.writeLock().lock();
        try {
            byte[] key = id.getBytes();
            db.delete(writeOptions, key);
        } catch (Exception e) {
            throw new StorageException("Failed to delete vector: " + id, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<Vector> scanAll() {
        lock.readLock().lock();
        try (RocksIterator iterator = db.newIterator(readOptions)) {
            List<Vector> vectors = new ArrayList<>();
            iterator.seekToFirst();

            while (iterator.isValid()) {
                vectors.add(objectMapper.readValue(iterator.value(), Vector.class));
                iterator.next();
            }

            return vectors;
        } catch (Exception e) {
            throw new StorageException("Failed to scan vectors", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Vector> scanRange(String startId, String endId, int limit) {
        lock.readLock().lock();
        try (RocksIterator iterator = db.newIterator(readOptions)) {
            List<Vector> vectors = new ArrayList<>();
            byte[] startKey = startId != null ? startId.getBytes() : null;

            if (startKey != null) {
                iterator.seek(startKey);
            } else {
                iterator.seekToFirst();
            }

            while (iterator.isValid() && vectors.size() < limit) {
                if (endId != null && new String(iterator.key()).compareTo(endId) > 0) {
                    break;
                }

                vectors.add(objectMapper.readValue(iterator.value(), Vector.class));
                iterator.next();
            }

            return vectors;
        } catch (Exception e) {
            throw new StorageException("Failed to scan vector range", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void createBackup(String checkpointPath) {
        lock.readLock().lock();
        try {
            Path path = Path.of(checkpointPath);
            // Parent directory should exist
            Files.createDirectories(path.getParent());

            // If checkpoint directory exists, delete it first
            if (Files.exists(path)) {
                deleteDirectory(path);
            }

            // Create checkpoint (RocksDB will create the directory)
            try (Checkpoint checkpoint = Checkpoint.create(db)) {
                checkpoint.createCheckpoint(checkpointPath);
            }
        } catch (Exception e) {
            throw new StorageException("Failed to create backup at: " + checkpointPath, e);
        } finally {
            lock.readLock().unlock();
        }
    }

    public String createTimestampedBackup(String baseBackupPath) {
        // Create base backup directory if it doesn't exist
        try {
            Files.createDirectories(Path.of(baseBackupPath));
        } catch (IOException e) {
            throw new StorageException("Failed to create backup directory: " + baseBackupPath, e);
        }

        // Generate timestamp and backup path
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String backupPath = Path.of(baseBackupPath, "backup_" + timestamp).toString();

        // Create the backup
        createBackup(backupPath);

        return backupPath;
    }

    private void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }

        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
    private void cleanupOldBackups(String baseBackupPath, int maxBackups) {
        try {
            Path basePath = Path.of(baseBackupPath);
            if (!Files.exists(basePath)) {
                return;
            }

            List<Path> backups = Files.list(basePath)
                    .filter(path -> Files.isDirectory(path) &&
                            path.getFileName().toString().startsWith("backup_"))
                    .sorted()
                    .collect(Collectors.toList());

            while (backups.size() > maxBackups) {
                Path oldestBackup = backups.remove(0);
                deleteDirectory(oldestBackup);
            }
        } catch (IOException e) {
            throw new StorageException("Failed to cleanup old backups", e);
        }
    }

    private void validateVector(Vector vector) {
        if (vector == null) {
            throw new IllegalArgumentException("Vector cannot be null");
        }
        if (vector.getId() == null) {
            throw new IllegalArgumentException("Vector ID cannot be null");
        }
        if (vector.getValues() == null || vector.getValues().length == 0) {
            throw new IllegalArgumentException("Vector values cannot be null or empty");
        }
    }

    public Map<String, String> getStatistics() throws Exception {
        return Map.of(
                "estimateNumKeys", String.valueOf(db.getLongProperty("rocksdb.estimate-num-keys")),
                "estimateTableReaders", String.valueOf(db.getLongProperty("rocksdb.estimate-table-readers-mem")),
                "numImmutableMemTable", String.valueOf(db.getLongProperty("rocksdb.num-immutable-mem-table")),
                "numRunningCompactions", String.valueOf(db.getLongProperty("rocksdb.num-running-compactions")),
                "numRunningFlushes", String.valueOf(db.getLongProperty("rocksdb.num-running-flushes"))
        );
    }

    @Override
    public void close() {
        lock.writeLock().lock();
        try {
            if (writeOptions != null) {
                writeOptions.close();
            }
            if (readOptions != null) {
                readOptions.close();
            }
            if (db != null) {
                db.close();
            }
            if (options != null) {
                options.close();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}