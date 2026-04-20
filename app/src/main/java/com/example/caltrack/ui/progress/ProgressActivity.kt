package com.example.caltrack.ui.progress

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.caltrack.R
import com.example.caltrack.data.repository.BMIRepository
import com.example.caltrack.data.repository.MealRepository
import com.example.caltrack.databinding.ActivityProgressBinding
import com.example.caltrack.utils.playEntrance
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class ProgressActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProgressBinding
    private lateinit var mealRepository: MealRepository
    private lateinit var bmiRepository: BMIRepository
    private lateinit var progressAdapter: DailyProgressAdapter

    private val userId: String by lazy { resolveUserId() }
    private var selectedDayStartMillis: Long = startOfDay(System.currentTimeMillis())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.playEntrance()

        mealRepository = MealRepository(this)
        bmiRepository = BMIRepository(this)
        progressAdapter = DailyProgressAdapter()

        binding.recyclerProgressHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerProgressHistory.adapter = progressAdapter

        binding.buttonBackDashboard.setOnClickListener { finish() }
        binding.buttonSaveCalorieGoal.setOnClickListener { saveCalorieGoal() }

        binding.calendarProgress.date = selectedDayStartMillis
        binding.calendarProgress.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDayStartMillis = startOfDay(year, month, dayOfMonth)
            refreshSelectedDaySummary()
        }

        val calorieGoal = loadCalorieGoal()
        if (calorieGoal > 0) {
            binding.inputCalorieGoal.setText(calorieGoal.toString())
        }

        refreshProgressViews()
    }

    override fun onResume() {
        super.onResume()
        refreshProgressViews()
    }

    private fun refreshProgressViews() {
        val history = mealRepository.getDailyNutritionHistory(userId, HISTORY_DAYS)
        val bmiByDay = bmiRepository.getDailyLatestRecords(userId, HISTORY_DAYS)
        val calorieGoal = loadCalorieGoal()

        progressAdapter.setCalorieGoal(calorieGoal)
        progressAdapter.setBmiByDayStart(bmiByDay)
        progressAdapter.submitList(history)
        binding.recyclerProgressHistory.scheduleLayoutAnimation()

        val hasNutritionData = history.any { it.calories > 0 || it.protein > 0 || it.carbs > 0 || it.fat > 0 }
        binding.textHistoryEmpty.visibility = if (hasNutritionData || bmiByDay.isNotEmpty()) {
            View.GONE
        } else {
            View.VISIBLE
        }

        refreshLatestBmiSnapshot()
        refreshSelectedDaySummary()
    }

    private fun refreshLatestBmiSnapshot() {
        val latest = bmiRepository.getLatestRecord(userId)
        if (latest == null) {
            binding.textLatestBmiSnapshot.text = getString(R.string.progress_latest_bmi_empty)
            return
        }

        val latestLine = getString(
            R.string.progress_latest_bmi_line,
            latest.bmiValue,
            latest.category,
            fullDateFormatter.format(Date(latest.timestamp)),
        )

        val previous = bmiRepository.getPreviousRecord(userId)
        if (previous == null) {
            binding.textLatestBmiSnapshot.text = getString(
                R.string.progress_latest_bmi_first_record,
                latestLine,
            )
            return
        }

        val delta = latest.bmiValue - previous.bmiValue
        val trend = when {
            abs(delta) < 0.01 -> getString(R.string.progress_bmi_trend_stable)
            delta > 0 -> getString(R.string.progress_bmi_trend_up, abs(delta))
            else -> getString(R.string.progress_bmi_trend_down, abs(delta))
        }

        binding.textLatestBmiSnapshot.text = getString(
            R.string.progress_latest_bmi_with_trend,
            latestLine,
            trend,
        )
    }

    private fun refreshSelectedDaySummary() {
        val nutrition = mealRepository.getNutritionForDay(userId, selectedDayStartMillis)
        val calorieGoal = loadCalorieGoal()
        val dayLabel = fullDateFormatter.format(Date(selectedDayStartMillis))

        binding.textSelectedDaySummary.text = if (calorieGoal > 0) {
            val goalMessage = when {
                nutrition.calories == calorieGoal -> getString(R.string.progress_selected_goal_on_target)
                nutrition.calories < calorieGoal -> getString(
                    R.string.progress_selected_goal_remaining,
                    calorieGoal - nutrition.calories,
                )
                else -> getString(
                    R.string.progress_selected_goal_over,
                    nutrition.calories - calorieGoal,
                )
            }

            getString(
                R.string.progress_selected_day_summary_with_goal,
                dayLabel,
                nutrition.calories,
                nutrition.protein,
                nutrition.carbs,
                nutrition.fat,
                calorieGoal,
                goalMessage,
            )
        } else {
            getString(
                R.string.progress_selected_day_summary_no_goal,
                dayLabel,
                nutrition.calories,
                nutrition.protein,
                nutrition.carbs,
                nutrition.fat,
            )
        }

        val selectedDayBmi = bmiRepository.getLatestRecordForDay(userId, selectedDayStartMillis)
        binding.textSelectedDayBmi.text = if (selectedDayBmi != null) {
            getString(
                R.string.progress_selected_day_bmi_value,
                selectedDayBmi.bmiValue,
                selectedDayBmi.category,
            )
        } else {
            getString(R.string.progress_selected_day_bmi_empty)
        }
    }

    private fun saveCalorieGoal() {
        val rawInput = binding.inputCalorieGoal.text?.toString()?.trim().orEmpty()
        if (rawInput.isBlank()) {
            saveCalorieGoalValue(0)
            showMessage(getString(R.string.progress_goal_cleared))
            refreshProgressViews()
            return
        }

        val parsed = rawInput.toIntOrNull()
        if (parsed == null || parsed <= 0) {
            showMessage(getString(R.string.progress_goal_invalid))
            return
        }

        saveCalorieGoalValue(parsed)
        showMessage(getString(R.string.progress_goal_saved, parsed))
        refreshProgressViews()
    }

    private fun saveCalorieGoalValue(value: Int) {
        getSharedPreferences(PROGRESS_PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putInt(getCalorieGoalKey(), value)
            .apply()
    }

    private fun loadCalorieGoal(): Int {
        return getSharedPreferences(PROGRESS_PREFS_NAME, MODE_PRIVATE)
            .getInt(getCalorieGoalKey(), 0)
    }

    private fun getCalorieGoalKey(): String = "calorie_goal_$userId"

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
        private const val HISTORY_DAYS = 30
        private const val PROGRESS_PREFS_NAME = "progress_prefs"
        private val fullDateFormatter = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    }
}
