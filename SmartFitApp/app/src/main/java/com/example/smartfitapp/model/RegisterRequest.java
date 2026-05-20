package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    @SerializedName("full_name")
    public String fullName;
    public String email;
    public String password;

    public RegisterRequest(String fullName, String email, String password) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
    }
}
