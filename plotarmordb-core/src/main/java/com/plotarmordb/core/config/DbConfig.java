package com.plotarmordb.core.config;

public class DbConfig {
    private String path = "plotarmor-data";

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final DbConfig config = new DbConfig();

        public Builder path(String path) {
            config.setPath(path);
            return this;
        }

        public DbConfig build() {
            return config;
        }
    }
}