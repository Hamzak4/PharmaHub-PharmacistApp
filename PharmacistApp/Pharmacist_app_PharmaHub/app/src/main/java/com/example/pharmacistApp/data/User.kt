package com.example.pharmacistApp.data

data class User(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val imagePath: String = "",
    val isActive: Boolean = true,
    val phone: String = "",
) {
    val name: String
        get() = "$firstName $lastName"
}