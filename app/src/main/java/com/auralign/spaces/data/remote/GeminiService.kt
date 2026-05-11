package com.auralign.spaces.data.remote

import android.util.Log
import com.auralign.spaces.BuildConfig
import com.auralign.spaces.data.model.AISuggestion
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class GeminiService @Inject constructor(
    private val httpClient: OkHttpClient,
    private val gson: Gson
) {
    private val geminiApiKey = BuildConfig.GEMINI_API_KEY
    private val model = "gemini-2.0-flash"
    private val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"

    suspend fun getDesignSuggestions(
        roomType: String,
        widthM: Double,
        lengthM: Double,
        heightM: Double,
        budgetInr: Double,
        roomPhotoBase64: String? = null
    ): List<AISuggestion> = withContext(Dispatchers.IO) {
        if (geminiApiKey.isBlank()) {
            throw IOException("Gemini API key missing. Add GEMINI_API_KEY to local.properties and rebuild.")
        }

        val prompt = """
            You are an expert interior designer AI.
            Room: $roomType, Dimensions: ${widthM}m x ${lengthM}m x ${heightM}m, Budget: Rs.${budgetInr.toInt()} INR.
            ${if (roomPhotoBase64 != null) "A room photo is attached. Analyze what you see and personalize the ideas to the room." else ""}

            Return ONLY a valid JSON array with exactly 4 interior design style suggestions.
            Each object must have these exact keys:
            {
              "id": "unique_id",
              "title": "Style Name",
              "description": "2 sentence description",
              "style": "style label",
              "recommendedItems": ["Item 1 with INR price", "Item 2 with INR price", "Item 3 with INR price", "Item 4 with INR price"],
              "wallColor": "#hexcode",
              "floorMaterial": "material name",
              "estimatedCostInr": 45000
            }
            Styles to cover: Scandinavian Minimalist, Warm Industrial, Japandi Zen, Luxury Modern.
            All prices must be realistic Indian market prices in INR.
            Respond with ONLY the JSON array. No markdown, no explanation, no code fences.
        """.trimIndent()

        val parts = JSONArray().put(JSONObject().put("text", prompt))
        if (roomPhotoBase64 != null) {
            parts.put(
                JSONObject().put(
                    "inlineData",
                    JSONObject()
                        .put("mimeType", "image/jpeg")
                        .put("data", roomPhotoBase64)
                )
            )
        }

        val requestBodyJson = JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("parts", parts)
                )
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.7)
                    .put("maxOutputTokens", 2048)
                    .put("responseMimeType", "application/json")
            )

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("x-goog-api-key", geminiApiKey)
            .addHeader("Content-Type", "application/json")
            .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        var lastException: Exception? = null
        for (attempt in 0..2) {
            try {
                val response = httpClient.newCall(request).execute()
                try {
                    val body = response.body?.string() ?: throw IOException("Empty Gemini response body")

                    if (response.code == 429) {
                        val waitMs = 3000L * (attempt + 1)
                        Log.w(TAG, "Gemini rate limited. Waiting ${waitMs}ms")
                        Thread.sleep(waitMs)
                        if (attempt < 2) continue
                        throw IOException("Gemini API error 429: quota limit reached")
                    }

                    if (!response.isSuccessful) {
                        throw IOException("Gemini API error ${response.code}: ${extractErrorMessage(body)}")
                    }

                    val text = extractCandidateText(body)
                    val cleanJson = extractJsonArray(text)
                    return@withContext gson.fromJson(cleanJson, Array<AISuggestion>::class.java).toList()
                } finally {
                    response.close()
                }
            } catch (e: IOException) {
                lastException = e
                if (attempt >= 2) break
            }
        }

        throw lastException ?: IOException("Unknown Gemini API error")
    }

    private fun extractCandidateText(body: String): String {
        val json = JSONObject(body)
        val candidates = json.optJSONArray("candidates")
            ?: throw IOException("Gemini response had no candidates")
        if (candidates.length() == 0) throw IOException("Gemini response had empty candidates")

        val parts = candidates
            .getJSONObject(0)
            .optJSONObject("content")
            ?.optJSONArray("parts")
            ?: throw IOException("Gemini response had no text parts")

        return buildString {
            for (i in 0 until parts.length()) {
                append(parts.getJSONObject(i).optString("text"))
            }
        }.trim().ifBlank {
            throw IOException("Gemini response text was empty")
        }
    }

    private fun extractJsonArray(text: String): String {
        val withoutFence = Regex("""```(?:json)?\s*([\s\S]*?)```""", RegexOption.IGNORE_CASE)
            .find(text)
            ?.groupValues
            ?.get(1)
            ?.trim()
            ?: text.trim()

        val start = withoutFence.indexOf('[')
        val end = withoutFence.lastIndexOf(']')
        if (start == -1 || end == -1 || end <= start) {
            throw IOException("Gemini response was not a JSON array")
        }
        return withoutFence.substring(start, end + 1)
    }

    private fun extractErrorMessage(body: String): String {
        return runCatching {
            JSONObject(body)
                .optJSONObject("error")
                ?.optString("message")
                ?.takeIf { it.isNotBlank() }
        }.getOrNull() ?: body.take(200)
    }

    private companion object {
        private const val TAG = "GeminiService"
    }
}
