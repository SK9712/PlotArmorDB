package com.plotarmor.controller;

import com.plotarmor.model.Vector;
import com.plotarmor.storage.VectorStorage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/vectors")
public class VectorController {
    private final VectorStorage storage;

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
}