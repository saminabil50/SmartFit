package com.example.FitApp.image;

import com.example.FitApp.image.dto.ImageListResponse;
import com.example.FitApp.image.dto.ImageResponse;
import com.example.FitApp.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImageResponse upload(Authentication authentication,
                                @RequestParam("image") MultipartFile file,
                                @RequestParam(value = "image_type", required = false) String imageType) {
        User user = (User) authentication.getPrincipal();
        return imageService.upload(user, file, imageType);
    }

    @GetMapping
    public ImageListResponse getMyImages(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return imageService.getMyImages(user);
    }

    @GetMapping("/{imageId}")
    public ImageResponse getImage(Authentication authentication,
                                  @PathVariable Long imageId) {
        User user = (User) authentication.getPrincipal();
        return imageService.getImage(user, imageId);
    }

    @DeleteMapping("/{imageId}")
    public Map<String, String> deleteImage(Authentication authentication,
                                           @PathVariable Long imageId) {
        User user = (User) authentication.getPrincipal();
        imageService.deleteImage(user, imageId);
        return Map.of("message", "Image deleted successfully");
    }
}
