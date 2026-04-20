package com.example.caltrack.ui.meals

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.caltrack.R
import com.example.caltrack.data.model.Meal
import com.example.caltrack.databinding.ItemMealEntryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MealAdapter(
    private val onDeleteClick: (Meal) -> Unit,
) : RecyclerView.Adapter<MealAdapter.MealViewHolder>() {

    private val items = mutableListOf<Meal>()

    fun submitList(newItems: List<Meal>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val binding = ItemMealEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MealViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class MealViewHolder(
        private val binding: ItemMealEntryBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(meal: Meal) {
            binding.textFoodName.text = meal.foodName
            binding.textMacros.text = binding.root.context.getString(
                R.string.meal_macro_line,
                meal.calories,
                meal.protein,
                meal.carbs,
                meal.fat,
            )
            binding.textLoggedTime.text = binding.root.context.getString(
                R.string.meal_logged_time,
                timestampFormatter.format(Date(meal.timestamp)),
            )
            binding.buttonDelete.setOnClickListener { onDeleteClick(meal) }
        }
    }

    companion object {
        private val timestampFormatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    }
}
