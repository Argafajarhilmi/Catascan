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
import android.util.Log // Import Log

class EditProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var preferenceManager: PreferenceManager
    private var selectedImageUri: Uri? = null

    private val TAG = "EditProfileActivity" // Tag untuk Logcat

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                binding.ivProfile.setImageURI(uri)
                Log.d(TAG, "Image selected: $uri")
            } ?: Log.w(TAG, "Image selection data is null.")
        } else {
            Log.d(TAG, "Image selection cancelled or failed. Result code: ${result.resultCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        val name = preferenceManager.getName() ?: ""
        val email = preferenceManager.getEmail() ?: ""
        val profileImage = preferenceManager.getProfileImage()

        Log.d(TAG, "Loading user data - Name: $name, Email: $email, Image: $profileImage")

        binding.apply {
            etName.setText(name)
            etEmail.setText(email)
            etAddress.setText("") // Address tidak ada di API response, bisa di-handle sesuai kebutuhan

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
        val address = binding.etAddress.text.toString().trim() // Anda mungkin ingin menyimpan ini juga

        if (validateInput(name, email)) {
            setLoading(true)
            updateProfileViaAPI(name, email, address) // Kirim juga data teks jika ingin diupdate
        }
    }

    private fun updateProfileViaAPI(name: String, email: String, address: String) { // Terima parameter teks
        val token = preferenceManager.getToken()
        if (token.isNullOrEmpty()) {
            showToast("Session expired, please login again")
            setLoading(false) // Pastikan loading dimatikan
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                var imagePart: MultipartBody.Part? = null

                selectedImageUri?.let { uri ->
                    Log.d(TAG, "Processing selected image URI: $uri")
                    val imageFile = createFileFromUri(uri)
                    Log.d(TAG, "Created image file: ${imageFile.absolutePath}, size: ${imageFile.length()} bytes")

                    if (imageFile.length() == 0L) {
                        Log.e(TAG, "Image file is empty after creation!")
                        showToast("Gagal mengunggah: File gambar kosong.")
                        setLoading(false)
                        return@launch // Hentikan coroutine jika file kosong
                    }

                    val mediaType = contentResolver.getType(uri)?.toMediaTypeOrNull() // Dapatkan MIME type dari ContentResolver
                        ?: "image/*".toMediaTypeOrNull() // Fallback jika tidak ditemukan

                    if (mediaType == null) {
                        Log.e(TAG, "Could not determine MediaType for image URI: $uri")
                        showToast("Gagal mengunggah: Tipe gambar tidak didukung.")
                        setLoading(false)
                        return@launch
                    }

                    val requestFile = imageFile.asRequestBody(mediaType)
                    imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile) // "image" adalah nama field yang diharapkan backend
                    Log.d(TAG, "Multipart image part created. Filename: ${imageFile.name}, MediaType: $mediaType")
                } ?: run {
                    Log.d(TAG, "No new image selected. Sending update without image part.")
                }

                // Jika Anda juga ingin mengirim data teks (nama, email, alamat), Anda perlu menambahkannya sebagai part terpisah
                // atau mengubah endpoint backend agar menerima JSON + Multipart (jarang)
                // Jika backend Anda mengharapkan semua data dalam satu form-data multipart:
                // val namePart = name.toRequestBody("text/plain".toMediaTypeOrNull())
                // val emailPart = email.toRequestBody("text/plain".toMediaTypeOrNull())
                // val addressPart = address.toRequestBody("text/plain".toMediaTypeOrNull())

                // Panggil API
                // Asumsi: updateProfile menerima MultipartBody.Part saja untuk gambar.
                // Jika Anda juga perlu mengirim data teks, API Anda mungkin perlu diubah
                // atau Anda perlu mengirimnya sebagai part terpisah
                val response = ApiClient.apiService.updateProfile("Bearer $token", imagePart) // Hanya mengirim imagePart

                setLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!
                    Log.d(TAG, "Profile update successful. Message: ${responseBody.message}, User: ${responseBody.user}")

                    // Update local preferences with new data
                    // Asumsi responseBody.user adalah model User atau UserProfile yang bisa disimpan
                    responseBody.user?.let { updatedUser -> // Periksa jika user tidak null
                        preferenceManager.saveUserProfile(updatedUser) // Simpan User model
                        // Atau jika Anda menggunakan UserProfile: preferenceManager.saveUserProfile(updatedUser.toUserProfile())
                    }

                    // Simpan data alamat secara lokal jika tidak dikirim ke API
                    val sharedPrefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                    sharedPrefs.edit().apply {
                        putString("address", address) // Simpan alamat yang diinput
                        apply()
                    }

                    showToast("Profile updated successfully")
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Failed to update profile. Code: ${response.code()}, Error: $errorBody")
                    showToast("Failed to update profile: ${response.code()}")
                }
            } catch (e: Exception) {
                setLoading(false)
                Log.e(TAG, "Exception during profile update: ${e.message}", e)
                showToast("Error: ${e.message}")
            }
        }
    }

    private fun createFileFromUri(uri: Uri): File {
        // Menggunakan ContentResolver untuk membaca stream dan menulis ke file cache
        val fileName = "profile_${System.currentTimeMillis()}.jpg"
        val file = File(cacheDir, fileName)
        contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
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