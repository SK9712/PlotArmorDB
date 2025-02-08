package com.plotarmordb.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import lombok.Data;

@Configuration
@EnableConfigurationProperties
public class PlotArmorConfig {

    @Bean
    @ConfigurationProperties(prefix = "plotarmor")
    public PlotArmorProperties plotArmorProperties() {
        return new PlotArmorProperties();
    }

    @Bean
    public OpenAPI apiDocumentation() {
        return new OpenAPI()
                .info(new Info()
                        .title("PlotArmorDB API")
                        .version("1.0.0")
                        .description("Vector database for AI-powered semantic search"));
    }

    @Data
    public static class PlotArmorProperties {
        private DbConfig db = new DbConfig();
        private CacheConfig cache = new CacheConfig();
        private EmbeddingConfig embedding = new EmbeddingConfig();
        private SearchConfig search = new SearchConfig();

        @Data
        public static class DbConfig {
            private String path = "plotarmor-data";
        }

        @Data
        public static class CacheConfig {
            private int maxSize = 1000;
        }

        @Data
        public static class EmbeddingConfig {
            private int vocabularySize = 10000;
        }

        @Data
        public static class SearchConfig {
            private int batchSize = 1000;
        }
    }
}