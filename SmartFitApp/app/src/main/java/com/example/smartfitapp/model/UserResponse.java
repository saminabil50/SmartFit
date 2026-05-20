package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

public class UserResponse {
    public Long id;
    @SerializedName("full_name")
    public String fullName;
    public String email;
    @SerializedName("created_at")
    public String createdAt;
}
