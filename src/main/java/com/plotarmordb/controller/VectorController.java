package com.plotarmordb.controller;

import com.plotarmordb.model.SearchRequest;
import com.plotarmordb.model.SearchResult;
import com.plotarmordb.model.TextRequest;
import com.plotarmordb.model.Vector;
import com.plotarmordb.service.TextEmbeddingService;
import com.plotarmordb.service.VectorSearchService;
import com.plotarmordb.storage.VectorStorage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/vectors")
public class VectorController {
    private final VectorStorage storage;

    @Autowired
    private VectorSearchService searchService;

    @Autowired
    private TextEmbeddingService textEmbeddingService;

    public VectorController(VectorStorage storage) {
        this.storage = storage;
    }

    @PostMapping
    public ResponseEntity<Vector> createVector(@RequestBody Vector vector) {
        try {
            // Generate ID if not provided
            if (vector.getId() == null) {
                vector.setId(UUID.randomUUID().toString());
            }

            storage.store(vector);
            return ResponseEntity.ok(vector);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
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
                    request.getTopK()
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
}