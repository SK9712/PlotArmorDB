package com.plotarmordb.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TextEmbeddingService {
    private final Map<String, Integer> vocabulary;
    private final List<String> vocabularyList;
    private static final int MAX_VOCABULARY_SIZE = 10000;

    public TextEmbeddingService() {
        this.vocabulary = new HashMap<>();
        this.vocabularyList = new ArrayList<>();
    }

    public float[] generateEmbedding(String text) {
        // Preprocess text
        List<String> tokens = tokenize(text.toLowerCase());

        // Calculate term frequencies for this document
        Map<String, Integer> termFrequencies = calculateTermFrequencies(tokens);

        // Generate TF-IDF vector
        float[] embedding = new float[MAX_VOCABULARY_SIZE];
        for (Map.Entry<String, Integer> entry : termFrequencies.entrySet()) {
            String term = entry.getKey();
            int frequency = entry.getValue();

            int index = getOrCreateTermIndex(term);
            if (index < MAX_VOCABULARY_SIZE) {
                // Simple TF-IDF calculation (just using TF for now)
                embedding[index] = (float) frequency / tokens.size();
            }
        }

        // Normalize the vector
        normalizeVector(embedding);

        return embedding;
    }

    private List<String> tokenize(String text) {
        // Basic tokenization: split on whitespace and remove punctuation
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

    private synchronized int getOrCreateTermIndex(String term) {
        if (vocabulary.containsKey(term)) {
            return vocabulary.get(term);
        }

        if (vocabularyList.size() < MAX_VOCABULARY_SIZE) {
            int newIndex = vocabularyList.size();
            vocabulary.put(term, newIndex);
            vocabularyList.add(term);
            return newIndex;
        }

        return MAX_VOCABULARY_SIZE; // Term not in vocabulary
    }

    private void normalizeVector(float[] vector) {
        float sumSquares = 0.0f;
        for (float value : vector) {
            sumSquares += value * value;
        }

        if (sumSquares > 0) {
            float magnitude = (float) Math.sqrt(sumSquares);
            for (int i = 0; i < vector.length; i++) {
                vector[i] = vector[i] / magnitude;
            }
        }
    }
}