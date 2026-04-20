package com.example.caltrack.data.repository

import android.content.Context
import com.example.caltrack.data.model.BMIRecord
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

class BMIRepository(context: Context) {

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getRecords(userId: String): List<BMIRecord> {
        return readRecords(userId).sortedByDescending { it.timestamp }
    }

    fun getLatestRecords(userId: String, limit: Int): List<BMIRecord> {
        return getRecords(userId).take(limit.coerceAtLeast(1))
    }

    fun getLatestRecord(userId: String): BMIRecord? {
        return getRecords(userId).firstOrNull()
    }

    fun getPreviousRecord(userId: String): BMIRecord? {
        return getRecords(userId).drop(1).firstOrNull()
    }

    fun addRecord(
        userId: String,
        bmiValue: Double,
        category: String,
        timestamp: Long = System.currentTimeMillis(),
    ): BMIRecord {
        val record = BMIRecord(
            recordId = UUID.randomUUID().toString(),
            userId = userId,
            bmiValue = bmiValue,
            category = category,
            timestamp = timestamp,
        )

        val updated = readRecords(userId).toMutableList().apply { add(record) }
        writeRecords(userId, updated)
        return record
    }

    fun getLatestRecordForDay(userId: String, dayTimestamp: Long): BMIRecord? {
        return readRecords(userId)
            .filter { isSameDay(it.timestamp, dayTimestamp) }
            .maxByOrNull { it.timestamp }
    }

    fun getDailyLatestRecords(userId: String, days: Int): Map<Long, BMIRecord> {
        val records = readRecords(userId)
        val todayStart = startOfDay(System.currentTimeMillis())
        val result = mutableMapOf<Long, BMIRecord>()

        for (offset in 0 until days.coerceAtLeast(1)) {
            val dayStart = Calendar.getInstance().run {
                timeInMillis = todayStart
                add(Calendar.DAY_OF_YEAR, -offset)
                timeInMillis
            }
            val latest = records
                .filter { isSameDay(it.timestamp, dayStart) }
                .maxByOrNull { it.timestamp }
            if (latest != null) {
                result[dayStart] = latest
            }
        }

        return result
    }

    private fun readRecords(userId: String): List<BMIRecord> {
        val json = preferences.getString(getUserKey(userId), null).orEmpty()
        if (json.isBlank()) {
            return emptyList()
        }

        return try {
            val items = JSONArray(json)
            buildList {
                for (index in 0 until items.length()) {
                    val recordJson = items.optJSONObject(index) ?: continue
                    add(
                        BMIRecord(
                            recordId = recordJson.optString("recordId"),
                            userId = recordJson.optString("userId"),
                            bmiValue = recordJson.optDouble("bmiValue"),
                            category = recordJson.optString("category"),
                            timestamp = recordJson.optLong("timestamp"),
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writeRecords(userId: String, records: List<BMIRecord>) {
        val serialized = JSONArray()
        records.forEach { record ->
            serialized.put(
                JSONObject()
                    .put("recordId", record.recordId)
                    .put("userId", record.userId)
                    .put("bmiValue", record.bmiValue)
                    .put("category", record.category)
                    .put("timestamp", record.timestamp),
            )
        }

        preferences.edit().putString(getUserKey(userId), serialized.toString()).apply()
    }

    private fun getUserKey(userId: String): String = "bmi_records_$userId"

    private fun isSameDay(firstTimestamp: Long, secondTimestamp: Long): Boolean {
        return startOfDay(firstTimestamp) == startOfDay(secondTimestamp)
    }

    private fun startOfDay(timestamp: Long): Long {
        return Calendar.getInstance().run {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            timeInMillis
        }
    }

    companion object {
        private const val PREFS_NAME = "bmi_record_prefs"
    }
}
