package com.example.pharmacistApp.util

// PrefsManager.kt
import android.content.Context
import android.content.SharedPreferences
import com.example.pharmacistApp.data.Pharmacist
import com.google.gson.Gson

object PrefsManager {
    private const val PREFS_NAME = "PharmacistPrefs"
    private const val KEY_PHARMACIST = "pharmacist_data"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun savePharmacistData(context: Context, pharmacist: Pharmacist) {
        val prefs = getSharedPreferences(context)
        val editor = prefs.edit()
        val gson = Gson()
        val json = gson.toJson(pharmacist)
        editor.putString(KEY_PHARMACIST, json)
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.apply()
    }

    fun getPharmacistData(context: Context): Pharmacist? {
        val prefs = getSharedPreferences(context)
        val gson = Gson()
        val json = prefs.getString(KEY_PHARMACIST, null)
        return gson.fromJson(json, Pharmacist::class.java)
    }

    fun clearData(context: Context) {
        val prefs = getSharedPreferences(context)
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }
}