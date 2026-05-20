package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

public class ImageItem {
    public Long id;
    public String filename;
    @SerializedName("original_filename")
    public String originalFilename;
    @SerializedName("content_type")
    public String contentType;
    @SerializedName("file_size")
    public Long fileSize;
    @SerializedName("image_type")
    public String imageType;
    @SerializedName("image_url")
    public String imageUrl;
    @SerializedName("created_at")
    public String createdAt;
}
