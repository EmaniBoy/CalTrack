package com.example.caltrack

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.caltrack.data.local.MealLocalStore
import com.example.caltrack.data.local.ProfileStore
import com.example.caltrack.ui.auth.LoginActivity
import com.example.caltrack.ui.bmi.BmiActivity
import com.example.caltrack.ui.meals.MealLoggingActivity
import com.example.caltrack.ui.profile.ProfileActivity
import com.example.caltrack.ui.progress.ProgressActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private val profileStore by lazy { ProfileStore(this) }
    private val mealStore by lazy { MealLocalStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<MaterialButton>(R.id.button_meals).setOnClickListener {
            startActivity(Intent(this, MealLoggingActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.button_progress).setOnClickListener {
            startActivity(Intent(this, ProgressActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.button_bmi).setOnClickListener {
            startActivity(Intent(this, BmiActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.button_profile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.button_logout).setOnClickListener {
            startActivity(
                Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                },
            )
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshDashboardSummary()
    }

    private fun refreshDashboardSummary() {
        val summary = findViewById<TextView>(R.id.dashboard_calorie_summary)
        val profile = profileStore.load()
        val consumed = mealStore.todayCalories()
        summary.text = when {
            profile.dailyCalorieGoal <= 0 ->
                getString(R.string.dashboard_calorie_no_goal, consumed)
            else -> {
                val left = (profile.dailyCalorieGoal - consumed).coerceAtLeast(0)
                getString(R.string.dashboard_calorie_with_goal, consumed, profile.dailyCalorieGoal, left)
            }
        }
    }
}
