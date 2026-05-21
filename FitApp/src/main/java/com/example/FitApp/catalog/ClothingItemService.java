package com.example.FitApp.catalog;

import com.example.FitApp.catalog.dto.ClothingItemListResponse;
import com.example.FitApp.catalog.dto.ClothingItemRequest;
import com.example.FitApp.catalog.dto.ClothingItemResponse;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClothingItemService {

    private static final Set<String> ALLOWED_GENDERS = Set.of("male", "female", "unisex");

    private final ClothingItemRepository repository;
    private final ObjectMapper objectMapper;

    public ClothingItemListResponse getItems(String category, String gender, int page, int limit) {
        Specification<ClothingItem> spec = (root, q, cb) -> cb.conjunction();
        if (category != null && !category.isBlank())
            spec = spec.and((root, q, cb) -> cb.equal(cb.lower(root.get("category")), category.toLowerCase()));
        if (gender != null && !gender.isBlank())
            spec = spec.and((root, q, cb) -> cb.equal(cb.lower(root.get("gender")), gender.toLowerCase()));

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

    public ClothingItemResponse getItem(Long id) {
        ClothingItem item = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clothing item not found"));
        return toResponse(item);
    }

    public ClothingItemResponse createItem(ClothingItemRequest request) {
        validateRequest(request);

        ClothingItem item = ClothingItem.builder()
                .name(request.getName().trim())
                .category(request.getCategory().trim().toLowerCase())
                .gender(request.getGender().trim().toLowerCase())
                .availableSizes(serializeList(request.getAvailableSizes()))
                .imageUrl(request.getImageUrl())
                .sizeChart(serializeMap(request.getSizeChart()))
                .build();

        return toResponse(repository.save(item));
    }

    public ClothingItemResponse updateItem(Long id, ClothingItemRequest request) {
        ClothingItem item = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clothing item not found"));

        if (request.getName() != null && !request.getName().isBlank())
            item.setName(request.getName().trim());
        if (request.getCategory() != null && !request.getCategory().isBlank())
            item.setCategory(request.getCategory().trim().toLowerCase());
        if (request.getGender() != null) {
            if (!ALLOWED_GENDERS.contains(request.getGender().toLowerCase()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gender must be male, female, or unisex");
            item.setGender(request.getGender().toLowerCase());
        }
        if (request.getAvailableSizes() != null)
            item.setAvailableSizes(serializeList(request.getAvailableSizes()));
        if (request.getImageUrl() != null)
            item.setImageUrl(request.getImageUrl());
        if (request.getSizeChart() != null)
            item.setSizeChart(serializeMap(request.getSizeChart()));

        return toResponse(repository.save(item));
    }

    public void deleteItem(Long id) {
        if (!repository.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Clothing item not found");
        repository.deleteById(id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateRequest(ClothingItemRequest request) {
        if (request.getName() == null || request.getName().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        if (request.getCategory() == null || request.getCategory().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "category is required");
        if (request.getGender() == null || !ALLOWED_GENDERS.contains(request.getGender().toLowerCase()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gender must be male, female, or unisex");
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

    private List<String> deserializeList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (JacksonException e) {
            // Fallback: treat as comma-separated
            return Arrays.stream(json.split(",")).map(String::trim).collect(Collectors.toList());
        }
    }

    private ClothingItemResponse toResponse(ClothingItem item) {
        return ClothingItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .category(item.getCategory())
                .gender(item.getGender())
                .availableSizes(deserializeList(item.getAvailableSizes()))
                .imageUrl(item.getImageUrl())
                .sizeChart(item.getSizeChart())  // returned verbatim via @JsonRawValue
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
