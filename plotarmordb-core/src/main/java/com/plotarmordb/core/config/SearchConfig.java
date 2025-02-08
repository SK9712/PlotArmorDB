package com.plotarmordb.core.config;

public class SearchConfig {
    private int batchSize = 1000;

    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int size) { this.batchSize = size; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final SearchConfig config = new SearchConfig();

        public Builder batchSize(int size) {
            config.setBatchSize(size);
            return this;
        }

        public SearchConfig build() {
            return config;
        }
    }
}