package com.example.pharmacistApp.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pharmacistApp.util.Constants.INTRODUCTION_KEY
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IntroductionViewModel @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    companion object {
        const val NAVIGATE_TO_ACCOUNT_OPTIONS = 1

    }

    private val _navigationEvent = MutableStateFlow(0)
    val navigationEvent: StateFlow<Int> = _navigationEvent

    init {
        checkUserState()
    }

    fun onStartButtonClicked() {
        sharedPreferences.edit().putBoolean(INTRODUCTION_KEY, true).apply()
        navigateToAccountOptions()
    }

    private fun checkUserState() {
        val user = firebaseAuth.currentUser
        if (user != null) {

        } else if (sharedPreferences.getBoolean(INTRODUCTION_KEY, false)) {
            navigateToAccountOptions()
        }
    }

    private fun navigateToAccountOptions() {
        viewModelScope.launch {
            _navigationEvent.emit(NAVIGATE_TO_ACCOUNT_OPTIONS)
        }
    }

}