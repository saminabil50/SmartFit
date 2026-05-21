package com.example.FitApp.catalog;

import com.example.FitApp.catalog.dto.*;
import com.example.FitApp.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/clothing-items")
@RequiredArgsConstructor
public class AdminClothingItemController {

    private final ClothingItemService service;

    @GetMapping
    public ClothingItemListResponse getItems(Authentication authentication,
                                             @RequestParam(required = false) String category,
                                             @RequestParam(required = false) String gender,
                                             @RequestParam(required = false) String sizeSystem,
                                             @RequestParam(required = false) Boolean isActive,
                                             @RequestParam(required = false) String search,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int limit) {
        requireAdmin(authentication);
        return service.getAdminItems(category, gender, sizeSystem, isActive, search, page, limit);
    }

    @GetMapping("/{itemId}")
    public ClothingItemResponse getItem(Authentication authentication,
                                        @PathVariable Long itemId) {
        requireAdmin(authentication);
        return service.getAdminItem(itemId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClothingItemResponse createItem(Authentication authentication,
                                           @RequestBody ClothingItemRequest request) {
        requireAdmin(authentication);
        return service.createItem(request);
    }

    @PatchMapping("/{itemId}")
    public ClothingItemSaveResponse updateItem(Authentication authentication,
                                               @PathVariable Long itemId,
                                               @RequestBody ClothingItemRequest request) {
        requireAdmin(authentication);
        return ClothingItemSaveResponse.builder()
                .message("Clothing item updated successfully")
                .item(service.updateItem(itemId, request))
                .build();
    }

    @DeleteMapping("/{itemId}")
    public Map<String, String> deactivateItem(Authentication authentication,
                                              @PathVariable Long itemId) {
        requireAdmin(authentication);
        service.deactivateItem(itemId);
        return Map.of("message", "Clothing item deactivated successfully");
    }

    @PostMapping(value = "/{itemId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ClothingItemSaveResponse uploadImage(Authentication authentication,
                                                @PathVariable Long itemId,
                                                @RequestParam("image") MultipartFile image) {
        requireAdmin(authentication);
        return ClothingItemSaveResponse.builder()
                .message("Catalog image uploaded successfully")
                .item(service.uploadCatalogImage(itemId, image))
                .build();
    }

    @PutMapping("/{itemId}/size-chart")
    public SizeChartResponse updateSizeChart(Authentication authentication,
                                             @PathVariable Long itemId,
                                             @RequestBody SizeChartRequest request) {
        requireAdmin(authentication);
        return service.updateSizeChart(itemId, request);
    }

    private void requireAdmin(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        if (!"admin".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}
