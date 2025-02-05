package com.plotarmordb.model;

public class SearchRequest {
    private float[] queryVector;
    private int topK;

    public float[] getQueryVector() { return queryVector; }
    public void setQueryVector(float[] queryVector) { this.queryVector = queryVector; }

    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }
}
