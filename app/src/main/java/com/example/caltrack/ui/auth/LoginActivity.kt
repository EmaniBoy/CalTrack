package com.example.caltrack.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.caltrack.R
import com.example.caltrack.data.repository.AuthRepository
import com.example.caltrack.databinding.ActivityLoginBinding
import com.example.caltrack.ui.dashboard.DashboardActivity
import com.example.caltrack.utils.playEntrance
import com.google.android.material.snackbar.Snackbar

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authRepository = AuthRepository()

    override fun onStart() {
        super.onStart()
        if (authRepository.isUserLoggedIn()) {
            goToDashboard()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.playEntrance()

        binding.buttonLogin.setOnClickListener { attemptLogin() }
        binding.buttonGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        val successMessage = intent.getStringExtra(EXTRA_MESSAGE)
        if (!successMessage.isNullOrBlank()) {
            showMessage(successMessage)
        }
    }

    private fun attemptLogin() {
        val email = binding.inputEmail.text?.toString()?.trim().orEmpty()
        val password = binding.inputPassword.text?.toString().orEmpty()

        when {
            email.isBlank() || password.isBlank() -> {
                showMessage(getString(R.string.auth_error_blank))
                return
            }

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showMessage(getString(R.string.auth_error_invalid_email))
                return
            }

            password.length < MIN_PASSWORD_LENGTH -> {
                showMessage(getString(R.string.auth_error_password_weak))
                return
            }
        }

        setLoading(true)
        authRepository.login(
            email = email,
            password = password,
            onSuccess = {
                setLoading(false)
                goToDashboard()
            },
            onError = { message ->
                setLoading(false)
                showMessage(message)
            },
        )
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressAuth.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonLogin.isEnabled = !isLoading
        binding.buttonGoRegister.isEnabled = !isLoading
        binding.inputEmail.isEnabled = !isLoading
        binding.inputPassword.isEnabled = !isLoading
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun goToDashboard() {
        startActivity(
            Intent(this, DashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
        )
        finish()
    }

    companion object {
        const val EXTRA_MESSAGE = "extra_message"
        private const val MIN_PASSWORD_LENGTH = 6
    }
}
