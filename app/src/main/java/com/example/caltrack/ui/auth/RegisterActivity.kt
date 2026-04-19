package com.example.caltrack.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.caltrack.databinding.ActivityRegisterBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        binding.btnRegisterSubmit.setOnClickListener {
            val email = binding.etRegisterEmail.text.toString().trim()
            val password = binding.etRegisterPassword.text.toString().trim()

            if (validateInput(email, password)) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                            navigateToLogin()
                        } else {
                            Toast.makeText(this, "Registration failed: ${task.exception?.message}", 
                                Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        binding.btnLoginRedirect.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.etRegisterEmail.error = "Email is required"
            return false
        }
        if (password.isEmpty()) {
            binding.etRegisterPassword.error = "Password is required"
            return false
        }
        if (password.length < 6) {
            binding.etRegisterPassword.error = "Password must be at least 6 characters"
            return false
        }
        return true
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}