package com.plotarmordb.service;

import com.plotarmordb.cache.SearchCache;
import com.plotarmordb.model.SearchResult;
import com.plotarmordb.model.Vector;
import com.plotarmordb.storage.VectorStorage;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class VectorSearchService {
    private final VectorStorage storage;
    private final SearchCache cache;

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
}