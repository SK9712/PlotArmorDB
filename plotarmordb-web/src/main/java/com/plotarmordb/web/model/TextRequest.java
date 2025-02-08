package com.plotarmordb.web.model;

import java.util.Map;

public class TextRequest {
    private String text;
    private Map<String, String> metadata;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
}