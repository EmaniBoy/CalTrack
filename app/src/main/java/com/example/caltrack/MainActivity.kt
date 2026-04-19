package com.example.caltrack

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.caltrack.ui.auth.LoginActivity
import com.example.caltrack.ui.meals.MealLogActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        auth = Firebase.auth
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val user = auth.currentUser
        if (user == null) {
            navigateToLogin()
            return
        } else {
            findViewById<TextView>(R.id.tvUserEmail).text = user.email
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            auth.signOut()
            navigateToLogin()
        }

        // Dashboard Navigation to Meal Log
        findViewById<View>(R.id.btnMealLog).setOnClickListener {
            startActivity(Intent(this, MealLogActivity::class.java))
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}