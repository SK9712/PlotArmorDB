package com.plotarmordb.model;

import java.util.Map;

public class SearchRequest {
    private float[] queryVector;
    private int topK;
    private Map<String, String> filter;

    public float[] getQueryVector() { return queryVector; }
    public void setQueryVector(float[] queryVector) { this.queryVector = queryVector; }

    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }

    public Map<String, String> getFilter() { return filter; }
    public void setFilter(Map<String, String> filter) { this.filter = filter; }
}