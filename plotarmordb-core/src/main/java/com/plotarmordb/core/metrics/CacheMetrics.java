package com.plotarmordb.core.metrics;

public class CacheMetrics {
    private long hits;
    private long misses;
    private long evictions;
    private int size;

    public long getHits() { return hits; }
    public void setHits(long hits) { this.hits = hits; }

    public long getMisses() { return misses; }
    public void setMisses(long misses) { this.misses = misses; }

    public long getEvictions() { return evictions; }
    public void setEvictions(long evictions) { this.evictions = evictions; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public double getHitRate() {
        long total = hits + misses;
        return total == 0 ? 0.0 : (double) hits / total;
    }
}