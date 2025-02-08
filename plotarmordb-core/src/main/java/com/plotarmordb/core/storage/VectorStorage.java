package com.plotarmordb.core.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plotarmordb.core.model.Vector;
import com.plotarmordb.core.config.StorageConfig;
import com.plotarmordb.core.exception.StorageException;

import org.rocksdb.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class VectorStorage implements AutoCloseable {
    private RocksDB db;
    private final ObjectMapper objectMapper;
    private final StorageConfig config;
    private final ReadWriteLock lock;
    private final Options options;
    private final Statistics statistics;

    public VectorStorage(StorageConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.lock = new ReentrantReadWriteLock();
        this.statistics = new Statistics();
        this.options = createOptions();
        initialize();
    }

    private Options createOptions() {
        Options options = new Options();
        options.setCreateIfMissing(true);
        options.setStatistics(statistics);

        // Performance optimizations
        options.setWriteBufferSize(config.getWriteBufferSize());
        options.setMaxWriteBufferNumber(3);
        options.setMinWriteBufferNumberToMerge(1);
        options.setLevel0FileNumCompactionTrigger(4);
        options.setLevel0SlowdownWritesTrigger(8);
        options.setLevel0StopWritesTrigger(12);
        options.setMaxBackgroundJobs(config.getMaxBackgroundJobs());

        // Compression settings
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
        if (vector == null || vector.getId() == null) {
            throw new IllegalArgumentException("Vector and vector ID cannot be null");
        }

        lock.writeLock().lock();
        try {
            byte[] key = vector.getId().getBytes();
            byte[] value = objectMapper.writeValueAsBytes(vector);
            db.put(key, value);
        } catch (Exception e) {
            throw new StorageException("Failed to store vector: " + vector.getId(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void storeBatch(List<Vector> vectors) {
        if (vectors == null || vectors.isEmpty()) {
            return;
        }

        lock.writeLock().lock();
        try (WriteOptions writeOpts = new WriteOptions()) {
            try (WriteBatch batch = new WriteBatch()) {
                for (Vector vector : vectors) {
                    if (vector.getId() != null) {
                        byte[] key = vector.getId().getBytes();
                        byte[] value = objectMapper.writeValueAsBytes(vector);
                        batch.put(key, value);
                    }
                }
                db.write(writeOpts, batch);
            }
        } catch (Exception e) {
            throw new StorageException("Failed to store batch of vectors", e);
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
            byte[] value = db.get(key);

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

    public void delete(String id) {
        if (id == null) {
            return;
        }

        lock.writeLock().lock();
        try {
            byte[] key = id.getBytes();
            db.delete(key);
        } catch (Exception e) {
            throw new StorageException("Failed to delete vector: " + id, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<Vector> scanAll() {
        lock.readLock().lock();
        try (RocksIterator iterator = db.newIterator()) {
            List<Vector> vectors = new ArrayList<>();
            iterator.seekToFirst();

            while (iterator.isValid()) {
                Vector vector = objectMapper.readValue(iterator.value(), Vector.class);
                vectors.add(vector);
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
        try (RocksIterator iterator = db.newIterator()) {
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

                Vector vector = objectMapper.readValue(iterator.value(), Vector.class);
                vectors.add(vector);
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
            Files.createDirectories(path);

            try (Checkpoint checkpoint = Checkpoint.create(db)) {
                checkpoint.createCheckpoint(checkpointPath);
            }
        } catch (Exception e) {
            throw new StorageException("Failed to create backup at: " + checkpointPath, e);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Map<String, String> getStatistics() {
        return Map.of(
                "totalBytes", String.valueOf(statistics.getTickerCount(TickerType.BYTES_WRITTEN)),
                "numberOfKeys", String.valueOf(statistics.getTickerCount(TickerType.NUMBER_KEYS_WRITTEN)),
                "writeOps", String.valueOf(statistics.getTickerCount(TickerType.BLOCK_CACHE_BYTES_WRITE)),
                "readOps", String.valueOf(statistics.getTickerCount(TickerType.BLOCK_CACHE_BYTES_READ))
        );
    }

    @Override
    public void close() {
        lock.writeLock().lock();
        try {
            if (db != null) {
                db.close();
            }
            if (options != null) {
                options.close();
            }
            if (statistics != null) {
                statistics.close();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}