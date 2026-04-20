package com.example.caltrack.data.repository

import android.os.Handler
import android.os.Looper
import com.example.caltrack.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class GeminiMealParser {

    private val client = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun parseMealText(
        mealText: String,
        onSuccess: (List<ParsedMeal>) -> Unit,
        onError: (String) -> Unit,
    ) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            onError("Gemini API key missing. Add GEMINI_API_KEY to local.properties.")
            return
        }

        val prompt = buildPrompt(mealText)
        val requestJson = buildRequestBody(prompt)

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                postError(onError, "Unable to reach Gemini API. Check internet connection and try again.")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val details = extractErrorMessage(body)
                        postError(onError, "Gemini API request failed (${response.code}): $details")
                        return
                    }

                    if (body.isBlank()) {
                        postError(onError, "Gemini returned an empty response.")
                        return
                    }

                    val parsed = parseResponse(body)
                    if (parsed.isEmpty()) {
                        postError(onError, "Could not parse meals. Try rephrasing your meal description.")
                        return
                    }

                    mainHandler.post { onSuccess(parsed) }
                }
            }
        })
    }

    private fun parseResponse(responseBody: String): List<ParsedMeal> {
        return try {
            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates") ?: return emptyList()
            if (candidates.length() == 0) {
                return emptyList()
            }

            val firstCandidate = candidates.optJSONObject(0) ?: return emptyList()
            val parts = firstCandidate
                .optJSONObject("content")
                ?.optJSONArray("parts") ?: return emptyList()

            if (parts.length() == 0) {
                return emptyList()
            }

            val jsonText = buildString {
                for (index in 0 until parts.length()) {
                    val textPart = parts.optJSONObject(index)?.optString("text").orEmpty()
                    append(textPart)
                }
            }
            if (jsonText.isBlank()) {
                return emptyList()
            }

            val parsedRoot = JSONObject(normalizeJsonText(jsonText))
            val items = parsedRoot.optJSONArray("items") ?: return emptyList()
            parseItems(items)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseItems(items: JSONArray): List<ParsedMeal> {
        val parsed = mutableListOf<ParsedMeal>()

        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val name = item.optString("foodName").trim()
            if (name.isBlank()) {
                continue
            }

            parsed.add(
                ParsedMeal(
                    foodName = name,
                    calories = item.optInt("calories").coerceAtLeast(0),
                    protein = item.optDouble("protein").coerceAtLeast(0.0),
                    carbs = item.optDouble("carbs").coerceAtLeast(0.0),
                    fat = item.optDouble("fat").coerceAtLeast(0.0),
                ),
            )
        }

        return parsed
    }

    private fun buildPrompt(mealText: String): String {
        return """
            You are a nutrition parser for a calorie tracking app.
            Parse the user's meal text into individual food entries with estimated macros.
            Rules:
            - Output only valid JSON.
            - Return an object with one key: items.
            - items must be an array of objects.
            - Each object must include: foodName (string), calories (integer), protein (number), carbs (number), fat (number).
            - Use realistic nutrition estimates if exact values are unknown.
            - Split combined input into clean entries by food item and quantity.
            - Do not include explanations, markdown, or any text outside JSON.
            User meal text: "$mealText"
        """.trimIndent()
    }

    private fun buildRequestBody(prompt: String): JSONObject {
        return JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", prompt)),
                    ),
                ),
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.2)
                    .put("responseMimeType", "application/json")
                    .put("responseSchema", buildSchema()),
            )
    }

    private fun buildSchema(): JSONObject {
        val mealItemSchema = JSONObject()
            .put("type", "object")
            .put(
                "properties",
                JSONObject()
                    .put("foodName", JSONObject().put("type", "string"))
                    .put("calories", JSONObject().put("type", "integer"))
                    .put("protein", JSONObject().put("type", "number"))
                    .put("carbs", JSONObject().put("type", "number"))
                    .put("fat", JSONObject().put("type", "number")),
            )
            .put(
                "required",
                JSONArray()
                    .put("foodName")
                    .put("calories")
                    .put("protein")
                    .put("carbs")
                    .put("fat"),
            )

        return JSONObject()
            .put("type", "object")
            .put(
                "properties",
                JSONObject().put(
                    "items",
                    JSONObject()
                        .put("type", "array")
                        .put("items", mealItemSchema),
                ),
            )
            .put("required", JSONArray().put("items"))
    }

    private fun postError(callback: (String) -> Unit, message: String) {
        mainHandler.post { callback(message) }
    }

    private fun extractErrorMessage(responseBody: String): String {
        if (responseBody.isBlank()) {
            return "No additional details."
        }

        return try {
            val root = JSONObject(responseBody)
            root.optJSONObject("error")
                ?.optString("message")
                ?.takeIf { it.isNotBlank() }
                ?: "No additional details."
        } catch (_: Exception) {
            "No additional details."
        }
    }

    private fun normalizeJsonText(text: String): String {
        val trimmed = text.trim()
        if (!trimmed.startsWith("```")) {
            return trimmed
        }

        return trimmed
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    data class ParsedMeal(
        val foodName: String,
        val calories: Int,
        val protein: Double,
        val carbs: Double,
        val fat: Double,
    )

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
