package com.example.caltrack.ui.meals

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.caltrack.data.model.Meal
import com.example.caltrack.databinding.ItemMealBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MealAdapter(private var meals: List<Meal>) : RecyclerView.Adapter<MealAdapter.MealViewHolder>() {

    class MealViewHolder(val binding: ItemMealBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val binding = ItemMealBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MealViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        val meal = meals[position]
        holder.binding.apply {
            tvFoodName.text = meal.foodName
            tvMealCalories.text = "${meal.calories} kcal"
            tvMealMacros.text = "P: ${meal.protein}g | C: ${meal.carbs}g | F: ${meal.fat}g"
            
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            tvMealTime.text = sdf.format(Date(meal.timestamp))
        }
    }

    override fun getItemCount() = meals.size

    fun updateMeals(newMeals: List<Meal>) {
        meals = newMeals
        notifyDataSetChanged()
    }
}