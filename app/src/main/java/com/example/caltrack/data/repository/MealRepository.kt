package com.example.caltrack.data.repository

import android.content.Context
import com.example.caltrack.data.model.Meal
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class MealRepository(context: Context) {

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getMeals(userId: String): List<Meal> {
        return readMeals(userId).sortedByDescending { it.timestamp }
    }

    fun addMeals(userId: String, meals: List<Meal>): List<Meal> {
        val updated = readMeals(userId).toMutableList()
        updated.addAll(meals)
        writeMeals(userId, updated)
        return updated.sortedByDescending { it.timestamp }
    }

    fun deleteMeal(userId: String, mealId: String): List<Meal> {
        val updated = readMeals(userId).filterNot { it.mealId == mealId }
        writeMeals(userId, updated)
        return updated.sortedByDescending { it.timestamp }
    }

    fun getMealsForDay(userId: String, dayTimestamp: Long): List<Meal> {
        return readMeals(userId)
            .filter { isSameDay(it.timestamp, dayTimestamp) }
            .sortedByDescending { it.timestamp }
    }

    fun deleteMealsForDay(userId: String, dayTimestamp: Long): List<Meal> {
        val updated = readMeals(userId).filterNot { isSameDay(it.timestamp, dayTimestamp) }
        writeMeals(userId, updated)
        return updated.sortedByDescending { it.timestamp }
    }

    fun getNutritionForDay(userId: String, dayTimestamp: Long): DailyNutritionSummary {
        val meals = getMealsForDay(userId, dayTimestamp)
        return DailyNutritionSummary(
            dayStartMillis = startOfDay(dayTimestamp),
            calories = meals.sumOf { it.calories },
            protein = meals.sumOf { it.protein },
            carbs = meals.sumOf { it.carbs },
            fat = meals.sumOf { it.fat },
        )
    }

    fun getDailyNutritionHistory(userId: String, days: Int): List<DailyNutritionSummary> {
        val allMeals = readMeals(userId)
        val todayStart = startOfDay(System.currentTimeMillis())
        val summaries = mutableListOf<DailyNutritionSummary>()

        for (offset in 0 until days.coerceAtLeast(1)) {
            val dayStart = Calendar.getInstance().run {
                timeInMillis = todayStart
                add(Calendar.DAY_OF_YEAR, -offset)
                timeInMillis
            }
            val mealsForDay = allMeals.filter { isSameDay(it.timestamp, dayStart) }

            summaries.add(
                DailyNutritionSummary(
                    dayStartMillis = dayStart,
                    calories = mealsForDay.sumOf { it.calories },
                    protein = mealsForDay.sumOf { it.protein },
                    carbs = mealsForDay.sumOf { it.carbs },
                    fat = mealsForDay.sumOf { it.fat },
                ),
            )
        }

        return summaries
    }

    private fun isSameDay(firstTimestamp: Long, secondTimestamp: Long): Boolean {
        return startOfDay(firstTimestamp) == startOfDay(secondTimestamp)
    }

    private fun startOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun readMeals(userId: String): List<Meal> {
        val json = preferences.getString(getUserKey(userId), null).orEmpty()
        if (json.isBlank()) {
            return emptyList()
        }

        return try {
            val items = JSONArray(json)
            buildList {
                for (index in 0 until items.length()) {
                    val mealJson = items.optJSONObject(index) ?: continue
                    add(
                        Meal(
                            mealId = mealJson.optString("mealId"),
                            userId = mealJson.optString("userId"),
                            foodName = mealJson.optString("foodName"),
                            calories = mealJson.optInt("calories"),
                            protein = mealJson.optDouble("protein"),
                            carbs = mealJson.optDouble("carbs"),
                            fat = mealJson.optDouble("fat"),
                            timestamp = mealJson.optLong("timestamp"),
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writeMeals(userId: String, meals: List<Meal>) {
        val serialized = JSONArray()
        meals.forEach { meal ->
            serialized.put(
                JSONObject()
                    .put("mealId", meal.mealId)
                    .put("userId", meal.userId)
                    .put("foodName", meal.foodName)
                    .put("calories", meal.calories)
                    .put("protein", meal.protein)
                    .put("carbs", meal.carbs)
                    .put("fat", meal.fat)
                    .put("timestamp", meal.timestamp),
            )
        }

        preferences.edit().putString(getUserKey(userId), serialized.toString()).apply()
    }

    private fun getUserKey(userId: String): String = "meals_$userId"

    companion object {
        private const val PREFS_NAME = "meal_log_prefs"
    }

    data class DailyNutritionSummary(
        val dayStartMillis: Long,
        val calories: Int,
        val protein: Double,
        val carbs: Double,
        val fat: Double,
    )
}
