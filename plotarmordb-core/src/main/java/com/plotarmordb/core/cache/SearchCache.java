package com.plotarmordb.core.cache;

import com.plotarmordb.core.model.SearchResult;
import com.plotarmordb.core.config.CacheConfig;
import com.plotarmordb.core.metrics.CacheMetrics;

import java.util.List;
import java.util.Objects;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.LongAdder;
import java.time.Instant;

public class SearchCache implements AutoCloseable {
    private final ConcurrentHashMap<CacheKey, CacheEntry> cache;
    private final int maxSize;
    private final ReentrantReadWriteLock evictionLock;
    private final CacheMetrics metrics;

    // Metrics counters
    private final LongAdder hits;
    private final LongAdder misses;
    private final LongAdder evictions;

    public SearchCache(CacheConfig config) {
        this.maxSize = config.getMaxSize();
        this.cache = new ConcurrentHashMap<>();
        this.evictionLock = new ReentrantReadWriteLock();
        this.metrics = new CacheMetrics();

        this.hits = new LongAdder();
        this.misses = new LongAdder();
        this.evictions = new LongAdder();
    }

    public List<SearchResult> get(float[] queryVector, int topK, String filterHash) {
        CacheKey key = new CacheKey(queryVector, topK, filterHash);
        CacheEntry entry = cache.get(key);

        if (entry != null && !isExpired(entry)) {
            hits.increment();
            entry.updateAccessTime();
            return entry.getResults();
        }

        if (entry != null) {
            // Entry exists but is expired
            cache.remove(key);
        }

        misses.increment();
        return null;
    }

    public void put(float[] queryVector, int topK, String filterHash, List<SearchResult> results) {
        CacheKey key = new CacheKey(queryVector, topK, filterHash);

        evictionLock.readLock().lock();
        try {
            if (cache.size() >= maxSize) {
                evict();
            }

            cache.put(key, new CacheEntry(results));

        } finally {
            evictionLock.readLock().unlock();
        }
    }

    private void evict() {
        evictionLock.writeLock().lock();
        try {
            if (cache.size() >= maxSize) {
                // LRU eviction - remove oldest 20% of entries
                int toRemove = Math.max(1, cache.size() / 5);

                cache.entrySet().stream()
                        .sorted((e1, e2) ->
                                e1.getValue().getLastAccessTime().compareTo(e2.getValue().getLastAccessTime()))
                        .limit(toRemove)
                        .forEach(entry -> {
                            cache.remove(entry.getKey());
                            evictions.increment();
                        });
            }
        } finally {
            evictionLock.writeLock().unlock();
        }
    }

    private boolean isExpired(CacheEntry entry) {
        // Consider entry expired if it hasn't been accessed in last hour
        return entry.getLastAccessTime().plusSeconds(3600).isBefore(Instant.now());
    }

    public void clear() {
        evictionLock.writeLock().lock();
        try {
            cache.clear();
        } finally {
            evictionLock.writeLock().unlock();
        }
    }

    public CacheMetrics getMetrics() {
        metrics.setHits(hits.sum());
        metrics.setMisses(misses.sum());
        metrics.setEvictions(evictions.sum());
        metrics.setSize(cache.size());
        return metrics;
    }

    @Override
    public void close() {
        clear();
    }

    private static class CacheKey {
        private final float[] queryVector;
        private final int topK;
        private final String filterHash;

        CacheKey(float[] queryVector, int topK, String filterHash) {
            this.queryVector = Arrays.copyOf(queryVector, queryVector.length);
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

    private static class CacheEntry {
        private final List<SearchResult> results;
        private volatile Instant lastAccessTime;

        CacheEntry(List<SearchResult> results) {
            this.results = List.copyOf(results); // Immutable copy
            this.lastAccessTime = Instant.now();
        }

        List<SearchResult> getResults() {
            return results;
        }

        Instant getLastAccessTime() {
            return lastAccessTime;
        }

        void updateAccessTime() {
            this.lastAccessTime = Instant.now();
        }
    }
}