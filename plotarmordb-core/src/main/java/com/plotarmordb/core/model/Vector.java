package com.plotarmordb.core.model;

import java.util.Map;

public class Vector {
    private String id;
    private float[] values;
    private Map<String, String> metadata;

    public Vector() {}

    public Vector(String id, float[] values, Map<String, String> metadata) {
        this.id = id;
        this.values = values;
        this.metadata = metadata;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public float[] getValues() { return values; }
    public void setValues(float[] values) { this.values = values; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
}