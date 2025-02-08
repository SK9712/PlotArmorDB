package com.plotarmordb.core.config;

public class StorageConfig {
    private String dbPath = "plotarmor-data";
    private boolean compressionEnabled = true;
    private int writeBufferSize = 64 * 1024 * 1024; // 64MB
    private int maxBackgroundJobs = 4;

    private StorageConfig(Builder builder) {
        this.dbPath = builder.dbPath;
        this.compressionEnabled = builder.compressionEnabled;
        this.writeBufferSize = builder.writeBufferSize;
        this.maxBackgroundJobs = builder.maxBackgroundJobs;
    }

    public String getDbPath() { return dbPath; }
    public boolean isCompressionEnabled() { return compressionEnabled; }
    public int getWriteBufferSize() { return writeBufferSize; }
    public int getMaxBackgroundJobs() { return maxBackgroundJobs; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String dbPath = "plotarmor-data";
        private boolean compressionEnabled = true;
        private int writeBufferSize = 64 * 1024 * 1024;
        private int maxBackgroundJobs = 4;

        public Builder dbPath(String path) {
            this.dbPath = path;
            return this;
        }

        public Builder compressionEnabled(boolean enabled) {
            this.compressionEnabled = enabled;
            return this;
        }

        public Builder writeBufferSize(int size) {
            this.writeBufferSize = size;
            return this;
        }

        public Builder maxBackgroundJobs(int jobs) {
            this.maxBackgroundJobs = jobs;
            return this;
        }

        public StorageConfig build() {
            return new StorageConfig(this);
        }
    }
}