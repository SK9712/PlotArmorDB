package com.plotarmordb.web.model;

import java.util.Map;

public class TextSearchRequest {
    private String query;
    private int topK;
    private Map<String, String> filter;

    // Getters and setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }

    public Map<String, String> getFilter() { return filter; }
    public void setFilter(Map<String, String> filter) { this.filter = filter; }
}