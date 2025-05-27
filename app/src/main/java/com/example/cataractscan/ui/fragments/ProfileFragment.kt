package com.example.cataractscan.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.cataractscan.R
import com.example.cataractscan.databinding.FragmentProfileBinding
import com.example.cataractscan.login.LoginActivity
import com.example.cataractscan.profile.EditProfileActivity
import com.example.cataractscan.utils.PreferenceManager
import com.example.cataractscan.api.ApiClient
import com.example.cataractscan.api.ProfileEditUser
import com.example.cataractscan.login.ChangePasswordActivity
// Import UserProfile dari package api Anda (PASTIKAN INI YANG DIGUNAKAN)

import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferenceManager: PreferenceManager

    companion object {
        private const val TAG = "ProfileFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferenceManager = PreferenceManager(requireContext())

        setupUI() // Panggil setupUI di sini untuk menampilkan data awal dari preferences
        setupListeners()
        loadUserProfile() // Panggil loadUserProfile untuk memuat data terbaru dari API
    }

    override fun onResume() {
        super.onResume()
        // Refresh profile data when returning to this fragment
        loadUserProfile()
    }

    private fun setupUI() {
        // Ambil data dari preferences (yang mungkin sudah ada dari sesi sebelumnya)
        val username = preferenceManager.getUsername() ?: "User"
        val email = preferenceManager.getEmail() ?: "email@example.com"
        val photoUrl = preferenceManager.getProfileImage()

        Log.d(TAG, "Setting up UI - username: $username, photoUrl: $photoUrl")

        // Set text fields
        binding.tvUsername.text = username
        // binding.tvEmailDetail.text = email // Jika ada TextView untuk email di profil

        // Load profile image with Glide
        loadProfileImage(photoUrl)
    }

    private fun loadProfileImage(imageUrl: String?) {
        Log.d(TAG, "Loading profile image: $imageUrl")
        Glide.with(this)
            .load(imageUrl)
            .apply(RequestOptions
                .circleCropTransform()
                .placeholder(R.drawable.splash_icon)
                .error(R.drawable.splash_icon))
            .into(binding.ivProfileImage)
    }

    private fun loadUserProfile() {
        val token = preferenceManager.getToken()
        if (token.isNullOrEmpty()) {
            Log.e(TAG, "Token is null or empty")
            return
        }

        Log.d(TAG, "Loading user profile from API...")

        lifecycleScope.launch {
            try {
                // Panggil endpoint getProfileEdit untuk mendapatkan UserProfile
                val response = ApiClient.apiService.getProfileEdit("Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    val profileResponse = response.body()!!
                    val userProfile = profileResponse.user // Ambil objek UserProfile

                    Log.d(TAG, "Profile loaded successfully from API: ${userProfile.username}, imageLink: ${userProfile.imageLink}")

                    // Update UI dengan data terbaru dari API
                    updateUIWithProfileData(userProfile) // <--- Tipe parameter diubah

                    // Simpan profile ke preferences
                    saveProfileToPreferences(userProfile) // <--- Tipe parameter diubah

                } else {
                    Log.e(TAG, "Failed to load profile: ${response.code()}")
                    Toast.makeText(requireContext(), "Gagal memuat profil: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading profile: ${e.message}")
                Toast.makeText(requireContext(), "Error memuat profil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Ubah tipe parameter dari ProfileEditUser menjadi UserProfile
    private fun updateUIWithProfileData(userProfile: ProfileEditUser) { // <--- KOREKSI DI SINI
        Log.d(TAG, "Updating UI with profile data: ${userProfile.username}")

        // Update username
        binding.tvUsername.text = userProfile.username

        // Load profile image
        loadProfileImage(userProfile.imageLink)
    }

    // Ubah tipe parameter dari ProfileEditUser menjadi UserProfile
    private fun saveProfileToPreferences(userProfile: ProfileEditUser) { // <--- KOREKSI DI SINI
        Log.d(TAG, "Attempting to save profile to preferences...")

        try {
            // Panggil metode saveUserProfile yang menerima UserProfile
            preferenceManager.saveUserProfile(userProfile)
            Log.d(TAG, "Profile saved successfully using saveUserProfile(UserProfile) method")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving profile using saveUserProfile(UserProfile) method: ${e.message}")

            // Fallback: Simpan secara manual jika ada masalah dengan overload method
            Log.d(TAG, "Saving profile manually using individual methods as fallback...")
            try {
                preferenceManager.saveUsername(userProfile.username)
                if (!userProfile.email.isNullOrEmpty()) {
                    preferenceManager.saveEmail(userProfile.email)
                }
                if (!userProfile.imageLink.isNullOrEmpty()) {
                    preferenceManager.saveProfileImage(userProfile.imageLink)
                } else {
                    Log.d(TAG, "No image link to save manually")
                }
                Log.d(TAG, "Profile saved manually successfully")
            } catch (manualException: Exception) {
                Log.e(TAG, "Error saving profile manually: ${manualException.message}")
            }
        }

        // Verifikasi bahwa data telah disimpan
        verifyProfileSaved()
    }

    private fun verifyProfileSaved() {
        val savedUsername = preferenceManager.getUsername()
        val savedImageUrl = preferenceManager.getProfileImage()
        Log.d(TAG, "Verification - saved username: $savedUsername, saved image: $savedImageUrl")
    }

    private fun setupListeners() {
        // Edit profile button
        binding.btnEditProfile.setOnClickListener {
            val intent = Intent(requireActivity(), EditProfileActivity::class.java)
            startActivity(intent)
        }

        // Change password button
        binding.btnChangePassword.setOnClickListener {
            val token = preferenceManager.getToken()

            if (token.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Sesi telah berakhir, silakan login kembali", Toast.LENGTH_LONG).show()
                redirectToLogin()
                return@setOnClickListener
            }

            val intent = Intent(requireContext(), ChangePasswordActivity::class.java)
            startActivity(intent)
        }

        // Logout button
        binding.btnLogout.setOnClickListener {
            handleLogout()
        }
    }

    private fun handleLogout() {
        val token = preferenceManager.getToken()
        if (token.isNullOrEmpty()) {
            clearSessionAndNavigateToLogin()
            return
        }

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.logout("Bearer $token")
                clearSessionAndNavigateToLogin()
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Logout berhasil", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                clearSessionAndNavigateToLogin()
            }
        }
    }

    private fun clearSessionAndNavigateToLogin() {
        preferenceManager.logout()
        redirectToLogin()
    }

    private fun redirectToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}