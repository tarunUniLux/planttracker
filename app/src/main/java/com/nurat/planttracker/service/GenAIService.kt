package com.nurat.planttracker.service

import com.google.protobuf.Struct
import com.google.protobuf.Value
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

interface GenAIService {
    /**
     * Analyzes a commit message and provides suggestions for code improvement.
     *
     * @param commitMessage The message from the Git commit.
     * @return A string containing the AI's suggestions, or an empty string if no suggestions/error.
     */
    suspend fun analyzeCommitMessage(commitMessage: String): String
}

// Implementation using Google Gemini API (conceptual - requires API key and proper setup)
class GeminiGenAIService(private val apiKey: String) : GenAIService {
    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    override suspend fun analyzeCommitMessage(commitMessage: String): String {
        val prompt = "Analyze the following commit message and suggest how the related code might be improved: \"$commitMessage\""

        val payload = """
            {
                "contents": [
                    {
                        "role": "user",
                        "parts": [
                            { "text": "$prompt" }
                        ]
                    }
                ],
                "generationConfig": {
                    "temperature": 0.7,
                    "topK": 40,
                    "topP": 0.95
                }
            }
        """.trimIndent()

        val requestBody = payload.toRequestBody(JSON)
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=$apiKey")
            .post(requestBody)
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()?.let { responseBody ->
                    val json = Json.parseToJsonElement(responseBody).jsonObject
                    // Adjust parsing based on actual Gemini API response structure
                    // This is a simplified example
                    json["candidates"]?.jsonArray?.get(0)?.jsonObject
                        ?.get("content")?.jsonObject
                        ?.get("parts")?.jsonArray?.get(0)?.jsonObject
                        ?.get("text")?.jsonPrimitive?.content ?: ""
                } ?: "No response from AI."
            } else {
                "AI API Error: ${response.code} - ${response.message}"
            }
        } catch (e: IOException) {
            "Network Error: ${e.message}"
        } catch (e: Exception) {
            "Parsing Error: ${e.message}"
        }
    }
}


