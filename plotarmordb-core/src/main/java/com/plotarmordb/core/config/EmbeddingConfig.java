package com.plotarmordb.core.config;

public class EmbeddingConfig {
    private int vocabularySize = 10000;

    public int getVocabularySize() { return vocabularySize; }
    public void setVocabularySize(int size) { this.vocabularySize = size; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final EmbeddingConfig config = new EmbeddingConfig();

        public Builder vocabularySize(int size) {
            config.setVocabularySize(size);
            return this;
        }

        public EmbeddingConfig build() {
            return config;
        }
    }
}