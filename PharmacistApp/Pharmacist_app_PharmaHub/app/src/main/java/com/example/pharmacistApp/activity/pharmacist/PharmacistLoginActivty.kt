package com.example.pharmacistApp.activity.pharmacist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.pharmacistApp.databinding.ActivityPharmacistLoginBinding
import com.example.pharmacistApp.viewmodel.PharmacistLoginViewModel
import com.github.leandroborgesferreira.loadingbutton.customViews.CircularProgressButton
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PharmacistLoginActivty : AppCompatActivity() {

    private lateinit var binding: ActivityPharmacistLoginBinding
    private val viewModel: PharmacistLoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPharmacistLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)



        setupViews()
        setupObservers()
    }

    private fun setupViews() {
        binding.apply {
            buttonLoginLogin.setOnClickListener {
                validateAndLogin()
            }

            tvDontHaveAccount.setOnClickListener {
                navigateToRegister()
            }

            tvForgotPasswordLogin.setOnClickListener {
                navigateToForgotPassword()
            }

            edPasswordLogin.setOnEditorActionListener { _, _, _ ->
                validateAndLogin()
                true
            }
        }
    }

    private fun setupObservers() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                PharmacistLoginViewModel.LoginState.Loading -> showLoading(true)
                PharmacistLoginViewModel.LoginState.Success -> {
                    showLoading(false)
                    navigateToHome()
                }
                PharmacistLoginViewModel.LoginState.AlreadyAuthenticated -> {
                    navigateToHome()
                }
                is PharmacistLoginViewModel.LoginState.Error -> {
                    showLoading(false)
                    showError(state.message)
                }
                PharmacistLoginViewModel.LoginState.NotApproved -> {
                    showLoading(false)
                    showApprovalPendingMessage()
                }
                PharmacistLoginViewModel.LoginState.Suspended -> {
                    showLoading(false)
                    showSuspendedMessage()
                }
            }
        }
    }

    private fun validateAndLogin() {
        val email = binding.edEmailLogin.text.toString().trim()
        val password = binding.edPasswordLogin.text.toString().trim()

        when {
            email.isEmpty() -> {
                binding.edEmailLogin.error = "Please enter email"
                binding.edEmailLogin.requestFocus()
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.edEmailLogin.error = "Please enter valid email"
                binding.edEmailLogin.requestFocus()
            }
            password.isEmpty() -> {
                binding.edPasswordLogin.error = "Please enter password"
                binding.edPasswordLogin.requestFocus()
            }
            password.length < 6 -> {
                binding.edPasswordLogin.error = "Password must be at least 6 characters"
                binding.edPasswordLogin.requestFocus()
            }
            else -> {
                clearErrors()
                viewModel.loginUser(email, password)
            }
        }
    }

    private fun clearErrors() {
        binding.edEmailLogin.error = null
        binding.edPasswordLogin.error = null
    }

    private fun showLoading(show: Boolean) {
        val loginButton = binding.buttonLoginLogin as CircularProgressButton
        if (show) {
            loginButton.startAnimation()
            loginButton.isEnabled = false
            binding.tvDontHaveAccount.isEnabled = false
            binding.tvForgotPasswordLogin.isEnabled = false
        } else {
            loginButton.revertAnimation()
            loginButton.isEnabled = true
            binding.tvDontHaveAccount.isEnabled = true
            binding.tvForgotPasswordLogin.isEnabled = true
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showApprovalPendingMessage() {
        Toast.makeText(
            this,
            "Your account is pending admin approval. Please try again later.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showSuspendedMessage() {
        Toast.makeText(
            this,
            "Your account has been suspended. Please contact support.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun navigateToHome() {
        startActivity(
            Intent(this, PharmacistDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    private fun navigateToRegister() {
        startActivity(Intent(this, PharmacistRegActivity::class.java))
    }

    private fun navigateToForgotPassword() {
        try {
            val intent = Intent(this, ForgotPasswordActivity::class.java).apply {
                // Clear any problematic flags
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        } catch (e: Exception) {
            Log.e("NAVIGATION", "Failed to open forgot password", e)
            Toast.makeText(this, "Error opening password reset", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        (binding.buttonLoginLogin as? CircularProgressButton)?.revertAnimation()
        super.onDestroy()
    }
}