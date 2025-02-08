package com.plotarmordb.controller;

import com.plotarmordb.model.*;
import com.plotarmordb.service.TextEmbeddingService;
import com.plotarmordb.service.VectorSearchService;
import com.plotarmordb.storage.VectorStorage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/vectors")
public class VectorController {

    @Autowired
    private VectorStorage storage;

    @Autowired
    private VectorSearchService searchService;

    @Autowired
    private TextEmbeddingService textEmbeddingService;

    @PostMapping
    public ResponseEntity<Vector> createVector(@RequestBody Vector vector) {
        try {
            if (vector.getId() == null) {
                vector.setId(UUID.randomUUID().toString());
            }

            // Get current vector values
            float[] values = vector.getValues();

            // Create padded vector
            float[] paddedValues = new float[10000];
            Arrays.fill(paddedValues, 0.0f); // Fill with zeros for padding

            // Copy original values or truncate if too long
            if (values != null) {
                int copyLength = Math.min(values.length, 10000);
                System.arraycopy(values, 0, paddedValues, 0, copyLength);

                // Normalize the padded vector
                normalizeVector(paddedValues);
            }

            // Update vector with padded values
            vector.setValues(paddedValues);

            // Store the padded vector
            storage.store(vector);
            return ResponseEntity.ok(vector);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private void normalizeVector(float[] vector) {
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

    @GetMapping("/{id}")
    public ResponseEntity<Vector> getVector(@PathVariable String id) {
        try {
            return storage.retrieve(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVector(@PathVariable String id) {
        try {
            storage.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/search")
    public ResponseEntity<List<SearchResult>> search(@RequestBody SearchRequest request) {
        try {
            List<SearchResult> results = searchService.search(
                    request.getQueryVector(),
                    request.getTopK(),
                    request.getFilter()
            );
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/text")
    public ResponseEntity<Vector> createVectorFromText(@RequestBody TextRequest request) {
        try {
            // Generate embedding from text
            float[] embedding = textEmbeddingService.generateEmbedding(request.getText());

            // Create new vector with generated embedding
            Vector vector = new Vector();
            vector.setId(UUID.randomUUID().toString());
            vector.setValues(embedding);
            vector.setMetadata(request.getMetadata());

            // Store the vector
            storage.store(vector);

            return ResponseEntity.ok(vector);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/search/text")
    public ResponseEntity<List<SearchResult>> searchByText(@RequestBody TextSearchRequest request) {
        try {
            List<SearchResult> results = searchService.searchByText(request);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}