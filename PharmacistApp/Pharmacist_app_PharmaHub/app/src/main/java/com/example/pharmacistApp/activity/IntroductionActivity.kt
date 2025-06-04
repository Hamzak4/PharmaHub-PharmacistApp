package com.example.pharmacistApp.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pharmacistApp.R
import com.example.pharmacistApp.databinding.ActivityIntroductionBinding
import com.example.pharmacistApp.viewmodel.IntroductionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class IntroductionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIntroductionBinding
    private val viewModel: IntroductionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroductionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        lifecycleScope.launchWhenStarted {
            viewModel.navigationEvent.collect { destination ->
                handleNavigation(destination)
            }
        }
    }

    private fun setupClickListeners() {
        binding.buttonStart.setOnClickListener {
            viewModel.onStartButtonClicked()
        }
    }

    private fun handleNavigation(destination: Int) {
        when (destination) {
            IntroductionViewModel.NAVIGATE_TO_ACCOUNT_OPTIONS -> {
                navigateToAccountOptions()
            }
        }
    }

    private fun navigateToAccountOptions() {
        Intent(this, AccOptionActivity::class.java).apply {
            startActivity(this)
            finish()
        }
    }

}