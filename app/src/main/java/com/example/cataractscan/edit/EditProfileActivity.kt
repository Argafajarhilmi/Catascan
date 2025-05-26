package com.example.cataractscan.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.cataractscan.R
import com.example.cataractscan.databinding.ActivityEditProfileBinding
import com.example.cataractscan.utils.PreferenceManager
import com.example.cataractscan.api.ApiClient
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class EditProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var preferenceManager: PreferenceManager
    private var selectedImageUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                binding.ivProfile.setImageURI(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable full screen mode
        enableFullScreenMode()

        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)
        loadUserData()
        setupClickListeners()
    }

    private fun enableFullScreenMode() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
    }

    private fun loadUserData() {
        // Load user data from preferences
        val name = preferenceManager.getName() ?: ""
        val email = preferenceManager.getEmail() ?: ""
        val profileImage = preferenceManager.getProfileImage()

        binding.apply {
            etName.setText(name)
            etEmail.setText(email)
            etAddress.setText("") // Address tidak ada di API response, bisa di-handle sesuai kebutuhan

            // Load profile image dengan Glide
            Glide.with(this@EditProfileActivity)
                .load(profileImage)
                .apply(RequestOptions
                    .circleCropTransform()
                    .placeholder(R.drawable.splash_icon)
                    .error(R.drawable.splash_icon))
                .into(ivProfile)
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        binding.btnChangePhoto.setOnClickListener {
            openImagePicker()
        }

        binding.btnSave.setOnClickListener {
            saveUserData()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage.launch(intent)
    }

    private fun saveUserData() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()

        if (validateInput(name, email)) {
            // Show loading
            setLoading(true)

            // Update profile via API
            updateProfileViaAPI()
        }
    }

    private fun updateProfileViaAPI() {
        val token = preferenceManager.getToken()
        if (token.isNullOrEmpty()) {
            showToast("Session expired, please login again")
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                var imagePart: MultipartBody.Part? = null

                // Prepare image if selected
                selectedImageUri?.let { uri ->
                    val imageFile = createFileFromUri(uri)
                    val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                    imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
                }

                // Call API
                val response = ApiClient.apiService.updateProfile("Bearer $token", imagePart)

                setLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!

                    // Update local preferences with new data
                    preferenceManager.saveUserProfile(responseBody.user)

                    // Save additional local data (address) to local preferences
                    val sharedPrefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                    sharedPrefs.edit().apply {
                        putString("address", binding.etAddress.text.toString().trim())
                        apply()
                    }

                    showToast("Profile updated successfully")
                    finish()
                } else {
                    showToast("Failed to update profile")
                }
            } catch (e: Exception) {
                setLoading(false)
                showToast("Error: ${e.message}")
            }
        }
    }

    private fun createFileFromUri(uri: Uri): File {
        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
        val file = File(cacheDir, "profile_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.flush()
        outputStream.close()
        return file
    }

    private fun validateInput(name: String, email: String): Boolean {
        if (name.isEmpty()) {
            showToast("Please enter your name")
            return false
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Please enter a valid email")
            return false
        }
        return true
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnSave.isEnabled = !isLoading
        binding.btnSave.text = if (isLoading) "Saving..." else "Save"
        binding.btnChangePhoto.isEnabled = !isLoading
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}