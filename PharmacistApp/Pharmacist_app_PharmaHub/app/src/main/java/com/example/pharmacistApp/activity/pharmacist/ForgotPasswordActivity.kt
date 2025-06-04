package com.example.pharmacistApp.activity.pharmacist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pharmacistApp.databinding.ActivityForgotPasswordBinding
import com.example.pharmacistApp.viewmodel.ForgotPasswordViewModel
import com.github.leandroborgesferreira.loadingbutton.customViews.CircularProgressButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ForgotPassword", "Activity created")

        // Ensure window settings prevent early destruction
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set transparent status bar if needed
        window.statusBarColor = getColor(android.R.color.transparent)

        setupViews()
        setupObservers()
    }

    override fun onStart() {
        super.onStart()
        Log.d("ForgotPassword", "Activity started")
    }

    override fun onResume() {
        super.onResume()
        Log.d("ForgotPassword", "Activity resumed")
    }

    private fun setupViews() {
        binding.apply {
            btnResetPassword.setOnClickListener {
                validateAndSendResetLink()
            }

            tvBackToLogin.setOnClickListener {
                navigateToLogin()
            }

            edEmail.setOnEditorActionListener { _, _, _ ->
                validateAndSendResetLink()
                true
            }
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.resetState.collect { state ->
                when (state) {
                    ForgotPasswordViewModel.ResetState.Idle -> Unit
                    ForgotPasswordViewModel.ResetState.Loading -> showLoading(true)
                    ForgotPasswordViewModel.ResetState.Success -> {
                        showLoading(false)
                        showSuccessMessage()
                        binding.root.postDelayed({
                            navigateToLogin()
                        }, 2000)
                    }
                    is ForgotPasswordViewModel.ResetState.Error -> {
                        showLoading(false)
                        showError(state.message)
                    }
                }

            }
        }
    }

    private fun validateAndSendResetLink() {
        val email = binding.edEmail.text.toString().trim()

        when {
            email.isEmpty() -> {
                binding.edEmail.error = "Please enter email"
                binding.edEmail.requestFocus()
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.edEmail.error = "Please enter valid email"
                binding.edEmail.requestFocus()
            }
            else -> {
                binding.edEmail.error = null
                viewModel.sendPasswordResetEmail(email)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        val resetButton = binding.btnResetPassword as CircularProgressButton
        if (show) {
            resetButton.startAnimation()
            resetButton.isEnabled = false
            binding.tvBackToLogin.isEnabled = false
        } else {
            resetButton.revertAnimation()
            resetButton.isEnabled = true
            binding.tvBackToLogin.isEnabled = true
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showSuccessMessage() {
        Toast.makeText(
            this,
            "Password reset link sent to your email",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, PharmacistLoginActivty::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onDestroy() {
        Log.d("ForgotPassword", "Activity destroyed")
        (binding.btnResetPassword as? CircularProgressButton)?.revertAnimation()
        super.onDestroy()
    }
}