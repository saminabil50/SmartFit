package com.example.smartfitapp.auth;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.smartfitapp.model.UserResponse;
import com.google.gson.Gson;

public class AuthManager {

    private static final String PREFS_NAME = "smartfit_auth";
    private static final String KEY_TOKEN = "access_token";
    private static final String KEY_USER = "user_json";

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public AuthManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public void saveUser(UserResponse user) {
        prefs.edit().putString(KEY_USER, gson.toJson(user)).apply();
    }

    public UserResponse getCurrentUser() {
        String json = prefs.getString(KEY_USER, null);
        if (json == null) return null;
        return gson.fromJson(json, UserResponse.class);
    }

    public String getBearerToken() {
        String token = getToken();
        return token != null ? "Bearer " + token : null;
    }

    public void logout() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_USER).apply();
    }
}
