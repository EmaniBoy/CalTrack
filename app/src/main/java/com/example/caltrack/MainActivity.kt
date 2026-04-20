package com.example.caltrack

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.caltrack.data.repository.AuthRepository
import com.example.caltrack.databinding.ActivityMainBinding
import com.example.caltrack.ui.auth.LoginActivity
import com.example.caltrack.ui.auth.RegisterActivity
import com.example.caltrack.ui.dashboard.DashboardActivity
import com.example.caltrack.utils.playEntrance
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.playEntrance()

        binding.buttonOpenLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        binding.buttonOpenRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.buttonOpenDashboard.setOnClickListener {
            if (authRepository.isUserLoggedIn()) {
                startActivity(Intent(this, DashboardActivity::class.java))
            } else {
                Snackbar.make(binding.root, R.string.main_requires_login, Snackbar.LENGTH_LONG).show()
            }
        }

        binding.buttonSignOut.setOnClickListener {
            authRepository.signOut()
            updateAuthStatus()
            Snackbar.make(binding.root, R.string.main_signed_out, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        updateAuthStatus()
    }

    private fun updateAuthStatus() {
        val email = authRepository.currentUserEmail()
        if (email.isNullOrBlank()) {
            binding.textAuthStatus.text = getString(R.string.main_not_logged_in)
            binding.buttonOpenDashboard.isEnabled = false
            binding.buttonSignOut.isEnabled = false
        } else {
            binding.textAuthStatus.text = getString(R.string.main_logged_in_as, email)
            binding.buttonOpenDashboard.isEnabled = true
            binding.buttonSignOut.isEnabled = true
        }
    }
}
