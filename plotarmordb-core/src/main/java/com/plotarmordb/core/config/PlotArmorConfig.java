package com.plotarmordb.core.config;

import java.nio.file.Path;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

public class PlotArmorConfig {
    private final DbConfig db;
    private final CacheConfig cache;
    private final EmbeddingConfig embedding;
    private final SearchConfig search;

    private PlotArmorConfig(Builder builder) {
        this.db = builder.db;
        this.cache = builder.cache;
        this.embedding = builder.embedding;
        this.search = builder.search;
    }

    public DbConfig getDb() { return db; }
    public CacheConfig getCache() { return cache; }
    public EmbeddingConfig getEmbedding() { return embedding; }
    public SearchConfig getSearch() { return search; }

    public static class Builder {
        private DbConfig db = new DbConfig();
        private CacheConfig cache = new CacheConfig();
        private EmbeddingConfig embedding = new EmbeddingConfig();
        private SearchConfig search = new SearchConfig();

        public Builder db(DbConfig db) {
            this.db = db;
            return this;
        }

        public Builder cache(CacheConfig cache) {
            this.cache = cache;
            return this;
        }

        public Builder embedding(EmbeddingConfig embedding) {
            this.embedding = embedding;
            return this;
        }

        public Builder search(SearchConfig search) {
            this.search = search;
            return this;
        }

        public Builder loadFromProperties(Path propertiesPath) throws IOException {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(propertiesPath.toFile())) {
                props.load(fis);
            }

            // Load DB config
            db.setPath(props.getProperty("plotarmor.db.path", db.getPath()));

            // Load cache config
            try {
                cache.setMaxSize(Integer.parseInt(
                        props.getProperty("plotarmor.cache.maxSize",
                                String.valueOf(cache.getMaxSize()))));
            } catch (NumberFormatException e) {
                // Keep default if parsing fails
            }

            // Load embedding config
            try {
                embedding.setVocabularySize(Integer.parseInt(
                        props.getProperty("plotarmor.embedding.vocabularySize",
                                String.valueOf(embedding.getVocabularySize()))));
            } catch (NumberFormatException e) {
                // Keep default if parsing fails
            }

            // Load search config
            try {
                search.setBatchSize(Integer.parseInt(
                        props.getProperty("plotarmor.search.batchSize",
                                String.valueOf(search.getBatchSize()))));
            } catch (NumberFormatException e) {
                // Keep default if parsing fails
            }

            return this;
        }

        public PlotArmorConfig build() {
            return new PlotArmorConfig(this);
        }
    }
}