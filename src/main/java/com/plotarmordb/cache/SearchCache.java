package com.plotarmordb.cache;

import com.plotarmordb.model.SearchResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;

@Component
public class SearchCache {
    private final ConcurrentHashMap<CacheKey, List<SearchResult>> cache;
    private static final int MAX_CACHE_SIZE = 1000;

    public SearchCache() {
        this.cache = new ConcurrentHashMap<>();
    }

    public List<SearchResult> get(float[] queryVector, int topK,
                                  String filterHash) {
        CacheKey key = new CacheKey(queryVector, topK, filterHash);
        return cache.get(key);
    }

    public void put(float[] queryVector, int topK, String filterHash,
                    List<SearchResult> results) {
        if (cache.size() >= MAX_CACHE_SIZE) {
            // Simple cache eviction - clear all when full
            cache.clear();
        }

        CacheKey key = new CacheKey(queryVector, topK, filterHash);
        cache.put(key, results);
    }

    private static class CacheKey {
        private final float[] queryVector;
        private final int topK;
        private final String filterHash;

        CacheKey(float[] queryVector, int topK, String filterHash) {
            this.queryVector = queryVector;
            this.topK = topK;
            this.filterHash = filterHash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return topK == cacheKey.topK &&
                    Arrays.equals(queryVector, cacheKey.queryVector) &&
                    Objects.equals(filterHash, cacheKey.filterHash);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(topK, filterHash);
            result = 31 * result + Arrays.hashCode(queryVector);
            return result;
        }
    }
}