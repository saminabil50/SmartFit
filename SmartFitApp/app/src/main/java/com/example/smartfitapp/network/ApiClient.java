package com.example.smartfitapp.network;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class ApiClient {

    // Use 10.0.2.2 for Android emulator (maps to host machine's localhost)
    // Change to your machine's LAN IP when testing on a physical device

        public static final String BASE_URL = "http://10.0.2.2:8080/"; // TODO: enable when testing virtual device
//    public static final String BASE_URL = "http://192.168.1.26:8080/";
    public static String fullImageUrl(String relativeUrl) {
        if (relativeUrl == null) return null;
        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) return relativeUrl;
        return BASE_URL.replaceAll("/$", "") + relativeUrl;
    }

    private static ApiService instance;

    public static ApiService get() {
        if (instance == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(2, TimeUnit.MINUTES)
                    .readTimeout(5, TimeUnit.MINUTES)
                    .callTimeout(6, TimeUnit.MINUTES)
                    .build();

            instance = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ApiService.class);
        }
        return instance;
    }
}
