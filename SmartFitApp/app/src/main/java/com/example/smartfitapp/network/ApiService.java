package com.example.smartfitapp.network;

import com.example.smartfitapp.model.LoginRequest;
import com.example.smartfitapp.model.LoginResponse;
import com.example.smartfitapp.model.RegisterRequest;
import com.example.smartfitapp.model.UserResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ApiService {

    @POST("api/v1/auth/register")
    Call<UserResponse> register(@Body RegisterRequest request);

    @POST("api/v1/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @GET("api/v1/auth/me")
    Call<UserResponse> getMe(@Header("Authorization") String bearerToken);
}
