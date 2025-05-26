package com.example.cataractscan.utils

import android.content.Context
import android.content.SharedPreferences
// Import model User dari package API Anda (jika masih digunakan)
import com.example.cataractscan.api.models.User
// Import model UserProfile dari package API Anda
import com.example.cataractscan.api.UserProfile

class PreferenceManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("CataractScanPrefs", Context.MODE_PRIVATE)

    // Auth related methods
    fun saveToken(token: String) {
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)
    }

    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrEmpty()
    }

    fun logout() {
        sharedPreferences.edit().clear().apply()
    }

    // User profile methods
    // Metode ini untuk menyimpan profil dari model User (misal dari /user endpoint)
    fun saveUserProfile(user: com.example.cataractscan.api.models.User) {
        sharedPreferences.edit().apply {
            putString(KEY_USER_ID, user.id)
            putString(KEY_NAME, user.name)
            putString(KEY_EMAIL, user.email)
            putString(KEY_PROFILE_IMAGE, user.profileImage) // Asumsi user.profileImage sudah ada
            user.token?.let { putString(KEY_TOKEN, it) }
            apply()
        }
    }

    // Metode ini untuk menyimpan profil dari model UserProfile (misal dari /auth/profile/edit endpoint)
    fun saveUserProfile(userProfile: UserProfile) { // Overload method
        val editor = sharedPreferences.edit()
        editor.putInt(KEY_USER_ID_INT, userProfile.id)
        editor.putString(KEY_USERNAME, userProfile.username)
        editor.putString(KEY_EMAIL, userProfile.email)
        editor.putString(KEY_PROFILE_IMAGE, userProfile.imageLink) // <<< imageLink disimpan di sini
        editor.putString(KEY_CREATED_AT, userProfile.createdAt)
        editor.putString(KEY_UPDATED_AT, userProfile.updatedAt)
        editor.apply()
    }

    fun saveLoginCredentials(email: String, password: String) {
        sharedPreferences.edit().apply {
            putString(KEY_LOGIN_EMAIL, email)
            putString(KEY_LOGIN_PASSWORD, password)
            apply()
        }
    }

    fun getLoginEmail(): String? {
        return sharedPreferences.getString(KEY_LOGIN_EMAIL, null)
    }

    fun getLoginPassword(): String? {
        return sharedPreferences.getString(KEY_LOGIN_PASSWORD, null)
    }

    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    fun getUserIdInt(): Int {
        return sharedPreferences.getInt(KEY_USER_ID_INT, 0)
    }

    fun getName(): String? { // Ini mungkin perlu disinkronkan dengan getUsername()
        return sharedPreferences.getString(KEY_NAME, null)
    }

    fun getUsername(): String? { // Untuk UserProfile model
        return sharedPreferences.getString(KEY_USERNAME, null)
    }

    fun getEmail(): String? {
        return sharedPreferences.getString(KEY_EMAIL, null)
    }

    fun getProfileImage(): String? {
        return sharedPreferences.getString(KEY_PROFILE_IMAGE, null) // <<< imageLink diambil dari sini
    }

    fun getCreatedAt(): String? {
        return sharedPreferences.getString(KEY_CREATED_AT, null)
    }

    fun getUpdatedAt(): String? {
        return sharedPreferences.getString(KEY_UPDATED_AT, null)
    }

    companion object {
        private const val KEY_TOKEN = "key_token"
        private const val KEY_USER_ID = "key_user_id" // Jika ID dari model User berupa String
        private const val KEY_USER_ID_INT = "user_id_int" // Untuk ID Integer dari UserProfile
        private const val KEY_NAME = "key_name" // Dari model User
        private const val KEY_USERNAME = "username" // Dari model UserProfile
        private const val KEY_EMAIL = "key_email"
        private const val KEY_PROFILE_IMAGE = "key_profile_image" // Kunci untuk menyimpan imageLink
        private const val KEY_LOGIN_EMAIL = "key_login_email"
        private const val KEY_LOGIN_PASSWORD = "key_login_password"
        private const val KEY_CREATED_AT = "created_at"
        private const val KEY_UPDATED_AT = "updated_at"
    }
}