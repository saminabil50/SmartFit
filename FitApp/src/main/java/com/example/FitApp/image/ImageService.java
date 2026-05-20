package com.example.FitApp.image;

import com.example.FitApp.image.dto.ImageListResponse;
import com.example.FitApp.image.dto.ImageResponse;
import com.example.FitApp.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("fitting_photo", "profile_photo", "mirror_photo");
    private static final Map<String, String> CONTENT_TYPE_TO_EXT = Map.of(
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    @Value("${app.uploads.dir:uploads}")
    private String uploadsDir;

    private final ImageRepository imageRepository;

    public ImageResponse upload(User user, MultipartFile file, String imageType) {
        if (file == null || file.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is required");

        String contentType = file.getContentType();
        if (contentType == null || !CONTENT_TYPE_TO_EXT.containsKey(contentType))
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Unsupported image type. Allowed types: JPG, PNG, WEBP");

        if (file.getSize() > MAX_FILE_SIZE)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Image file is too large. Maximum allowed size is 5 MB");

        String resolvedType = (imageType == null || imageType.isBlank()) ? "fitting_photo" : imageType.trim();
        if (!ALLOWED_IMAGE_TYPES.contains(resolvedType))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "image_type must be one of: fitting_photo, profile_photo, mirror_photo");

        String ext = CONTENT_TYPE_TO_EXT.get(contentType);
        String filename = UUID.randomUUID().toString() + "." + ext;

        Path uploadPath = Paths.get(uploadsDir).toAbsolutePath();
        try {
            Files.createDirectories(uploadPath);
            Path dest = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), dest);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save image");
        }

        Image image = Image.builder()
                .userId(user.getId())
                .filename(filename)
                .originalFilename(file.getOriginalFilename())
                .contentType(contentType)
                .fileSize(file.getSize())
                .imageType(resolvedType)
                .filePath(uploadPath.resolve(filename).toString())
                .build();

        Image saved = imageRepository.save(image);
        return toResponse(saved);
    }

    public ImageListResponse getMyImages(User user) {
        List<ImageResponse> items = imageRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
        return ImageListResponse.builder().items(items).build();
    }

    public ImageResponse getImage(User user, Long imageId) {
        Image image = imageRepository.findByIdAndUserId(imageId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));
        return toResponse(image);
    }

    public void deleteImage(User user, Long imageId) {
        Image image = imageRepository.findByIdAndUserId(imageId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));
        deleteFile(image.getFilePath());
        imageRepository.delete(image);
    }

    @Transactional
    public void deleteAllUserImages(User user) {
        List<Image> images = imageRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        images.forEach(img -> deleteFile(img.getFilePath()));
        imageRepository.deleteByUserId(user.getId());
    }

    private void deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Handle missing file gracefully
        }
    }

    private ImageResponse toResponse(Image image) {
        return ImageResponse.builder()
                .id(image.getId())
                .filename(image.getFilename())
                .originalFilename(image.getOriginalFilename())
                .contentType(image.getContentType())
                .fileSize(image.getFileSize())
                .imageType(image.getImageType())
                .imageUrl("/uploads/" + image.getFilename())
                .createdAt(image.getCreatedAt())
                .build();
    }
}
