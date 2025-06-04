package com.example.pharmacistApp.activity.pharmacist

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.pharmacistApp.databinding.ActivityPharmacistRegBinding
import com.example.pharmacistApp.viewmodel.PharmacistRegistrationViewModel
import com.github.leandroborgesferreira.loadingbutton.customViews.CircularProgressButton
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PharmacistRegActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPharmacistRegBinding
    private val viewModel: PharmacistRegistrationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPharmacistRegBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupObservers()
        setupInputListeners()
    }

    private fun setupViews() {
        // Register button click
        binding.buttonRegisterRegister.setOnClickListener {
            validateAndRegister()
        }

        // Login text click
        binding.tvDoYouHaveAccount.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun setupObservers() {
        viewModel.registrationState.observe(this) { state ->
            when (state) {
                is PharmacistRegistrationViewModel.RegistrationState.Loading -> {
                    showLoading(true)
                }
                is PharmacistRegistrationViewModel.RegistrationState.Success -> {
                    showLoading(false)
                    showSuccessMessage()
                    navigateToLogin()
                }
                is PharmacistRegistrationViewModel.RegistrationState.Error -> {
                    showLoading(false)
                    showErrorMessage(state.message)
                }
            }
        }
    }

    private fun setupInputListeners() {
        // Clear errors when user starts typing
        binding.edFirstNameRegister.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.edFirstNameRegister.error = null
        }
        binding.edLastNameRegister.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.edLastNameRegister.error = null
        }
        binding.edEmailRegister.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.edEmailRegister.error = null
        }
        binding.edPasswordRegister.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.edPasswordRegister.error = null
        }
        // Add listeners for other fields as needed
    }

    private fun validateAndRegister() {
        val firstName = binding.edFirstNameRegister.text.toString().trim()
        val lastName = binding.edLastNameRegister.text.toString().trim()
        val email = binding.edEmailRegister.text.toString().trim()
        val password = binding.edPasswordRegister.text.toString().trim()
        val confirmPassword = binding.edPasswordRegister.text.toString().trim()
        val pharmacyName = binding.edPharmacyName.text.toString().trim()
        val licenseNumber = binding.edLicenseNumber.text.toString().trim()
        val phoneNumber = binding.edPhoneNumber.text.toString().trim()
        val address = binding.edAddress.text.toString().trim()
        val city = binding.edCity.text.toString().trim()
        val state = binding.edState.text.toString().trim()
        val zipCode = binding.edZipCode.text.toString().trim()

        when {
            firstName.isEmpty() -> {
                binding.edFirstNameRegister.error = "Please enter first name"
                binding.edFirstNameRegister.requestFocus()
            }
            lastName.isEmpty() -> {
                binding.edLastNameRegister.error = "Please enter last name"
                binding.edLastNameRegister.requestFocus()
            }
            email.isEmpty() -> {
                binding.edEmailRegister.error = "Please enter email"
                binding.edEmailRegister.requestFocus()
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.edEmailRegister.error = "Please enter valid email"
                binding.edEmailRegister.requestFocus()
            }
            password.isEmpty() -> {
                binding.edPasswordRegister.error = "Please enter password"
                binding.edPasswordRegister.requestFocus()
            }
            password.length < 8 -> {
                binding.edPasswordRegister.error = "Password must be at least 8 characters"
                binding.edPasswordRegister.requestFocus()
            }
            confirmPassword != password -> {
                binding.edPasswordRegister.error = "Passwords don't match"
                binding.edPasswordRegister.requestFocus()
            }
            pharmacyName.isEmpty() -> {
                binding.edPharmacyName.error = "Please enter pharmacy name"
                binding.edPharmacyName.requestFocus()
            }
            licenseNumber.isEmpty() -> {
                binding.edLicenseNumber.error = "Please enter license number"
                binding.edLicenseNumber.requestFocus()
            }
            phoneNumber.isEmpty() -> {
                binding.edPhoneNumber.error = "Please enter phone number"
                binding.edPhoneNumber.requestFocus()
            }
            address.isEmpty() -> {
                binding.edAddress.error = "Please enter address"
                binding.edAddress.requestFocus()
            }
            city.isEmpty() -> {
                binding.edCity.error = "Please enter city"
                binding.edCity.requestFocus()
            }
            state.isEmpty() -> {
                binding.edState.error = "Please enter state"
                binding.edState.requestFocus()
            }
            zipCode.isEmpty() -> {
                binding.edZipCode.error = "Please enter zip code"
                binding.edZipCode.requestFocus()
            }
            else -> {
                clearErrors()
                viewModel.registerPharmacist(
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    password = password,
                    pharmacyName = pharmacyName,
                    licenseNumber = licenseNumber,
                    phoneNumber = phoneNumber,
                    address = address,
                    city = city,
                    state = state,
                    zipCode = zipCode
                )
            }
        }
    }

    private fun clearErrors() {
        binding.edFirstNameRegister.error = null
        binding.edLastNameRegister.error = null
        binding.edEmailRegister.error = null
        binding.edPasswordRegister.error = null
        binding.edPasswordRegister.error = null
        binding.edPharmacyName.error = null
        binding.edLicenseNumber.error = null
        binding.edPhoneNumber.error = null
        binding.edAddress.error = null
        binding.edCity.error = null
        binding.edState.error = null
        binding.edZipCode.error = null
    }

    private fun showLoading(show: Boolean) {
        val registerButton = binding.buttonRegisterRegister as CircularProgressButton
        if (show) {
            registerButton.startAnimation()
            registerButton.isEnabled = false
        } else {
            registerButton.revertAnimation()
            registerButton.isEnabled = true
        }
    }

    private fun showSuccessMessage() {
        Toast.makeText(
            this,
            "Registration successful! Your account is pending admin approval.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, PharmacistLoginActivty::class.java))
        finish()
    }

    override fun onDestroy() {
        // Clean up loading button animation
        (binding.buttonRegisterRegister as? CircularProgressButton)?.revertAnimation()
        super.onDestroy()
    }
}