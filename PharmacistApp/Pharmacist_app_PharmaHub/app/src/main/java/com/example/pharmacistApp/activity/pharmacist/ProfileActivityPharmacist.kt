package com.example.pharmacistApp.activity.pharmacist

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.pharmacistApp.R
import com.example.pharmacistApp.data.Pharmacist
import com.example.pharmacistApp.databinding.ActivityProfilePharmacistBinding
import com.example.pharmacistApp.viewmodel.ProfilePharmacistViewModel
import com.example.pharmacistApp.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileActivityPharmacist : AppCompatActivity() {

    private lateinit var binding: ActivityProfilePharmacistBinding
    private val viewModel: ProfilePharmacistViewModel by viewModels()
    private var selectedImageUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                displaySelectedImage(uri)
                getPharmacistId()?.let { pharmacistId ->
                    viewModel.uploadProfileImage(uri, pharmacistId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfilePharmacistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViews()
        setupObservers()
        loadPharmacistData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.my_profile)
        }
    }

    private fun setupViews() {
        binding.btnChangePhoto.setOnClickListener {
            openImagePicker()
        }

        // Disable all input fields except photo change
        listOf(
            binding.etFirstName, binding.etLastName, binding.etEmail,
            binding.etPhone, binding.etLicense, binding.etPharmacyName,
            binding.etAddress, binding.etCity, binding.etState, binding.etZip
        ).forEach { it.isEnabled = false }

        binding.btnSave.isVisible = false
    }

    private fun setupObservers() {
        viewModel.pharmacistData.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> showLoading(true)
                is Resource.Success -> {
                    showLoading(false)
                    resource.data?.let { populateFields(it) }
                }
                is Resource.Error -> {
                    showLoading(false)
                    showError(resource.message)
                }
                else -> {
                    // Handle any unexpected states
                    showLoading(false)
                    Log.w("ProfileActivity", "Unexpected resource state: $resource")
                }
            }
        }

        viewModel.uploadStatus.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> showLoading(true)
                is Resource.Success -> {
                    showLoading(false)
                    showSuccess(getString(R.string.profile_pic_updated))
                }
                is Resource.Error -> {
                    showLoading(false)
                    showError(resource.message)
                }
                else -> {
                    // Handle any unexpected states
                    showLoading(false)
                    Log.w("ProfileActivity", "Unexpected upload status: $resource")
                }
            }
        }
    }

    private fun openImagePicker() {
        Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
        }.also { pickImage.launch(it) }
    }

    private fun loadPharmacistData() {
        getPharmacistId()?.let(viewModel::getPharmacistProfile)
            ?: showError(getString(R.string.error_pharmacist_id))
    }

    private fun populateFields(pharmacist: Pharmacist) {
        with(binding) {
            etFirstName.setText(pharmacist.firstName)
            etLastName.setText(pharmacist.lastName)
            etEmail.setText(pharmacist.email)
            etPhone.setText(pharmacist.phoneNumber)
            etLicense.setText(pharmacist.licenseNumber)
            etPharmacyName.setText(pharmacist.pharmacyName)
            etAddress.setText(pharmacist.address)
            etCity.setText(pharmacist.city)
            etState.setText(pharmacist.state)
            etZip.setText(pharmacist.zipCode)

            pharmacist.profileImageUrl?.let(::loadProfileImage)
        }
    }

    private fun loadProfileImage(imageUrl: String) {
        Glide.with(this)
            .load(imageUrl)
            .circleCrop()
            .placeholder(R.drawable.ic_user)
            .into(binding.profileImage)
    }

    private fun displaySelectedImage(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .circleCrop()
            .placeholder(R.drawable.ic_user)
            .into(binding.profileImage)
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.btnChangePhoto.isEnabled = !isLoading
    }

    private fun showError(message: String?) {
        Toast.makeText(
            this,
            message ?: getString(R.string.error_unknown),
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun getPharmacistId(): String? = FirebaseAuth.getInstance().currentUser?.uid

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}