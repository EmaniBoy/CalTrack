package com.example.caltrack.data.model

data class WeightLog (
    val logId: String = "",
    val userId: String = "",
    val weightKg: Double = 0.0,
    val timestamp: Long = 0L
)