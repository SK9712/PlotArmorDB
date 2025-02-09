package com.plotarmordb.core.search;

import com.plotarmordb.core.config.CacheConfig;
import com.plotarmordb.core.config.EmbeddingConfig;
import com.plotarmordb.core.model.*;
import com.plotarmordb.core.model.Vector;
import com.plotarmordb.core.storage.VectorStorage;
import com.plotarmordb.core.config.SearchConfig;
import com.plotarmordb.core.cache.SearchCache;
import com.plotarmordb.core.embedding.TextEmbeddingEngine;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class VectorSearchEngine implements AutoCloseable {
    private final VectorStorage storage;
    private final SearchCache cache;
    private final TextEmbeddingEngine embeddingEngine;
    private final SearchConfig config;
    private final AtomicReference<ExecutorService> executor;

    public VectorSearchEngine(VectorStorage storage, SearchConfig searchConfig,
                              CacheConfig cacheConfig, EmbeddingConfig embeddingConfig) {
        this.storage = storage;
        this.config = searchConfig;
        this.cache = new SearchCache(cacheConfig);
        this.embeddingEngine = new TextEmbeddingEngine(embeddingConfig);
        this.executor = new AtomicReference<>(createExecutor());
    }

    private ExecutorService createExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    public List<SearchResult> search(float[] queryVector, int topK, Map<String, String> filter) {
        String filterHash = filter != null ? filter.toString() : "";

        float[] paddedQueryVector = addPadding(queryVector);
        // Check cache first
        List<SearchResult> cachedResults = cache.get(paddedQueryVector, topK, filterHash);
        if (cachedResults != null) {
            return cachedResults;
        }

        try {
            List<Vector> vectors = storage.scanAll();
            List<SearchResult> results = searchParallel(vectors, paddedQueryVector, topK, filter);

            // Cache results
            cache.put(paddedQueryVector, topK, filterHash, results);
            return results;
        } catch (Exception e) {
            throw new RuntimeException("Search failed", e);
        }
    }

    private float[] addPadding(float[] queryVector) {
        float[] embedding = new float[10000];
        int index = 0;
        for (float value : queryVector) {
            if (index < 10000) {
                embedding[index++] = value;
            }
        }
        // Normalize the embedding vector
        VectorMath.normalizeVector(embedding);
        return embedding;
    }

    public List<SearchResult> searchByText(String query, int topK, Map<String, String> filter) {
        float[] queryVector = embeddingEngine.generateEmbedding(query);
        return search(queryVector, topK, filter);
    }

    private List<SearchResult> searchParallel(List<Vector> vectors, float[] queryVector,
                                              int topK, Map<String, String> filter) {
        int batchSize = config.getBatchSize();
        List<List<Vector>> batches = splitIntoBatches(vectors, batchSize);

        PriorityQueue<SearchResult> resultQueue = new PriorityQueue<>(
                Comparator.comparingDouble(SearchResult::getSimilarity).reversed()
        );

        float[] paddedQueryVector = addPadding(queryVector);

        try {
            List<Future<List<SearchResult>>> futures = new ArrayList<>();
            ExecutorService currentExecutor = executor.get();

            // Submit batch processing tasks
            for (List<Vector> batch : batches) {
                futures.add(currentExecutor.submit(() ->
                        processBatch(batch, paddedQueryVector, filter)));
            }

            // Collect and merge results
            for (Future<List<SearchResult>> future : futures) {
                resultQueue.addAll(future.get());
            }

            return extractTopK(resultQueue, topK);
        } catch (Exception e) {
            throw new RuntimeException("Parallel search failed", e);
        }
    }

    private List<List<Vector>> splitIntoBatches(List<Vector> vectors, int batchSize) {
        List<List<Vector>> batches = new ArrayList<>();
        for (int i = 0; i < vectors.size(); i += batchSize) {
            batches.add(vectors.subList(i, Math.min(i + batchSize, vectors.size())));
        }
        return batches;
    }

    private List<SearchResult> processBatch(List<Vector> batch, float[] queryVector,
                                            Map<String, String> filter) {
        List<SearchResult> batchResults = new ArrayList<>();
        float[] paddedQueryVector = addPadding(queryVector);

        for (Vector vector : batch) {
            if (matchesFilter(vector, filter)) {
                double similarity = VectorMath.calculateCosineSimilarity(
                        paddedQueryVector, vector.getValues());
                batchResults.add(new SearchResult(vector, similarity));
            }
        }

        return batchResults;
    }

    private boolean matchesFilter(Vector vector, Map<String, String> filter) {
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

    private List<SearchResult> extractTopK(PriorityQueue<SearchResult> queue, int k) {
        List<SearchResult> results = new ArrayList<>();
        for (int i = 0; i < k && !queue.isEmpty(); i++) {
            results.add(queue.poll());
        }
        return results;
    }

    @Override
    public void close() {
        ExecutorService currentExecutor = executor.get();
        if (currentExecutor != null) {
            currentExecutor.shutdown();
            try {
                if (!currentExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    currentExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                currentExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        cache.close();
    }
}