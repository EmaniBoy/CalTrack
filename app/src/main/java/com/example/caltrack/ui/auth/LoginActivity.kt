package com.example.caltrack.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.caltrack.MainActivity
import com.example.caltrack.R
import com.example.caltrack.data.local.LocalAccountStore
import com.example.caltrack.data.local.SessionStore
import com.example.caltrack.databinding.ActivityLoginBinding
import com.google.android.material.snackbar.Snackbar

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val accounts by lazy { LocalAccountStore(this) }
    private val session by lazy { SessionStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (session.isLoggedIn()) {
            goToMain()
            return
        }

        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        binding.buttonLogin.setOnClickListener { attemptLogin() }
        binding.buttonGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun attemptLogin() {
        val email = binding.inputEmail.text?.toString()?.trim().orEmpty()
        val password = binding.inputPassword.text?.toString().orEmpty()

        if (email.isEmpty() || password.isEmpty()) {
            Snackbar.make(binding.root, R.string.auth_error_blank, Snackbar.LENGTH_LONG).show()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Snackbar.make(binding.root, R.string.auth_error_invalid_email, Snackbar.LENGTH_LONG).show()
            return
        }
        if (password.length < 6) {
            Snackbar.make(binding.root, R.string.auth_error_password_weak, Snackbar.LENGTH_LONG).show()
            return
        }

        if (accounts.verifyLogin(email, password)) {
            session.setLoggedIn(email)
            goToMain()
        } else {
            Snackbar.make(binding.root, R.string.auth_error_login_failed, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
