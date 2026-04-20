package com.example.caltrack.ui.meals

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.caltrack.R
import com.example.caltrack.data.model.Meal
import com.example.caltrack.data.repository.GeminiMealParser
import com.example.caltrack.data.repository.MealRepository
import com.example.caltrack.databinding.ActivityMealLogBinding
import com.example.caltrack.utils.playEntrance
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class MealLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMealLogBinding
    private lateinit var mealRepository: MealRepository
    private lateinit var mealAdapter: MealAdapter

    private val mealParser = GeminiMealParser()
    private val userId: String by lazy { resolveUserId() }
    private var selectedDayStartMillis: Long = startOfDay(System.currentTimeMillis())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMealLogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.playEntrance()

        mealRepository = MealRepository(this)
        mealAdapter = MealAdapter { meal -> deleteMeal(meal) }

        binding.recyclerMeals.layoutManager = LinearLayoutManager(this)
        binding.recyclerMeals.adapter = mealAdapter

        binding.buttonBackDashboard.setOnClickListener { finish() }
        binding.buttonChooseDate.setOnClickListener { openDatePicker() }
        binding.buttonResetDay.setOnClickListener { resetSelectedDay() }
        binding.buttonParseAndLog.setOnClickListener { parseAndLogMeal() }

        refreshDayView()
    }

    private fun parseAndLogMeal() {
        val mealText = binding.inputMealPrompt.text?.toString()?.trim().orEmpty()
        if (mealText.isBlank()) {
            showMessage(getString(R.string.meal_ai_input_required))
            return
        }

        setLoading(true)
        mealParser.parseMealText(
            mealText = mealText,
            onSuccess = { parsedMeals ->
                if (parsedMeals.isEmpty()) {
                    setLoading(false)
                    showMessage(getString(R.string.meal_ai_parse_failed))
                    return@parseMealText
                }

                val mealsToSave = parsedMeals.mapIndexed { index, parsedMeal ->
                    Meal(
                        mealId = UUID.randomUUID().toString(),
                        userId = userId,
                        foodName = parsedMeal.foodName,
                        calories = parsedMeal.calories,
                        protein = parsedMeal.protein,
                        carbs = parsedMeal.carbs,
                        fat = parsedMeal.fat,
                        timestamp = buildTimestampForSelectedDay(index),
                    )
                }

                mealRepository.addMeals(userId, mealsToSave)
                binding.inputMealPrompt.text?.clear()
                refreshDayView()
                setLoading(false)
                showMessage(getString(R.string.meal_ai_logged_count, mealsToSave.size))
            },
            onError = { message ->
                setLoading(false)
                showMessage(message)
            },
        )
    }

    private fun deleteMeal(meal: Meal) {
        mealRepository.deleteMeal(userId, meal.mealId)
        refreshDayView()
        showMessage(getString(R.string.meal_deleted))
    }

    private fun resetSelectedDay() {
        val mealsForDay = mealRepository.getMealsForDay(userId, selectedDayStartMillis)
        if (mealsForDay.isEmpty()) {
            showMessage(getString(R.string.meal_reset_empty_day))
            return
        }

        mealRepository.deleteMealsForDay(userId, selectedDayStartMillis)
        refreshDayView()
        showMessage(
            getString(
                R.string.meal_reset_success,
                dateLabelFormatter.format(Date(selectedDayStartMillis)),
            ),
        )
    }

    private fun refreshDayView() {
        val mealsForDay = mealRepository.getMealsForDay(userId, selectedDayStartMillis)
        mealAdapter.submitList(mealsForDay)
        binding.recyclerMeals.scheduleLayoutAnimation()
        binding.textEmptyState.visibility = if (mealsForDay.isEmpty()) View.VISIBLE else View.GONE

        binding.textSelectedDate.text = getString(
            R.string.meal_selected_date,
            dateLabelFormatter.format(Date(selectedDayStartMillis)),
        )

        val summary = mealRepository.getNutritionForDay(userId, selectedDayStartMillis)
        binding.textDailyTotals.text = getString(
            R.string.meal_selected_day_totals,
            summary.calories,
            summary.protein,
            summary.carbs,
            summary.fat,
        )
    }

    private fun openDatePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedDayStartMillis }
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDayStartMillis = startOfDay(year, month, dayOfMonth)
                refreshDayView()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    private fun buildTimestampForSelectedDay(index: Int): Long {
        val now = System.currentTimeMillis()
        if (isSameDay(now, selectedDayStartMillis)) {
            return now + index
        }

        return Calendar.getInstance().run {
            timeInMillis = selectedDayStartMillis
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            timeInMillis + index
        }
    }

    private fun isSameDay(firstTimestamp: Long, secondTimestamp: Long): Boolean {
        return startOfDay(firstTimestamp) == startOfDay(secondTimestamp)
    }

    private fun startOfDay(timestamp: Long): Long {
        return Calendar.getInstance().run {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            timeInMillis
        }
    }

    private fun startOfDay(year: Int, month: Int, dayOfMonth: Int): Long {
        return Calendar.getInstance().run {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            timeInMillis
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressParsing.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonParseAndLog.isEnabled = !isLoading
        binding.inputMealPrompt.isEnabled = !isLoading
        binding.buttonChooseDate.isEnabled = !isLoading
        binding.buttonResetDay.isEnabled = !isLoading
    }

    private fun resolveUserId(): String {
        return try {
            FirebaseAuth.getInstance().currentUser?.uid ?: "guest_user"
        } catch (_: Exception) {
            "guest_user"
        }
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    companion object {
        private val dateLabelFormatter = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    }
}
