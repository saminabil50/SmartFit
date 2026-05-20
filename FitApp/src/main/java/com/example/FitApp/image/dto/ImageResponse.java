package com.example.FitApp.image.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageResponse {
    private Long id;
    private String filename;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private String imageType;
    private String imageUrl;
    private LocalDateTime createdAt;
}
