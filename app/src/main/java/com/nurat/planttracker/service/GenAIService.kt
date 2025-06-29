package com.nurat.planttracker.service

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

interface GenAIService {
    suspend fun analyzeCommitMessage(commitMessage: String): String
}

class GeminiGenAIService(private val apiKey: String) : GenAIService {
    private val client = OkHttpClient()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    // Create a Json instance for serialization.
    // Configure it to be strict or lenient as needed, but for simple string encoding, defaults are often fine.
    private val json = Json {
        // This is often useful for parsing responses where keys might be missing or unordered
        ignoreUnknownKeys = true
    }

    override suspend fun analyzeCommitMessage(commitMessage: String): String {
        // Step 1: Construct the raw prompt string. This is the text content we want to send to Gemini.
        // It might contain characters that are problematic for JSON embedding.
        val rawPromptContent = "Analyze the following commit message and suggest how the related code might be improved: \"$commitMessage\". Keep the suggestion concise and actionable."

        // Step 2: Use Kotlinx Serialization's Json.encodeToString to convert `rawPromptContent`
        // into a *valid JSON string literal*. This function automatically handles escaping
        // all problematic characters (like double quotes, newlines, backslashes).
        val jsonSafePromptText: String = json.encodeToString(rawPromptContent)

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=$apiKey"

        // Step 3: Construct the final JSON payload.
        // IMPORTANT: Notice that $jsonSafePromptText is embedded *without* additional quotes around it.
        // This is because `json.encodeToString` already produces a quoted string, like "\"your escaped string\"".
        val payload = """
            {
                "contents": [
                    {
                        "role": "user",
                        "parts": [
                            { "text": $jsonSafePromptText }
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

        // Log the constructed payload for debugging purposes
        println("GeminiGenAIService: Sending payload: $payload")

        val requestBody = payload.toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful) {
                if (responseBody.isNullOrEmpty()) {
                    println("GeminiGenAIService: Empty response body from AI.")
                    return "AI: Received empty response."
                }
                println("GeminiGenAIService: Raw successful response body: $responseBody")
                try {
                    val jsonElement = json.parseToJsonElement(responseBody)
                    jsonElement.jsonObject["candidates"]?.jsonArray?.get(0)?.jsonObject
                        ?.get("content")?.jsonObject
                        ?.get("parts")?.jsonArray?.get(0)?.jsonObject
                        ?.get("text")?.jsonPrimitive?.content ?: "AI: No text part found in response."
                } catch (e: Exception) {
                    println("GeminiGenAIService: JSON Parsing Error: ${e.message}. Raw body: $responseBody")
                    e.printStackTrace()
                    return "AI: Parsing error - unexpected response format. (Raw: $responseBody)"
                }
            } else {
                println("GeminiGenAIService: AI API Error: ${response.code} - ${response.message}. Raw error body: $responseBody")
                return "AI API Error: ${response.code} - ${response.message}. (Details: $responseBody)"
            }
        } catch (e: IOException) {
            println("GeminiGenAIService: Network Error: ${e.message}")
            e.printStackTrace()
            return "AI: Network error: ${e.message}"
        } catch (e: Exception) {
            println("GeminiGenAIService: General Error: ${e.localizedMessage ?: e.message ?: e.javaClass.simpleName}")
            e.printStackTrace()
            return "AI: An unexpected error occurred: ${e.localizedMessage ?: e.message ?: e.javaClass.simpleName}"
        }
    }
}
