package com.example.cataractscan.api

import com.example.cataractscan.api.models.User
import com.example.cataractscan.api.models.AnalysisResult
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*
import com.google.gson.annotations.SerializedName // Import SerializedName

interface ApiService {
    // User Authentication
    @POST("auth/login")
    suspend fun login(
        @Body loginRequest: LoginRequest
    ): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(
        @Body registerRequest: RegisterRequest
    ): Response<RegisterResponse>

    // Forgot Password
    @POST("auth/forgot-password")
    suspend fun forgotPassword(
        @Body forgotRequest: ForgotRequest
    ): Response<ForgotPasswordResponse>

    // Change Password (for logged-in users)
    @PATCH("auth/change-password")
    suspend fun changePassword(
        @Header("Authorization") authorization: String, // Pass "Bearer {token}"
        @Body request: ChangePasswordRequest
    ): Response<ChangePasswordResponse>

    // Reset Password (using token from email link)
    @POST("auth/reset-password") // Tambahkan endpoint ini kembali
    suspend fun resetPassword(
        @Query("token") token: String,
        @Body request: ResetPasswordRequest
    ): Response<ResetPasswordResponse>

    // User Profile
    @GET("user")
    suspend fun getUserProfile(
        @Header("Authorization") authorization: String // Pass "Bearer {token}"
    ): Response<User>

    // Logout
    @POST("auth/logout")
    suspend fun logout(
        @Header("Authorization") authorization: String // Pass "Bearer {token}"
    ): Response<LogoutResponse>

    // Profile update
    @Multipart
    @PATCH("profile/edit")
    suspend fun updateProfile(
        @Header("Authorization") authorization: String, // Pass "Bearer {token}"
        @Part image: MultipartBody.Part?
    ): Response<ProfileUpdateResponse>

    // Image Analysis
    @Multipart
    @POST("user/dashboard/predict")
    suspend fun analyzeImage(
        @Header("Authorization") authorization: String, // Pass "Bearer {token}"
        @Part image: MultipartBody.Part
    ): Response<AnalysisResult>

    // Get Profile Edit (New endpoint)
    @GET("auth/profile/edit")
    suspend fun getProfileEdit(
        @Header("Authorization") token: String
    ): Response<ProfileResponse>

    // History endpoint
    @GET("user/dashboard/history")
    suspend fun getHistory(
        @Header("Authorization") authorization: String // Pass "Bearer {token}"
    ): Response<HistoryResponse>
}

// Request data classes
data class LoginRequest(
    val login: String,
    val password: String
)

data class ForgotRequest(
    val email: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val retype_password: String
)

// KOREKSI DI SINI: ChangePasswordRequest hanya oldPassword dan newPassword
data class ChangePasswordRequest(
    val newPassword: String,
    val retypePassword: String
)

// Tambahkan kembali ResetPasswordRequest
data class ResetPasswordRequest(
    val newPassword: String
)

// Response data classes
data class LoginResponse(
    val message: String,
    val greeting: String,
    val token: String
)

data class RegisterResponse(
    val message: String,
    val user: RegisterUser
)

data class RegisterUser(
    val id: Int,
    val username: String,
    val email: String
)

data class LogoutResponse(
    val message: String
)

data class ProfileUpdateResponse(
    val message: String,
    val user: User
)

data class ForgotPasswordResponse(
    val message: String,
    val resetLink: String? = null
)

// Tambahkan kembali ChangePasswordResponse yang lebih sesuai
data class ChangePasswordResponse(
    val message: String // Cukup message, sesuai respons sukses
)

// Tambahkan kembali ResetPasswordResponse
data class ResetPasswordResponse(
    val message: String
)

// History response data classes
data class HistoryResponse(
    val message: String,
    val history: List<HistoryItem>
)

data class HistoryItem(
    val id: Int,
    val prediction: String,
    val explanation: String,
    val createdAt: String,
    val updatedAt: String,
    val photoUrl: String
)

// Profile Response dan UserProfile yang baru ditambahkan
data class ProfileResponse(
    val message: String,
    val user: UserProfile
)

data class UserProfile(
    val id: Int,
    val username: String,
    val email: String,
    @SerializedName("image_link") // Pastikan ada import com.google.gson.annotations.SerializedName
    val imageLink: String?,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("updatedAt")
    val updatedAt: String
)