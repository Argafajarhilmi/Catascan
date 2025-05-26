package com.example.cataractscan.ui.fragments

import android.content.Intent
import android.os.Bundle
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
import com.example.cataractscan.login.ChangePasswordActivity
import kotlinx.coroutines.launch

// Import UserProfile dari package api Anda
import com.example.cataractscan.api.UserProfile

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferenceManager: PreferenceManager

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
        val username = preferenceManager.getUsername() ?: "User" // Menggunakan getUsername
        val email = preferenceManager.getEmail() ?: "email@example.com"
        val photoUrl = preferenceManager.getProfileImage() // <<< Ambil imageLink dari preferences

        // Set text fields
        binding.tvUsername.text = username
        // binding.tvEmailDetail.text = email // Jika ada TextView untuk email di profil

        // Load profile image with Glide
        Glide.with(this)
            .load(photoUrl) // <<< Muat imageLink di sini
            .apply(RequestOptions
                .circleCropTransform()
                .placeholder(R.drawable.splash_icon) // Placeholder
                .error(R.drawable.splash_icon)) // Gambar error jika gagal
            .into(binding.ivProfileImage)
    }

    private fun loadUserProfile() {
        val token = preferenceManager.getToken()
        if (token.isNullOrEmpty()) {
            return
        }

        lifecycleScope.launch {
            try {
                // Panggil endpoint getProfileEdit untuk mendapatkan UserProfile
                val response = ApiClient.apiService.getProfileEdit("Bearer $token") // Ganti ke getProfileEdit

                if (response.isSuccessful && response.body() != null) {
                    val profileResponse = response.body()!!
                    val userProfile = profileResponse.user // Ambil objek UserProfile

                    // Update UI dengan data terbaru dari API
                    binding.tvUsername.text = userProfile.username // Menggunakan username dari UserProfile

                    // Muat gambar profil dengan Glide dari imageLink API
                    Glide.with(this@ProfileFragment)
                        .load(userProfile.imageLink) // <<< Muat imageLink dari respons API
                        .apply(RequestOptions
                            .circleCropTransform()
                            .placeholder(R.drawable.splash_icon)
                            .error(R.drawable.splash_icon))
                        .into(binding.ivProfileImage)

                    // Simpan UserProfile lengkap ke preferences
                    preferenceManager.saveUserProfile(userProfile) // <<< Simpan UserProfile ke preferences
                } else {
                    // Handle error respons API
                    Toast.makeText(requireContext(), "Gagal memuat profil: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // Handle network error atau exception
                Toast.makeText(requireContext(), "Error memuat profil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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