package com.example.caltrack.ui.progress

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.caltrack.R
import com.example.caltrack.data.model.BMIRecord
import com.example.caltrack.data.repository.MealRepository
import com.example.caltrack.databinding.ItemDailyProgressBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyProgressAdapter : RecyclerView.Adapter<DailyProgressAdapter.ProgressViewHolder>() {

    private val items = mutableListOf<MealRepository.DailyNutritionSummary>()
    private var bmiByDayStart = emptyMap<Long, BMIRecord>()
    private var calorieGoal = 0

    fun submitList(newItems: List<MealRepository.DailyNutritionSummary>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun setCalorieGoal(goal: Int) {
        calorieGoal = goal.coerceAtLeast(0)
        notifyDataSetChanged()
    }

    fun setBmiByDayStart(map: Map<Long, BMIRecord>) {
        bmiByDayStart = map
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgressViewHolder {
        val binding = ItemDailyProgressBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ProgressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProgressViewHolder, position: Int) {
        holder.bind(items[position], calorieGoal, bmiByDayStart[items[position].dayStartMillis])
    }

    override fun getItemCount(): Int = items.size

    inner class ProgressViewHolder(
        private val binding: ItemDailyProgressBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MealRepository.DailyNutritionSummary, goal: Int, bmiRecord: BMIRecord?) {
            val context = binding.root.context
            binding.textDayLabel.text = dayFormatter.format(Date(item.dayStartMillis))
            binding.textCalories.text = context.getString(R.string.progress_day_calories, item.calories)
            binding.textMacros.text = context.getString(
                R.string.progress_day_macros,
                item.protein,
                item.carbs,
                item.fat,
            )
            binding.textBmi.text = if (bmiRecord != null) {
                context.getString(
                    R.string.progress_day_bmi_value,
                    bmiRecord.bmiValue,
                    bmiRecord.category,
                )
            } else {
                context.getString(R.string.progress_day_bmi_empty)
            }

            binding.textGoalStatus.text = if (goal > 0) {
                val difference = item.calories - goal
                when {
                    difference == 0 -> context.getString(R.string.progress_goal_on_target)
                    difference > 0 -> context.getString(R.string.progress_goal_over, difference)
                    else -> context.getString(R.string.progress_goal_under, -difference)
                }
            } else {
                context.getString(R.string.progress_goal_not_set)
            }
        }
    }

    companion object {
        private val dayFormatter = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    }
}
