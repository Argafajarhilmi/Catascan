package com.example.cataractscan.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.cataractscan.R
import com.example.cataractscan.databinding.FragmentHomeBinding
import com.example.cataractscan.utils.PreferenceManager
import com.example.cataractscan.api.ApiClient
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Set fullscreen flag
        requireActivity().window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferenceManager = PreferenceManager(requireContext())

        // Setup initial UI with cached data
        setupInitialUI()

        // Load fresh user profile data from API
        loadUserProfile()

        // Setup card click listeners
        setupCardClickListeners()
    }

    override fun onResume() {
        super.onResume()
        // Refresh profile data when returning to this fragment
        loadUserProfile()
    }

    private fun setupInitialUI() {
        // Display cached user information from preferences
        val email = preferenceManager.getEmail() ?: "email@example.com"
        val username = preferenceManager.getUsername() ?: preferenceManager.getName() ?: "User"
        val profileImage = preferenceManager.getProfileImage()

        // Set email
        binding.tvEmail.text = email

        // Set welcome text with username
        binding.tvWelcome.text = "Selamat datang, $username!"

        // Activate marquee
        binding.tvWelcome.isSelected = true
        binding.tvWelcome.requestFocus()  // Request focus for marquee to work

        // Load profile image with Glide using the correct ID from your layout
        profileImage?.let { imageUrl ->
            Glide.with(this)
                .load(imageUrl)
                .apply(RequestOptions
                    .circleCropTransform()
                    .placeholder(R.drawable.splash_icon)
                    .error(R.drawable.splash_icon))
                .into(binding.profileImage) // ID dari layout XML Anda
        }
    }

    private fun loadUserProfile() {
        val token = preferenceManager.getToken()
        if (token.isNullOrEmpty()) {
            return
        }

        lifecycleScope.launch {
            try {
                // Call getUserProfile endpoint to get fresh data
                val response = ApiClient.apiService.getUserProfile("Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    val profileResponse = response.body()!!
                    val userProfileData = profileResponse.user

                    // Update UI with fresh data from API
                    val currentEmail = preferenceManager.getEmail()
                    if (currentEmail != null) {
                        binding.tvEmail.text = currentEmail
                    }

                    // Update welcome message with username
                    binding.tvWelcome.text = "Selamat datang, ${userProfileData.username}!"

                    // Load profile image with Glide using the correct ID
                    userProfileData.imageLink?.let { imageUrl ->
                        Glide.with(this@HomeFragment)
                            .load(imageUrl)
                            .apply(RequestOptions
                                .circleCropTransform()
                                .placeholder(R.drawable.splash_icon)
                                .error(R.drawable.splash_icon))
                            .into(binding.profileImage) // ID dari layout XML Anda
                    }

                    // Update preferences with latest username and image
                    preferenceManager.saveUserProfileData(
                        userProfileData.id,
                        userProfileData.username,
                        userProfileData.imageLink
                    )

                } else {
                    // Handle API error silently or show subtle error
                    // Don't show toast to avoid disrupting user experience
                }
            } catch (e: Exception) {
                // Handle network error silently
                // Use cached data that's already displayed
            }
        }
    }

    private fun setupCardClickListeners() {
        binding.cardPengertian.setOnClickListener {
            findNavController().navigate(R.id.infoPengertianFragment)
        }

        binding.cardMature.setOnClickListener {
            findNavController().navigate(R.id.infoMatureFragment)
        }

        binding.cardImmature.setOnClickListener {
            findNavController().navigate(R.id.infoImmatureFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}