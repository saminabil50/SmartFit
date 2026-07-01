package com.example.smartfitapp.network;

import com.example.smartfitapp.model.ClothingItem;
import com.example.smartfitapp.model.ClothingItemListResponse;
import com.example.smartfitapp.model.ClothingItemRequest;
import com.example.smartfitapp.model.ClothingItemSaveResponse;
import com.example.smartfitapp.model.FittingResult;
import com.example.smartfitapp.model.FittingResultRequest;
import com.example.smartfitapp.model.ImageItem;
import com.example.smartfitapp.model.LoginRequest;
import com.example.smartfitapp.model.LoginResponse;
import com.example.smartfitapp.model.MessageResponse;
import com.example.smartfitapp.model.RegisterRequest;
import com.example.smartfitapp.model.SizeChartUpdateRequest;
import com.example.smartfitapp.model.SizeChartUpdateResponse;
import com.example.smartfitapp.model.TryOnResult;
import com.example.smartfitapp.model.TryOnResultListResponse;
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
import retrofit2.http.PUT;
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

    // ── Catalog ───────────────────────────────────────────────────────────────
    @GET("api/v1/clothing-items")
    Call<ClothingItemListResponse> getClothingItems(@Header("Authorization") String bearerToken,
                                                    @Query("category") String category,
                                                    @Query("gender") String gender,
                                                    @Query("page") int page,
                                                    @Query("limit") int limit);

    // ── Admin Catalog ───────────────────────────────────────────────────────
    @GET("api/v1/admin/clothing-items")
    Call<ClothingItemListResponse> getAdminClothingItems(@Header("Authorization") String bearerToken,
                                                         @Query("category") String category,
                                                         @Query("gender") String gender,
                                                         @Query("sizeSystem") String sizeSystem,
                                                         @Query("isActive") Boolean isActive,
                                                         @Query("search") String search,
                                                         @Query("page") int page,
                                                         @Query("limit") int limit);

    @GET("api/v1/admin/clothing-items/{itemId}")
    Call<ClothingItem> getAdminClothingItem(@Header("Authorization") String bearerToken,
                                            @Path("itemId") Long itemId);

    @POST("api/v1/admin/clothing-items")
    Call<ClothingItem> createAdminClothingItem(@Header("Authorization") String bearerToken,
                                               @Body ClothingItemRequest request);

    @PATCH("api/v1/admin/clothing-items/{itemId}")
    Call<ClothingItemSaveResponse> updateAdminClothingItem(@Header("Authorization") String bearerToken,
                                                           @Path("itemId") Long itemId,
                                                           @Body ClothingItemRequest request);

    @DELETE("api/v1/admin/clothing-items/{itemId}")
    Call<MessageResponse> deactivateAdminClothingItem(@Header("Authorization") String bearerToken,
                                                      @Path("itemId") Long itemId);

    @Multipart
    @POST("api/v1/admin/clothing-items/{itemId}/image")
    Call<ClothingItemSaveResponse> uploadAdminClothingItemImage(@Header("Authorization") String bearerToken,
                                                                @Path("itemId") Long itemId,
                                                                @Part MultipartBody.Part image);

    @PUT("api/v1/admin/clothing-items/{itemId}/size-chart")
    Call<SizeChartUpdateResponse> updateAdminSizeChart(@Header("Authorization") String bearerToken,
                                                       @Path("itemId") Long itemId,
                                                       @Body SizeChartUpdateRequest request);

    // ── Virtual Try-On ───────────────────────────────────────────────────────
    @Multipart
    @POST("api/v1/tryon/generate-from-image")
    Call<TryOnResult> generateTryOnFromImage(@Header("Authorization") String bearerToken,
                                             @Part MultipartBody.Part image,
                                             @Part("item_id") RequestBody itemId);

    @GET("api/v1/tryon")
    Call<TryOnResultListResponse> getTryOnResults(@Header("Authorization") String bearerToken);

    @DELETE("api/v1/tryon/{tryOnId}")
    Call<MessageResponse> deleteTryOnResult(@Header("Authorization") String bearerToken,
                                            @Path("tryOnId") Long tryOnId);

    // ── Fitting Results ─────────────────────────────────────────────────────
    @POST("api/v1/fitting-results")
    Call<FittingResult> createFittingResult(@Header("Authorization") String bearerToken,
                                            @Body FittingResultRequest request);

    @DELETE("api/v1/fitting-results/{resultId}")
    Call<MessageResponse> deleteFittingResult(@Header("Authorization") String bearerToken,
                                              @Path("resultId") Long resultId);
}
