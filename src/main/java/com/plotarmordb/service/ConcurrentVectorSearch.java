package com.plotarmordb.service;

import com.plotarmordb.model.SearchResult;
import com.plotarmordb.model.Vector;
import java.util.*;
import java.util.concurrent.*;

public class ConcurrentVectorSearch {
    private static final int BATCH_SIZE = 1000;

    public static List<SearchResult> searchParallel(List<Vector> vectors, float[] queryVector,
                                                    int topK, Map<String, String> filter) {
        // Split vectors into batches
        List<List<Vector>> batches = splitIntoBatches(vectors, BATCH_SIZE);

        // Create thread-safe priority queue for results
        PriorityQueue<SearchResult> resultQueue = new PriorityQueue<>(
                Comparator.comparingDouble(SearchResult::getSimilarity).reversed()
        );

        try {
            // Process batches with virtual threads
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                List<Future<List<SearchResult>>> futures = new ArrayList<>();

                // Submit batch processing tasks
                for (List<Vector> batch : batches) {
                    futures.add(executor.submit(() -> processBatch(batch, queryVector, filter)));
                }

                // Collect and merge results
                for (Future<List<SearchResult>> future : futures) {
                    resultQueue.addAll(future.get());
                }
            }

            // Return top-K results
            return extractTopK(resultQueue, topK);
        } catch (Exception e) {
            throw new RuntimeException("Parallel search failed", e);
        }
    }

    private static List<List<Vector>> splitIntoBatches(List<Vector> vectors, int batchSize) {
        List<List<Vector>> batches = new ArrayList<>();
        for (int i = 0; i < vectors.size(); i += batchSize) {
            batches.add(vectors.subList(i,
                    Math.min(i + batchSize, vectors.size())));
        }
        return batches;
    }

    private static List<SearchResult> processBatch(List<Vector> batch,
                                                   float[] queryVector, Map<String, String> filter) {
        List<SearchResult> batchResults = new ArrayList<>();

        for (Vector vector : batch) {
            if (matchesFilter(vector, filter)) {
                double similarity = calculateCosineSimilarity(queryVector, vector.getValues());
                batchResults.add(new SearchResult(vector, similarity));
            }
        }

        return batchResults;
    }

    private static boolean matchesFilter(Vector vector, Map<String, String> filter) {
        if (filter == null || filter.isEmpty()) {
            return true;
        }

        Map<String, String> metadata = vector.getMetadata();
        if (metadata == null) {
            return false;
        }

        return filter.entrySet().stream()
                .allMatch(entry ->
                        metadata.containsKey(entry.getKey()) &&
                                metadata.get(entry.getKey()).equals(entry.getValue())
                );
    }

    private static double calculateCosineSimilarity(float[] v1, float[] v2) {
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

    private static List<SearchResult> extractTopK(PriorityQueue<SearchResult> queue, int k) {
        List<SearchResult> results = new ArrayList<>();
        for (int i = 0; i < k && !queue.isEmpty(); i++) {
            results.add(queue.poll());
        }
        return results;
    }
}