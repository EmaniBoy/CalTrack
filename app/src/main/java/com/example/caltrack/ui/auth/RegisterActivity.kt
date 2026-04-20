package com.example.caltrack.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.caltrack.R
import com.example.caltrack.data.repository.AuthRepository
import com.example.caltrack.databinding.ActivityRegisterBinding
import com.example.caltrack.utils.playEntrance
import com.google.android.material.snackbar.Snackbar

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.playEntrance()

        binding.buttonRegister.setOnClickListener { attemptRegistration() }
        binding.buttonGoLogin.setOnClickListener { finish() }
    }

    private fun attemptRegistration() {
        val email = binding.inputEmail.text?.toString()?.trim().orEmpty()
        val password = binding.inputPassword.text?.toString().orEmpty()
        val confirmPassword = binding.inputConfirmPassword.text?.toString().orEmpty()

        when {
            email.isBlank() || password.isBlank() || confirmPassword.isBlank() -> {
                showMessage(getString(R.string.auth_error_register_blank))
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

            password != confirmPassword -> {
                showMessage(getString(R.string.auth_error_password_mismatch))
                return
            }
        }

        setLoading(true)
        authRepository.register(
            email = email,
            password = password,
            onSuccess = {
                setLoading(false)
                startActivity(
                    Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra(LoginActivity.EXTRA_MESSAGE, getString(R.string.auth_register_success))
                    },
                )
                finish()
            },
            onError = { message ->
                setLoading(false)
                showMessage(message)
            },
        )
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressAuth.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonRegister.isEnabled = !isLoading
        binding.buttonGoLogin.isEnabled = !isLoading
        binding.inputEmail.isEnabled = !isLoading
        binding.inputPassword.isEnabled = !isLoading
        binding.inputConfirmPassword.isEnabled = !isLoading
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
    }
}
