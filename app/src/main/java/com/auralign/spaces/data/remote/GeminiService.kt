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
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

@Singleton
class GeminiService @Inject constructor(
    private val httpClient: OkHttpClient,
    private val gson: Gson
) {
    private val groqApiKey = BuildConfig.GROQ_API_KEY
    private val endpoint = "https://api.groq.com/openai/v1/chat/completions"

    // llama-4-scout supports vision (image input) and is free on Groq
    private val visionModel = "meta-llama/llama-4-scout-17b-16e-instruct"
    // llama-3.3-70b for text-only requests (faster, more tokens)
    private val textModel = "llama-3.3-70b-versatile"

    suspend fun getDesignSuggestions(
        roomType: String,
        widthM: Double,
        lengthM: Double,
        heightM: Double,
        budgetInr: Double,
        roomPhotoBase64: String? = null
    ): List<AISuggestion> = withContext(Dispatchers.IO) {

        val prompt = """
            You are an expert interior designer AI.
            Room: $roomType, Dimensions: ${widthM}m x ${lengthM}m x ${heightM}m, Budget: Rs.${budgetInr.toInt()} INR.
            ${if (roomPhotoBase64 != null) "A room photo is attached. Analyze it carefully and give personalized suggestions based on what you see. " else ""}
            Return ONLY a valid JSON array with exactly 4 interior design style suggestions.
            Each object must have these exact keys:
            {
              "id": "unique_id",
              "title": "Style Name",
              "description": "2 sentence description",
              "style": "emoji + style label",
              "recommendedItems": ["Item 1 with INR price", "Item 2 with INR price", "Item 3 with INR price", "Item 4 with INR price"],
              "wallColor": "#hexcode",
              "floorMaterial": "material name",
              "estimatedCostInr": 45000
            }
            Styles to cover: Scandinavian Minimalist, Warm Industrial, Japandi Zen, Luxury Modern.
            All prices must be realistic Indian market prices in INR.
            Respond with ONLY the JSON array. No markdown, no explanation, no code fences.
        """.trimIndent()

        // Build message content
        val contentArray = JSONArray()

        // If image provided, add it first (vision model supports image_url with base64)
        if (roomPhotoBase64 != null) {
            contentArray.put(JSONObject().apply {
                put("type", "image_url")
                put("image_url", JSONObject().apply {
                    put("url", "data:image/jpeg;base64,$roomPhotoBase64")
                })
            })
        }

        // Add the text prompt
        contentArray.put(JSONObject().apply {
            put("type", "text")
            put("text", prompt)
        })

        val messages = JSONArray().put(JSONObject().apply {
            put("role", "user")
            put("content", if (roomPhotoBase64 != null) contentArray else prompt)
        })

        val requestBodyJson = JSONObject().apply {
            put("model", if (roomPhotoBase64 != null) visionModel else textModel)
            put("messages", messages)
            put("max_tokens", 2048)
            put("temperature", 0.7)
        }

        Log.d("GroqService", "Sending request. Has image: ${roomPhotoBase64 != null}, Model: ${if (roomPhotoBase64 != null) visionModel else textModel}")

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $groqApiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        var lastException: Exception? = null
        for (attempt in 0..2) {
            try {
                val response = httpClient.newCall(request).execute()
                val body = response.body?.string() ?: throw IOException("Empty response body")

                if (response.code == 429) {
                    val waitMs = 3000L * (attempt + 1)
                    Log.w("GroqService", "Rate limited. Waiting ${waitMs}ms (attempt ${attempt + 1})")
                    Thread.sleep(waitMs)
                    if (attempt < 2) continue
                    throw IOException("Rate limit exceeded. Please wait a moment and try again.")
                }

                if (!response.isSuccessful) {
                    Log.e("GroqService", "API error ${response.code}: $body")
                    throw IOException("Groq API error ${response.code}: $body")
                }

                Log.d("GroqService", "Success response received")

                val text = JSONObject(body)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()

                // Strip any accidental markdown code fences
                val cleanText = Regex("""```(?:json)?\s*([\s\S]*?)```""", RegexOption.IGNORE_CASE)
                    .find(text)?.groupValues?.get(1)?.trim() ?: text

                Log.d("GroqService", "Response length: ${cleanText.length}")

                return@withContext gson.fromJson(cleanText, Array<AISuggestion>::class.java).toList()

            } catch (e: IOException) {
                lastException = e
                if (attempt >= 2) break
            }
        }

        throw lastException ?: IOException("Unknown error")
    }
}
