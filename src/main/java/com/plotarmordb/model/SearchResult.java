package com.plotarmordb.model;

public class SearchResult {
    private Vector vector;
    private double similarity;

    public SearchResult(Vector vector, double similarity) {
        this.vector = vector;
        this.similarity = similarity;
    }

    public Vector getVector() { return vector; }
    public double getSimilarity() { return similarity; }
}
