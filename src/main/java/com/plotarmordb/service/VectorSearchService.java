package com.plotarmordb.service;

import com.plotarmordb.model.SearchResult;
import com.plotarmordb.storage.VectorStorage;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VectorSearchService {
    private final VectorStorage storage;

    public VectorSearchService(VectorStorage storage) {
        this.storage = storage;
    }

    public List<SearchResult> search(float[] queryVector, int topK) {
        List<SearchResult> results = new ArrayList<>();

        try {
            // Iterate through all vectors (brute-force)
            storage.scanAll().forEach(vector -> {
                double similarity = cosineSimilarity(queryVector, vector.getValues());
                results.add(new SearchResult(vector, similarity));
            });

            // Sort by similarity (descending) and return top-K
            return results.stream()
                    .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
                    .limit(topK)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Search failed", e);
        }
    }

    private double cosineSimilarity(float[] v1, float[] v2) {
        if (v1.length != v2.length) {
            throw new IllegalArgumentException("Vectors must have same length");
        }

        double dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;

        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}