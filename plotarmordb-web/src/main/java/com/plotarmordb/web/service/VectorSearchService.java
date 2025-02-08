package com.plotarmordb.web.service;

import com.plotarmordb.web.cache.SearchCache;
import com.plotarmordb.web.model.SearchResult;
import com.plotarmordb.web.model.TextSearchRequest;
import com.plotarmordb.web.model.Vector;
import com.plotarmordb.web.storage.VectorStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class VectorSearchService {
    private final VectorStorage storage;
    private final SearchCache cache;
    @Autowired
    private TextEmbeddingService textEmbeddingService;

    public VectorSearchService(VectorStorage storage, SearchCache cache) {
        this.storage = storage;
        this.cache = cache;
    }

    public List<SearchResult> search(float[] queryVector, int topK, Map<String, String> filter) {
        // Generate cache key for filter
        String filterHash = filter != null ? filter.toString() : "";

        // Check cache first
        List<SearchResult> cachedResults = cache.get(queryVector, topK, filterHash);
        if (cachedResults != null) {
            return cachedResults;
        }

        try {
            // Get all vectors and perform parallel search
            List<Vector> vectors = storage.scanAll();
            List<SearchResult> results = ConcurrentVectorSearch.searchParallel(
                    vectors, queryVector, topK, filter
            );

            // Cache results
            cache.put(queryVector, topK, filterHash, results);

            return results;
        } catch (Exception e) {
            throw new RuntimeException("Search failed", e);
        }
    }

    public List<SearchResult> searchByText(TextSearchRequest request) {
        // Convert text query to vector embedding
        float[] queryVector = textEmbeddingService.generateEmbedding(request.getQuery());

        // Perform vector search
        return search(queryVector, request.getTopK(), request.getFilter());
    }
}