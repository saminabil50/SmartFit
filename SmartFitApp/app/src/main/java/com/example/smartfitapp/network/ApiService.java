package com.example.smartfitapp.network;

import com.example.smartfitapp.model.ClothingItem;
import com.example.smartfitapp.model.ClothingItemListResponse;
import com.example.smartfitapp.model.ClothingItemRequest;
import com.example.smartfitapp.model.EstimateMeasurementRequest;
import com.example.smartfitapp.model.ImageItem;
import com.example.smartfitapp.model.ImageListResponse;
import com.example.smartfitapp.model.LoginRequest;
import com.example.smartfitapp.model.LoginResponse;
import com.example.smartfitapp.model.MeasurementListResponse;
import com.example.smartfitapp.model.MeasurementResponse;
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
import retrofit2.http.Query;

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

    // ── Measurements ──────────────────────────────────────────────────────────
    @POST("api/v1/measurements/estimate")
    Call<MeasurementResponse> estimateMeasurements(@Header("Authorization") String bearerToken,
                                                   @Body EstimateMeasurementRequest request);

    @GET("api/v1/measurements")
    Call<MeasurementListResponse> getMyMeasurements(@Header("Authorization") String bearerToken);

    @GET("api/v1/measurements/{measurementId}")
    Call<MeasurementResponse> getMeasurement(@Header("Authorization") String bearerToken,
                                             @Path("measurementId") Long measurementId);

    @DELETE("api/v1/measurements/{measurementId}")
    Call<MessageResponse> deleteMeasurement(@Header("Authorization") String bearerToken,
                                            @Path("measurementId") Long measurementId);

    // ── Catalog ───────────────────────────────────────────────────────────────
    @GET("api/v1/clothing-items")
    Call<ClothingItemListResponse> getClothingItems(@Header("Authorization") String bearerToken,
                                                    @Query("category") String category,
                                                    @Query("gender") String gender,
                                                    @Query("page") int page,
                                                    @Query("limit") int limit);

    @GET("api/v1/clothing-items/{itemId}")
    Call<ClothingItem> getClothingItem(@Header("Authorization") String bearerToken,
                                       @Path("itemId") Long itemId);

    @POST("api/v1/clothing-items")
    Call<ClothingItem> createClothingItem(@Header("Authorization") String bearerToken,
                                          @Body ClothingItemRequest request);

    @PATCH("api/v1/clothing-items/{itemId}")
    Call<ClothingItem> updateClothingItem(@Header("Authorization") String bearerToken,
                                          @Path("itemId") Long itemId,
                                          @Body ClothingItemRequest request);

    @DELETE("api/v1/clothing-items/{itemId}")
    Call<MessageResponse> deleteClothingItem(@Header("Authorization") String bearerToken,
                                             @Path("itemId") Long itemId);
}
