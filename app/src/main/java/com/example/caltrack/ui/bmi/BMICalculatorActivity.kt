package com.example.caltrack.ui.bmi

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.caltrack.R
import com.example.caltrack.data.repository.BMIRepository
import com.example.caltrack.databinding.ActivityBmiCalculatorBinding
import com.example.caltrack.utils.playEntrance
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class BMICalculatorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBmiCalculatorBinding
    private lateinit var bmiRepository: BMIRepository
    private lateinit var bmiRecordAdapter: BMIRecordAdapter

    private val userId: String by lazy { resolveUserId() }
    private var pendingBmiValue: Double? = null
    private var pendingCategory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBmiCalculatorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.playEntrance()

        bmiRepository = BMIRepository(this)
        bmiRecordAdapter = BMIRecordAdapter()

        binding.recyclerBmiHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerBmiHistory.adapter = bmiRecordAdapter

        binding.buttonBackDashboard.setOnClickListener { finish() }
        binding.buttonCalculateBmi.setOnClickListener { calculateBmi() }
        binding.buttonSaveBmiRecord.setOnClickListener { saveBmiRecord() }
        binding.buttonClearBmiInputs.setOnClickListener { clearInputs() }

        refreshHistory()
        showResult(null, null)
    }

    private fun calculateBmi() {
        val heightCm = binding.inputHeightCm.text?.toString()?.trim().orEmpty().toDoubleOrNull()
        val weightKg = binding.inputWeightKg.text?.toString()?.trim().orEmpty().toDoubleOrNull()

        if (heightCm == null || weightKg == null) {
            showMessage(getString(R.string.bmi_error_invalid_number))
            return
        }
        if (heightCm <= 0.0 || weightKg <= 0.0) {
            showMessage(getString(R.string.bmi_error_non_positive))
            return
        }

        val heightMeters = heightCm / 100.0
        val bmi = weightKg / (heightMeters * heightMeters)
        val category = resolveCategory(bmi)

        pendingBmiValue = bmi
        pendingCategory = category
        showResult(bmi, category)
    }

    private fun saveBmiRecord() {
        val bmi = pendingBmiValue
        val category = pendingCategory
        if (bmi == null || category == null) {
            showMessage(getString(R.string.bmi_error_calculate_first))
            return
        }

        bmiRepository.addRecord(userId = userId, bmiValue = bmi, category = category)
        showMessage(getString(R.string.bmi_saved_message))
        refreshHistory()
    }

    private fun clearInputs() {
        binding.inputHeightCm.text?.clear()
        binding.inputWeightKg.text?.clear()
        pendingBmiValue = null
        pendingCategory = null
        showResult(null, null)
    }

    private fun showResult(bmi: Double?, category: String?) {
        if (bmi == null || category == null) {
            binding.cardBmiResult.visibility = View.GONE
            binding.buttonSaveBmiRecord.isEnabled = false
            return
        }

        binding.cardBmiResult.visibility = View.VISIBLE
        binding.buttonSaveBmiRecord.isEnabled = true
        binding.textBmiValue.text = getString(R.string.bmi_result_value_precise, bmi)
        binding.textBmiCategory.text = getString(R.string.bmi_result_category, category)
        binding.textBmiInsight.text = getString(R.string.bmi_goal_insight, resolveInsight(category))
    }

    private fun refreshHistory() {
        val records = bmiRepository.getLatestRecords(userId, MAX_HISTORY)
        bmiRecordAdapter.submitList(records)
        binding.recyclerBmiHistory.scheduleLayoutAnimation()
        binding.textBmiHistoryEmpty.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun resolveCategory(bmi: Double): String {
        return when {
            bmi < 18.5 -> getString(R.string.bmi_category_underweight)
            bmi < 25.0 -> getString(R.string.bmi_category_normal)
            bmi < 30.0 -> getString(R.string.bmi_category_overweight)
            else -> getString(R.string.bmi_category_obesity)
        }
    }

    private fun resolveInsight(category: String): String {
        return when (category) {
            getString(R.string.bmi_category_underweight) -> getString(R.string.bmi_insight_underweight)
            getString(R.string.bmi_category_normal) -> getString(R.string.bmi_insight_normal)
            getString(R.string.bmi_category_overweight) -> getString(R.string.bmi_insight_overweight)
            else -> getString(R.string.bmi_insight_obesity)
        }
    }

    private fun resolveUserId(): String {
        return try {
            FirebaseAuth.getInstance().currentUser?.uid ?: "guest_user"
        } catch (_: Exception) {
            "guest_user"
        }
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    companion object {
        private const val MAX_HISTORY = 10
    }
}
