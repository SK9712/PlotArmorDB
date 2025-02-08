package com.plotarmordb.core.embedding;

import com.plotarmordb.core.config.EmbeddingConfig;
import com.plotarmordb.core.search.VectorMath;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TextEmbeddingEngine {
    private final Map<String, Integer> vocabulary;
    private final List<String> vocabularyList;
    private final int maxVocabularySize;
    private final Object vocabularyLock = new Object();

    public TextEmbeddingEngine(EmbeddingConfig config) {
        this.maxVocabularySize = config.getVocabularySize();
        this.vocabulary = new ConcurrentHashMap<>();
        this.vocabularyList = Collections.synchronizedList(new ArrayList<>());
    }

    public float[] generateEmbedding(String text) {
        // Preprocess and tokenize text
        List<String> tokens = tokenize(text.toLowerCase());

        // Calculate term frequencies
        Map<String, Integer> termFrequencies = calculateTermFrequencies(tokens);

        // Generate TF-IDF vector
        float[] embedding = new float[maxVocabularySize];
        for (Map.Entry<String, Integer> entry : termFrequencies.entrySet()) {
            String term = entry.getKey();
            int frequency = entry.getValue();

            int index = getOrCreateTermIndex(term);
            if (index < maxVocabularySize) {
                embedding[index] = (float) frequency / tokens.size();
            }
        }

        // Normalize the embedding vector
        VectorMath.normalizeVector(embedding);
        return embedding;
    }

    private List<String> tokenize(String text) {
        return Arrays.stream(text.replaceAll("[^a-zA-Z0-9\\s]", "").split("\\s+"))
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toList());
    }

    private Map<String, Integer> calculateTermFrequencies(List<String> tokens) {
        Map<String, Integer> frequencies = new HashMap<>();
        for (String token : tokens) {
            frequencies.merge(token, 1, Integer::sum);
        }
        return frequencies;
    }

    private int getOrCreateTermIndex(String term) {
        Integer index = vocabulary.get(term);
        if (index != null) {
            return index;
        }

        synchronized (vocabularyLock) {
            // Double-check after acquiring lock
            index = vocabulary.get(term);
            if (index != null) {
                return index;
            }

            if (vocabularyList.size() < maxVocabularySize) {
                index = vocabularyList.size();
                vocabulary.put(term, index);
                vocabularyList.add(term);
                return index;
            }
        }

        return maxVocabularySize; // Term not in vocabulary
    }

    public Set<String> getVocabulary() {
        return new HashSet<>(vocabulary.keySet());
    }

    public int getVocabularySize() {
        return vocabulary.size();
    }
}