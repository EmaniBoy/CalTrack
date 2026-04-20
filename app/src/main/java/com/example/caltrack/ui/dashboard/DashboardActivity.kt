package com.example.caltrack.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.caltrack.R
import com.example.caltrack.data.repository.AuthRepository
import com.example.caltrack.databinding.ActivityDashboardBinding
import com.example.caltrack.ui.auth.LoginActivity
import com.example.caltrack.ui.bmi.BMICalculatorActivity
import com.example.caltrack.ui.meals.MealLogActivity
import com.example.caltrack.ui.progress.ProgressActivity
import com.example.caltrack.utils.playEntrance
import com.google.android.material.snackbar.Snackbar

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val authRepository = AuthRepository()

    override fun onStart() {
        super.onStart()
        if (!authRepository.isUserLoggedIn()) {
            goToLogin()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.playEntrance()

        val currentEmail = authRepository.currentUserEmail().orEmpty()
        if (currentEmail.isNotBlank()) {
            binding.textSignedInAs.text = getString(R.string.dashboard_signed_in_as, currentEmail)
        }

        binding.buttonOpenBmi.setOnClickListener {
            startActivity(Intent(this, BMICalculatorActivity::class.java))
        }

        binding.buttonOpenMeals.setOnClickListener {
            startActivity(Intent(this, MealLogActivity::class.java))
        }

        binding.buttonOpenProgress.setOnClickListener {
            startActivity(Intent(this, ProgressActivity::class.java))
        }

        binding.buttonLogout.setOnClickListener {
            authRepository.signOut()
            showMessage(getString(R.string.dashboard_logged_out))
            goToLogin()
        }
    }

    private fun goToLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
        )
        finish()
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
}
