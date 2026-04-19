package com.example.caltrack.ui.meals

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.caltrack.data.model.Meal
import com.example.caltrack.databinding.ActivityMealLogBinding
import com.example.caltrack.databinding.DialogAddMealBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

class MealLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMealLogBinding
    private val database = Firebase.database.reference
    private val auth = Firebase.auth
    private lateinit var adapter: MealAdapter
    private var mealsList = mutableListOf<Meal>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMealLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        listenForMeals()

        binding.fabAddMeal.setOnClickListener {
            showAddMealDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = MealAdapter(mealsList)
        binding.rvMeals.layoutManager = LinearLayoutManager(this)
        binding.rvMeals.adapter = adapter
    }

    private fun listenForMeals() {
        val userId = auth.currentUser?.uid ?: return
        database.child("meals").child(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    mealsList.clear()
                    var totalCals = 0
                    var totalP = 0.0
                    var totalC = 0.0
                    var totalF = 0.0

                    snapshot.children.forEach {
                        val meal = it.getValue(Meal::class.java)
                        if (meal != null) {
                            mealsList.add(meal)
                            totalCals += meal.calories
                            totalP += meal.protein
                            totalC += meal.carbs
                            totalF += meal.fat
                        }
                    }
                    
                    // Sort by newest first
                    mealsList.sortByDescending { it.timestamp }
                    adapter.updateMeals(mealsList)
                    updateSummary(totalCals, totalP, totalC, totalF)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MealLogActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateSummary(cals: Int, p: Double, c: Double, f: Double) {
        binding.apply {
            tvTotalCalories.text = cals.toString()
            tvTotalProtein.text = "${p.toInt()}g"
            tvTotalCarbs.text = "${c.toInt()}g"
            tvTotalFat.text = "${f.toInt()}g"
        }
    }

    private fun showAddMealDialog() {
        val dialogBinding = DialogAddMealBinding.inflate(LayoutInflater.from(this))
        
        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Log") { _, _ ->
                val name = dialogBinding.etFoodName.text.toString()
                val cals = dialogBinding.etCalories.text.toString().toIntOrNull() ?: 0
                val p = dialogBinding.etProtein.text.toString().toDoubleOrNull() ?: 0.0
                val c = dialogBinding.etCarbs.text.toString().toDoubleOrNull() ?: 0.0
                val f = dialogBinding.etFat.text.toString().toDoubleOrNull() ?: 0.0

                if (name.isNotEmpty()) {
                    saveMeal(name, cals, p, c, f)
                } else {
                    Toast.makeText(this, "Food name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveMeal(name: String, cals: Int, p: Double, c: Double, f: Double) {
        val userId = auth.currentUser?.uid ?: return
        val mealId = database.child("meals").child(userId).push().key ?: return
        
        val meal = Meal(
            mealId = mealId,
            userId = userId,
            foodName = name,
            calories = cals,
            protein = p,
            carbs = c,
            fat = f,
            timestamp = System.currentTimeMillis()
        )

        database.child("meals").child(userId).child(mealId).setValue(meal)
            .addOnSuccessListener {
                Toast.makeText(this, "Meal logged!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to log meal", Toast.LENGTH_SHORT).show()
            }
    }
}