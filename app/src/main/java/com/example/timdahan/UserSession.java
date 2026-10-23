package com.example.timdahan;

import android.content.Context;
import android.content.SharedPreferences;

public class UserSession {

    private static final String PREF_NAME = "TIMdahanUserSession";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_STORE_NAME = "store_name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    public UserSession(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    // Save logged in user details
    public void createSession(int userId, String storeName, String email) {
        String canonicalEmail = BackupManager.getCanonicalEmail(email);
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_STORE_NAME, storeName);
        editor.putString(KEY_EMAIL, canonicalEmail);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    // Get current logged in user ID
    public int getUserId() {
        return pref.getInt(KEY_USER_ID, -1);
    }

    // Get current store name
    public String getStoreName() {
        return pref.getString(KEY_STORE_NAME, "Sari-Sari Store");
    }

    // Get current user email (always canonical lowercase)
    public String getEmail() {
        return BackupManager.getCanonicalEmail(pref.getString(KEY_EMAIL, ""));
    }

    // Check if user is logged in
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    // Clear session on logout
    public void logoutUser() {
        editor.clear();
        editor.apply();
    }
}