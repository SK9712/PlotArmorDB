package com.plotarmordb.core.config;

public class CacheConfig {
    private int maxSize = 1000;

    public int getMaxSize() { return maxSize; }
    public void setMaxSize(int maxSize) { this.maxSize = maxSize; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final CacheConfig config = new CacheConfig();

        public Builder maxSize(int maxSize) {
            config.setMaxSize(maxSize);
            return this;
        }

        public CacheConfig build() {
            return config;
        }
    }
}