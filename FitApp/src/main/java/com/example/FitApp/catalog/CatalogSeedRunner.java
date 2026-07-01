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
 * Seeds the catalog from src/main/resources/catalog-seed/ so every clone of the repo
 * shows the same clothing items, instead of relying on a local, untracked database.
 * Each seed item is matched against existing rows by its image URL (unique per seed
 * item), so only items missing from this database get inserted — it never touches or
 * duplicates rows that are already present, whether they came from a previous seed run
 * or were added locally through the admin screen.
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
        List<Map<String, Object>> seedItems;
        try (InputStream in = new ClassPathResource(SEED_MANIFEST).getInputStream()) {
            seedItems = objectMapper.readValue(in, new TypeReference<>() {});
        } catch (IOException e) {
            log.warn("No catalog seed data found at {}, skipping catalog seeding", SEED_MANIFEST);
            return;
        }

        Path catalogDir = Paths.get(uploadsDir).toAbsolutePath().normalize().resolve("catalog");
        int inserted = 0;
        for (Map<String, Object> seed : seedItems) {
            String filename = (String) seed.get("image");
            if (filename == null || filename.isBlank()) {
                log.warn("Skipping seed item '{}' — no image filename to key off of", seed.get("name"));
                continue;
            }
            String imageUrl = "/uploads/catalog/" + filename;
            if (repository.existsByImageUrl(imageUrl)) continue;

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
                    .imageUrl(copySeedImage(catalogDir, filename, imageUrl))
                    .sizeChart(writeJson(seed.get("sizeChart")))
                    .isActive(true)
                    .build();
            repository.save(item);
            inserted++;
        }
        if (inserted > 0) {
            log.info("Seeded {} missing catalog item(s) from {}", inserted, SEED_MANIFEST);
        }
    }

    private String copySeedImage(Path catalogDir, String filename, String imageUrl) {
        try {
            Files.createDirectories(catalogDir);
            Path target = catalogDir.resolve(filename);
            if (!Files.exists(target)) {
                try (InputStream in = new ClassPathResource(SEED_IMAGES_DIR + filename).getInputStream()) {
                    Files.copy(in, target);
                }
            }
            return imageUrl;
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
