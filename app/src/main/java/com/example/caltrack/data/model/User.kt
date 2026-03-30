package com.example.caltrack.data.model

data class User (
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val age: Int = 0,
    val heightCm: Double = 0.0,
    val weightKg: Double = 0.0
)