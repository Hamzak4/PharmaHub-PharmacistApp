package com.example.pharmacistApp.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.pharmacistApp.activity.admin.AdminLoginActivity
import com.example.pharmacistApp.activity.pharmacist.PharmacistLoginActivty
import com.example.pharmacistApp.activity.pharmacist.PharmacistRegActivity
import com.example.pharmacistApp.databinding.ActivityAccOptionBinding


class AccOptionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccOptionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccOptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Pharmacist button - navigates to Pharmacist Registration
        binding.buttonRegisterAccountOptions.setOnClickListener {
            navigateToPharmacistRegistration()
        }

        // Admin button - navigates to Admin Login
        binding.buttonLoginAccountOptions.setOnClickListener {
            navigateToAdminLogin()
        }
    }

    private fun navigateToPharmacistRegistration() {
        startActivity(Intent(this, PharmacistLoginActivty::class.java))
        // Optional: finish() if you don't want users to come back to this screen
    }

    private fun navigateToAdminLogin() {
        startActivity(Intent(this, AdminLoginActivity::class.java))
        // Optional: finish() if you don't want users to come back to this screen
    }
}