package com.example.smartfitapp.network;

import com.example.smartfitapp.model.ImageItem;
import com.example.smartfitapp.model.ImageListResponse;
import com.example.smartfitapp.model.LoginRequest;
import com.example.smartfitapp.model.LoginResponse;
import com.example.smartfitapp.model.MessageResponse;
import com.example.smartfitapp.model.RegisterRequest;
import com.example.smartfitapp.model.UpdateProfileRequest;
import com.example.smartfitapp.model.UpdateProfileResponse;
import com.example.smartfitapp.model.UserResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────
    @POST("api/v1/auth/register")
    Call<UserResponse> register(@Body RegisterRequest request);

    @POST("api/v1/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @GET("api/v1/auth/me")
    Call<UserResponse> getMe(@Header("Authorization") String bearerToken);

    // ── Profile ───────────────────────────────────────────────────────────────
    @GET("api/v1/users/me")
    Call<UserResponse> getMyProfile(@Header("Authorization") String bearerToken);

    @PATCH("api/v1/users/me")
    Call<UpdateProfileResponse> updateProfile(@Header("Authorization") String bearerToken,
                                              @Body UpdateProfileRequest request);

    @DELETE("api/v1/users/me")
    Call<MessageResponse> deleteAccount(@Header("Authorization") String bearerToken);

    // ── Images ────────────────────────────────────────────────────────────────
    @Multipart
    @POST("api/v1/images/upload")
    Call<ImageItem> uploadImage(@Header("Authorization") String bearerToken,
                                @Part MultipartBody.Part image,
                                @Part("image_type") RequestBody imageType);

    @GET("api/v1/images")
    Call<ImageListResponse> getMyImages(@Header("Authorization") String bearerToken);

    @GET("api/v1/images/{imageId}")
    Call<ImageItem> getImage(@Header("Authorization") String bearerToken,
                             @Path("imageId") Long imageId);

    @DELETE("api/v1/images/{imageId}")
    Call<MessageResponse> deleteImage(@Header("Authorization") String bearerToken,
                                      @Path("imageId") Long imageId);
}
