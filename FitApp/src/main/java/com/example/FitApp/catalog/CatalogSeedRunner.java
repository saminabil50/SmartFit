package com.example.FitApp.catalog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Seeds the catalog from src/main/resources/catalog-seed/ on a fresh database so
 * every clone of the repo shows the same clothing items, instead of relying on a
 * local, untracked database. Only runs when clothing_items is empty, so it never
 * touches or duplicates data on a database that already has items.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogSeedRunner implements CommandLineRunner {

    private static final String SEED_MANIFEST = "catalog-seed/items.json";
    private static final String SEED_IMAGES_DIR = "catalog-seed/images/";

    private final ClothingItemRepository repository;
    private final ObjectMapper objectMapper;

    @Value("${app.uploads.dir:uploads}")
    private String uploadsDir;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) return;

        List<Map<String, Object>> seedItems;
        try (InputStream in = new ClassPathResource(SEED_MANIFEST).getInputStream()) {
            seedItems = objectMapper.readValue(in, new TypeReference<>() {});
        } catch (IOException e) {
            log.warn("No catalog seed data found at {}, skipping catalog seeding", SEED_MANIFEST);
            return;
        }

        Path catalogDir = Paths.get(uploadsDir).toAbsolutePath().normalize().resolve("catalog");
        for (Map<String, Object> seed : seedItems) {
            ClothingItem item = ClothingItem.builder()
                    .name((String) seed.get("name"))
                    .description((String) seed.get("description"))
                    .category((String) seed.get("category"))
                    .gender((String) seed.get("gender"))
                    .brand((String) seed.get("brand"))
                    .sizeSystem((String) seed.get("sizeSystem"))
                    .availableSizes(writeJson(seed.get("availableSizes")))
                    .basePrice(seed.get("basePrice") != null ? ((Number) seed.get("basePrice")).doubleValue() : null)
                    .currency((String) seed.get("currency"))
                    .imageUrl(copySeedImage(catalogDir, (String) seed.get("image")))
                    .sizeChart(writeJson(seed.get("sizeChart")))
                    .isActive(true)
                    .build();
            repository.save(item);
        }
        log.info("Seeded {} catalog items from {}", seedItems.size(), SEED_MANIFEST);
    }

    private String copySeedImage(Path catalogDir, String filename) {
        if (filename == null || filename.isBlank()) return null;
        try {
            Files.createDirectories(catalogDir);
            Path target = catalogDir.resolve(filename);
            if (!Files.exists(target)) {
                try (InputStream in = new ClassPathResource(SEED_IMAGES_DIR + filename).getInputStream()) {
                    Files.copy(in, target);
                }
            }
            return "/uploads/catalog/" + filename;
        } catch (IOException e) {
            log.warn("Failed to copy seed image {}: {}", filename, e.getMessage());
            return null;
        }
    }

    private String writeJson(Object value) {
        if (value == null) return null;
        try { return objectMapper.writeValueAsString(value); }
        catch (JacksonException e) { return null; }
    }
}
