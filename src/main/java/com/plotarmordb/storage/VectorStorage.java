package com.plotarmor.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plotarmor.model.Vector;
import org.rocksdb.*;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Component
public class VectorStorage {
    private RocksDB db;
    private final ObjectMapper objectMapper;
    private static final String DB_PATH = "plotarmor-data";

    public VectorStorage(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() throws RocksDBException, IOException {
        RocksDB.loadLibrary();
        Files.createDirectories(Path.of(DB_PATH));

        Options options = new Options();
        options.setCreateIfMissing(true);
        db = RocksDB.open(options, DB_PATH);
    }

    @PreDestroy
    public void cleanup() {
        if (db != null) {
            db.close();
        }
    }

    public void store(Vector vector) throws RocksDBException, IOException {
        byte[] key = vector.getId().getBytes();
        byte[] value = objectMapper.writeValueAsBytes(vector);
        db.put(key, value);
    }

    public Optional<Vector> retrieve(String id) throws RocksDBException, IOException {
        byte[] key = id.getBytes();
        byte[] value = db.get(key);

        if (value == null) {
            return Optional.empty();
        }

        return Optional.of(objectMapper.readValue(value, Vector.class));
    }

    public void delete(String id) throws RocksDBException {
        byte[] key = id.getBytes();
        db.delete(key);
    }
}