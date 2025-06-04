package com.example.pharmacistApp.activity.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.pharmacistApp.databinding.ActivityAdminLoginBinding
import com.example.pharmacistApp.viewmodel.AdminLoginViewModel
import com.github.leandroborgesferreira.loadingbutton.customViews.CircularProgressButton
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdminLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminLoginBinding
    private val viewModel: AdminLoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupObservers()
    }

    private fun setupViews() {
        // Set up login button click listener
        binding.buttonLoginLogin.setOnClickListener {
            validateAndLogin()
        }

        // Optional: Add forgot password functionality if needed
    }

    private fun setupObservers() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is AdminLoginViewModel.LoginState.Loading -> {
                    showLoading(true)
                }
                is AdminLoginViewModel.LoginState.Success -> {
                    showLoading(false)
                    navigateToAdminDashboard()
                }
                is AdminLoginViewModel.LoginState.Error -> {
                    showLoading(false)
                    showError(state.message)
                }
            }
        }
    }

    private fun validateAndLogin() {
        val email = binding.edEmailLogin.text.toString().trim()
        val password = binding.edPasswordLogin.text.toString().trim()

        when {
            email.isEmpty() -> {
                binding.edEmailLogin.error = "Please enter admin email"
                binding.edEmailLogin.requestFocus()
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
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
                viewModel.loginAdmin(email, password)
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
        } else {
            loginButton.revertAnimation()
            loginButton.isEnabled = true
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToAdminDashboard() {
        // Log to make sure it's called
        println("DEBUG: Navigating to Admin Dashboard...")
        startActivity(Intent(this, AdminDashboardActivity::class.java))
        finishAffinity()  // Close the login activity
    }

    override fun onDestroy() {
        (binding.buttonLoginLogin as? CircularProgressButton)?.revertAnimation()
        super.onDestroy()
    }
}
