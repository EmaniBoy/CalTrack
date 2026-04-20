package com.example.caltrack.data.model

data class BMIRecord (
    val recordId: String = "",
    val userId: String = "",
    val bmiValue: Double = 0.0,
    val category: String = "",
    val timestamp: Long = 0L
)