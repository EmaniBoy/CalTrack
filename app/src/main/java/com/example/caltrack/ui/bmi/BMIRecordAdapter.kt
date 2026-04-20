package com.example.caltrack.ui.bmi

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.caltrack.R
import com.example.caltrack.data.model.BMIRecord
import com.example.caltrack.databinding.ItemBmiRecordBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BMIRecordAdapter : RecyclerView.Adapter<BMIRecordAdapter.BMIRecordViewHolder>() {

    private val items = mutableListOf<BMIRecord>()

    fun submitList(records: List<BMIRecord>) {
        items.clear()
        items.addAll(records)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BMIRecordViewHolder {
        val binding = ItemBmiRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return BMIRecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BMIRecordViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class BMIRecordViewHolder(
        private val binding: ItemBmiRecordBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(record: BMIRecord) {
            val context = binding.root.context
            binding.textBmiValue.text = context.getString(
                R.string.bmi_record_line,
                record.bmiValue,
                record.category,
            )
            binding.textBmiDate.text = context.getString(
                R.string.bmi_record_date,
                timestampFormatter.format(Date(record.timestamp)),
            )
        }
    }

    companion object {
        private val timestampFormatter = SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault())
    }
}
