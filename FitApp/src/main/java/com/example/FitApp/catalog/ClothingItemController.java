package com.example.FitApp.catalog;

import com.example.FitApp.catalog.dto.ClothingItemListResponse;
import com.example.FitApp.catalog.dto.ClothingItemRequest;
import com.example.FitApp.catalog.dto.ClothingItemResponse;
import com.example.FitApp.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/clothing-items")
@RequiredArgsConstructor
public class ClothingItemController {

    private final ClothingItemService service;

    @GetMapping
    public ClothingItemListResponse getItems(
            Authentication authentication,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String gender,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        User user = (User) authentication.getPrincipal();
        return service.getItems(user, category, gender, page, limit);
    }

    @GetMapping("/{itemId}")
    public ClothingItemResponse getItem(@PathVariable Long itemId) {
        return service.getItem(itemId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClothingItemResponse createItem(Authentication authentication,
                                           @RequestBody ClothingItemRequest request) {
        requireAdmin(authentication);
        return service.createItem(request);
    }

    @PatchMapping("/{itemId}")
    public ClothingItemResponse updateItem(Authentication authentication,
                                           @PathVariable Long itemId,
                                           @RequestBody ClothingItemRequest request) {
        requireAdmin(authentication);
        return service.updateItem(itemId, request);
    }

    @DeleteMapping("/{itemId}")
    public Map<String, String> deleteItem(Authentication authentication,
                                          @PathVariable Long itemId) {
        requireAdmin(authentication);
        service.deleteItem(itemId);
        return Map.of("message", "Clothing item deactivated successfully");
    }

    private void requireAdmin(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        if (!"admin".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}
