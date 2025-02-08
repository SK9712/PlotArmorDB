package com.plotarmordb.core.search;

public final class VectorMath {
    private VectorMath() {} // Prevent instantiation

    public static double calculateCosineSimilarity(float[] v1, float[] v2) {
        if (v1.length != v2.length) {
            throw new IllegalArgumentException("Vectors must have same length");
        }

        double dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;

        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }

        double norms = Math.sqrt(norm1) * Math.sqrt(norm2);
        return norms > 0 ? dotProduct / norms : 0.0;
    }

    public static void normalizeVector(float[] vector) {
        float sumSquares = 0.0f;
        for (float value : vector) {
            sumSquares += value * value;
        }

        if (sumSquares > 0) {
            float magnitude = (float) Math.sqrt(sumSquares);
            for (int i = 0; i < vector.length; i++) {
                vector[i] = vector[i] / magnitude;
            }
        }
    }

    public static float[] padVector(float[] vector, int targetSize) {
        float[] padded = new float[targetSize];
        if (vector != null && vector.length > 0) {
            System.arraycopy(vector, 0, padded, 0,
                    Math.min(vector.length, targetSize));
        }
        return padded;
    }
}