package com.example.pharmacistApp

sealed class Category(val category: String) {
    object OverTheCounter: Category("Over The Counter")
    object Prescription: Category("Prescription")
    object Vitamins: Category("Vitamins")
    object FirstAid: Category("First Aid")
    object Wellness: Category("Wellness")

    companion object {
        fun fromString(value: String): Category {
            return when (value) {
                "Over The Counter" -> OverTheCounter
                "Prescription" -> Prescription
                "Vitamins" -> Vitamins
                "First Aid" -> FirstAid
                "Wellness" -> Wellness
                else -> OverTheCounter // default
            }
        }
    }
}