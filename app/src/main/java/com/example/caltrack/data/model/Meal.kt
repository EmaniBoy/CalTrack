package com.example.caltrack.data.model

data class Meal (
    val mealId: String = "",
    val userId: String = "",
    val foodName: String = "",
    val calories: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val timestamp: Long = 0L
)