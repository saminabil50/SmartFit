package com.example.FitApp.catalog;

import com.example.FitApp.catalog.dto.ClothingItemListResponse;
import com.example.FitApp.catalog.dto.ClothingItemRequest;
import com.example.FitApp.catalog.dto.ClothingItemResponse;
import com.example.FitApp.catalog.dto.SizeChartRequest;
import com.example.FitApp.catalog.dto.SizeChartResponse;
import com.example.FitApp.preferences.PreferencesService;
import com.example.FitApp.preferences.dto.UserPreferencesResponse;
import com.example.FitApp.user.User;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClothingItemService {

    private static final Set<String> ALLOWED_GENDERS = Set.of("male", "female", "unisex");
    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "tshirt", "shirt", "hoodie", "jacket", "sweater", "pants",
            "jeans", "shorts", "skirt", "dress", "shoes", "accessories"
    );
    private static final Set<String> ALLOWED_SIZE_SYSTEMS = Set.of("US", "UK", "EU", "INT");
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final Map<String, String> CONTENT_TYPE_TO_EXT = Map.of(
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    @Value("${app.uploads.dir:uploads}")
    private String uploadsDir;

    private final ClothingItemRepository repository;
    private final PreferencesService preferencesService;
    private final ObjectMapper objectMapper;

    public ClothingItemListResponse getItems(String category, String gender, int page, int limit) {
        return getItems(null, category, gender, page, limit);
    }

    public ClothingItemListResponse getItems(User user, String category, String gender, int page, int limit) {
        UserPreferencesResponse preferences = user == null ? null : preferencesService.findExisting(user);
        String effectiveGender = gender;
        List<String> effectiveCategories = List.of();
        boolean appliedPreferences = false;

        if (isBlank(category) && isBlank(gender) && preferences != null) {
            if (!"unisex".equals(preferences.getPreferredGenderCategory())) {
                effectiveGender = preferences.getPreferredGenderCategory();
                appliedPreferences = true;
            }
            if (preferences.getPreferredCategories() != null && !preferences.getPreferredCategories().isEmpty()) {
                effectiveCategories = preferences.getPreferredCategories();
                appliedPreferences = true;
            }
        }

        ClothingItemListResponse response = queryItems(category, effectiveGender, effectiveCategories, page, limit);
        if (appliedPreferences && response.getItems().isEmpty()) {
            return queryItems(category, gender, List.of(), page, limit);
        }
        return response;
    }

    private ClothingItemListResponse queryItems(String category, String gender, List<String> categories, int page, int limit) {
        Specification<ClothingItem> spec = (root, q, cb) -> cb.conjunction();
        spec = spec.and((root, q, cb) -> cb.isTrue(root.get("isActive")));
        if (category != null && !category.isBlank())
            spec = spec.and((root, q, cb) -> cb.equal(cb.lower(root.get("category")), category.toLowerCase()));
        if (categories != null && !categories.isEmpty())
            spec = spec.and((root, q, cb) -> cb.lower(root.get("category")).in(categories));
        if (gender != null && !gender.isBlank())
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.equal(cb.lower(root.get("gender")), gender.toLowerCase()),
                    cb.equal(cb.lower(root.get("gender")), "unisex")
            ));

        int safeLimit = Math.min(Math.max(limit, 1), 100);
        Page<ClothingItem> result = repository.findAll(spec,
                PageRequest.of(page, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<ClothingItemResponse> items = result.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ClothingItemListResponse.builder()
                .items(items)
                .total(result.getTotalElements())
                .page(page)
                .limit(safeLimit)
                .build();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public ClothingItemResponse getItem(Long id) {
        ClothingItem item = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clothing item not found"));
        if (!Boolean.TRUE.equals(item.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Clothing item not found");
        }
        return toResponse(item);
    }

    public ClothingItemListResponse getAdminItems(String category, String gender, String sizeSystem,
                                                  Boolean isActive, String search, int page, int limit) {
        Specification<ClothingItem> spec = (root, q, cb) -> cb.conjunction();
        if (!isBlank(category))
            spec = spec.and((root, q, cb) -> cb.equal(cb.lower(root.get("category")), category.toLowerCase()));
        if (!isBlank(gender))
            spec = spec.and((root, q, cb) -> cb.equal(cb.lower(root.get("gender")), gender.toLowerCase()));
        if (!isBlank(sizeSystem))
            spec = spec.and((root, q, cb) -> cb.equal(root.get("sizeSystem"), sizeSystem.toUpperCase()));
        if (isActive != null)
            spec = spec.and((root, q, cb) -> isActive ? cb.isTrue(root.get("isActive")) : cb.isFalse(root.get("isActive")));
        if (!isBlank(search)) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("name")), pattern));
        }

        int safeLimit = Math.min(Math.max(limit, 1), 100);
        Page<ClothingItem> result = repository.findAll(spec,
                PageRequest.of(Math.max(page, 0), safeLimit, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ClothingItemListResponse.builder()
                .items(result.getContent().stream().map(this::toResponse).toList())
                .total(result.getTotalElements())
                .page(Math.max(page, 0))
                .limit(safeLimit)
                .build();
    }

    public ClothingItemResponse getAdminItem(Long id) {
        return toResponse(repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clothing item not found")));
    }

    public ClothingItemResponse createItem(ClothingItemRequest request) {
        validateRequest(request);

        ClothingItem item = ClothingItem.builder()
                .name(request.getName().trim())
                .description(trimToNull(request.getDescription()))
                .category(request.getCategory().trim().toLowerCase())
                .gender(request.getGender().trim().toLowerCase())
                .brand(trimToNull(request.getBrand()))
                .sizeSystem(request.getSizeSystem().trim().toUpperCase())
                .availableSizes(serializeList(request.getAvailableSizes()))
                .basePrice(request.getBasePrice())
                .currency(trimToNull(request.getCurrency()))
                .imageUrl(request.getImageUrl())
                .sizeChart(serializeMap(request.getSizeChart()))
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return toResponse(repository.save(item));
    }

    public ClothingItemResponse updateItem(Long id, ClothingItemRequest request) {
        ClothingItem item = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clothing item not found"));

        if (request.getName() != null && !request.getName().isBlank())
            item.setName(request.getName().trim());
        if (request.getDescription() != null)
            item.setDescription(trimToNull(request.getDescription()));
        if (request.getCategory() != null && !request.getCategory().isBlank())
            item.setCategory(validateCategory(request.getCategory()));
        if (request.getGender() != null) {
            item.setGender(validateGender(request.getGender()));
        }
        if (request.getBrand() != null)
            item.setBrand(trimToNull(request.getBrand()));
        if (request.getSizeSystem() != null)
            item.setSizeSystem(validateSizeSystem(request.getSizeSystem()));
        if (request.getAvailableSizes() != null) {
            validateAvailableSizes(request.getAvailableSizes());
            item.setAvailableSizes(serializeList(request.getAvailableSizes()));
        }
        if (request.getBasePrice() != null) {
            if (request.getBasePrice() <= 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "base_price must be positive");
            item.setBasePrice(request.getBasePrice());
        }
        if (request.getCurrency() != null)
            item.setCurrency(trimToNull(request.getCurrency()));
        if (request.getImageUrl() != null)
            item.setImageUrl(request.getImageUrl());
        if (request.getSizeChart() != null)
            item.setSizeChart(serializeMap(request.getSizeChart()));
        if (request.getIsActive() != null)
            item.setIsActive(request.getIsActive());

        return toResponse(repository.save(item));
    }

    public void deleteItem(Long id) {
        deactivateItem(id);
    }

    public void deactivateItem(Long id) {
        ClothingItem item = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clothing item not found"));
        item.setIsActive(false);
        repository.save(item);
    }

    public ClothingItemResponse uploadCatalogImage(Long id, MultipartFile file) {
        ClothingItem item = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clothing item not found"));
        if (file == null || file.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is required");
        String contentType = file.getContentType();
        if (contentType == null || !CONTENT_TYPE_TO_EXT.containsKey(contentType))
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Unsupported image type. Allowed types: JPG, PNG, WEBP");
        if (file.getSize() > MAX_IMAGE_SIZE)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Image file is too large. Maximum allowed size is 5 MB");

        String filename = UUID.randomUUID() + "." + CONTENT_TYPE_TO_EXT.get(contentType);
        Path catalogDir = Paths.get(uploadsDir).toAbsolutePath().normalize().resolve("catalog");
        try {
            Files.createDirectories(catalogDir);
            Files.copy(file.getInputStream(), catalogDir.resolve(filename));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to manage clothing item");
        }

        item.setImageUrl("/uploads/catalog/" + filename);
        return toResponse(repository.save(item));
    }

    public SizeChartResponse updateSizeChart(Long id, SizeChartRequest request) {
        ClothingItem item = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clothing item not found"));
        List<Map<String, Object>> rows = request == null ? null : request.getSizeChart();
        validateSizeChart(rows);

        List<String> sizes = rows.stream()
                .map(row -> String.valueOf(row.get("size")).trim())
                .distinct()
                .toList();
        item.setAvailableSizes(serializeList(sizes));
        item.setSizeChart(serializeListOfMaps(rows));
        ClothingItem saved = repository.save(item);
        return SizeChartResponse.builder()
                .message("Size chart updated successfully")
                .itemId(saved.getId())
                .availableSizes(sizes)
                .sizeChart(saved.getSizeChart())
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateRequest(ClothingItemRequest request) {
        if (request.getName() == null || request.getName().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        validateCategory(request.getCategory());
        validateGender(request.getGender());
        validateSizeSystem(request.getSizeSystem());
        validateAvailableSizes(request.getAvailableSizes());
        if (request.getBasePrice() != null && request.getBasePrice() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "base_price must be positive");
    }

    private String validateCategory(String category) {
        if (category == null || !ALLOWED_CATEGORIES.contains(category.trim().toLowerCase()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category");
        return category.trim().toLowerCase();
    }

    private String validateGender(String gender) {
        if (gender == null || !ALLOWED_GENDERS.contains(gender.trim().toLowerCase()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid gender");
        return gender.trim().toLowerCase();
    }

    private String validateSizeSystem(String sizeSystem) {
        if (sizeSystem == null || !ALLOWED_SIZE_SYSTEMS.contains(sizeSystem.trim().toUpperCase()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid size_system");
        return sizeSystem.trim().toUpperCase();
    }

    private void validateAvailableSizes(List<String> sizes) {
        if (sizes == null || sizes.stream().noneMatch(size -> size != null && !size.isBlank()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "available_sizes must be a non-empty array");
    }

    private void validateSizeChart(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid size chart");
        }
        for (Map<String, Object> row : rows) {
            if (row.get("size") == null || String.valueOf(row.get("size")).isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid size chart");
            }
            validateRange(row, "chest_cm_min", "chest_cm_max");
            validateRange(row, "waist_cm_min", "waist_cm_max");
            validateRange(row, "hip_cm_min", "hip_cm_max");
            validateRange(row, "shoulder_width_cm_min", "shoulder_width_cm_max");
            validateRange(row, "inseam_cm_min", "inseam_cm_max");
        }
    }

    private void validateRange(Map<String, Object> row, String minKey, String maxKey) {
        Double min = numberValue(row.get(minKey));
        Double max = numberValue(row.get(maxKey));
        if (min != null && max != null && min > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid size chart");
        }
    }

    private String serializeList(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        try { return objectMapper.writeValueAsString(list); }
        catch (JacksonException e) { return null; }
    }

    private String serializeMap(Object map) {
        if (map == null) return null;
        try { return objectMapper.writeValueAsString(map); }
        catch (JacksonException e) { return null; }
    }

    private String serializeListOfMaps(List<Map<String, Object>> list) {
        try { return objectMapper.writeValueAsString(list); }
        catch (JacksonException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to manage clothing item");
        }
    }

    private List<String> deserializeList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (JacksonException e) {
            // Fallback: treat as comma-separated
            return Arrays.stream(json.split(",")).map(String::trim).collect(Collectors.toList());
        }
    }

    private Double numberValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.doubleValue();
        if (value instanceof String string && !string.isBlank()) {
            try { return Double.parseDouble(string); }
            catch (NumberFormatException ignored) { return null; }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private ClothingItemResponse toResponse(ClothingItem item) {
        return ClothingItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .category(item.getCategory())
                .gender(item.getGender())
                .brand(item.getBrand())
                .sizeSystem(item.getSizeSystem())
                .availableSizes(deserializeList(item.getAvailableSizes()))
                .basePrice(item.getBasePrice())
                .currency(item.getCurrency())
                .imageUrl(item.getImageUrl())
                .sizeChart(item.getSizeChart())  // returned verbatim via @JsonRawValue
                .isActive(item.getIsActive())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
