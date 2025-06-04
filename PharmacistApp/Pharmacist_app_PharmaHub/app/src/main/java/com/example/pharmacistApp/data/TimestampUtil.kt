package com.example.pharmacistApp.data

import android.util.Log
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimestampUtil {
    private const val TAG = "TimestampUtil"

    /**
     * Safely converts any timestamp object to a Date
     * Handles different timestamp formats from Firestore
     */
    fun toDate(timestamp: Any?): Date {
        return when (timestamp) {
            is Timestamp -> timestamp.toDate()
            is Date -> timestamp
            is Long -> Date(timestamp)
            is Map<*, *> -> {
                try {
                    // Handle server timestamp object with seconds and nanoseconds
                    val seconds = (timestamp["seconds"] as? Number)?.toLong() ?: 0L
                    val nanoseconds = (timestamp["nanoseconds"] as? Number)?.toInt() ?: 0
                    Date(seconds * 1000 + nanoseconds / 1000000)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing timestamp map: $timestamp", e)
                    Date()
                }
            }
            else -> {
                Log.w(TAG, "Unknown timestamp format: $timestamp (${timestamp?.javaClass?.name})")
                Date()
            }
        }
    }

    /**
     * Format a timestamp of any type to a readable string
     */
    fun formatTimestamp(timestamp: Any?, pattern: String = "dd MMM yyyy, HH:mm"): String {
        val date = toDate(timestamp)
        return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
    }
}