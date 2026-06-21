package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content?
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

/**
 * Executes a text generation session with the Develgram AI Assistant.
 * Uses BuildConfig.GEMINI_API_KEY if present, otherwise provides interactive localized guidance.
 */
suspend fun queryGemini(prompt: String, chatHistory: List<MessageEntity>): String {
    val apiKey = BuildConfig.GEMINI_API_KEY
    if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
        return getLocalFallbackResponse(prompt)
    }

    try {
        // Construct standard chat history context for Gemini (last 8 messages)
        val conversationHistory = chatHistory.takeLast(8).map { msg ->
            Content(
                parts = listOf(Part(text = msg.text))
            )
        }

        // Add user prompt
        val contentsList = conversationHistory + Content(parts = listOf(Part(text = prompt)))

        val request = GenerateContentRequest(
            contents = contentsList,
            systemInstruction = Content(
                parts = listOf(
                    Part(
                        text = "You are @DevelgramAIBot, the official helpful AI chatbot for Develgram, a telegram-like client. " +
                                "Respond elegantly in Hindi/Urdu/Hinglish or English reflecting the user's querying style. " +
                                "Keep your replies concise (under 120 words), informative, witty, and helpful. Use emojis like a stylish Telegram bot."
                    )
                )
            )
        )

        val response = RetrofitClient.service.generateContent(apiKey, request)
        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: "Aapki request ka response khali mila. Kripya fir se koshish karein! 🤖"
    } catch (e: Exception) {
        return "Oops! Network issue aa gyi: ${e.localizedMessage}. Lekin Develgram offline features kaam kar rhe hain! 🌐✈️"
    }
}

/**
 * Intelligent localized fallback handler for off-line prototyping and seamless first-boot testing
 */
fun getLocalFallbackResponse(prompt: String): String {
    val cleanPrompt = prompt.lowercase().trim()
    return when {
        cleanPrompt.contains("hello") || cleanPrompt.contains("hi") || cleanPrompt.contains("salam") -> {
            "Salam! Welcome to **Develgram** (Telegram Clone) ✈️!\n\nMain aapka AI Assistant hu. " +
            "Aap real-time mere sath chat kar sakte hain. Aap Gemini API key configure karke direct real replies pa sakte hain!\n\nAap custom themes explore karne ke liye top-right Settings or Sidebar check karein!"
        }
        cleanPrompt.contains("kaise ho") || cleanPrompt.contains("how are you") -> {
            "Main ekdum badiya hu! 🚀 Aap bataiye, Develgram experience kaisa lag rha hai? " +
            "Aap channels join kar sakte hain or user interface check kar sakte hain."
        }
        cleanPrompt.contains("develgram") || cleanPrompt.contains("telegram") -> {
            "**Develgram** ek ultra-fast messaging framework hai jo Modern Jetpack Compose & State-Persistence (Room Database) se bna hai.\n\n" +
            "Features:\n" +
            "• Direct Chat & Persistent Threads 💬\n" +
            "• Custom UI Themes (Classic Blue, Sunset Red, Velvet, Matrix Green) 🎨\n" +
            "• Local Group chats and announcements channels 🎙️\n" +
            "• Search messages indexingly."
        }
        cleanPrompt.contains("help") || cleanPrompt.contains("features") || cleanPrompt.contains("caise") -> {
            "Develgram commands list:\n" +
            "• Mujhe naye features ya ideas pucho 💡\n" +
            "• Profile changes test karne settings button tap karein ⚙️\n" +
            "• Chat thread switch karne sidebar se selection kijiye 📚\n\n" +
            "*Tip: Apne API Studio panel me 'GEMINI_API_KEY' add karke real AI active karein!*"
        }
        cleanPrompt.contains("joke") || cleanPrompt.contains("hansa") -> {
            "Ek programming joke: 😂\n\n" +
            "Why do programmers wear glasses?\n" +
            "Because they can't C#! 💻🤓\n" +
            "Pasand aya? Ek or bar try kijiye!"
        }
        else -> {
            "Aapne likha: \"$prompt\"\n\n" +
            "Main offline mode me hu kyunki AI Studio me `GEMINI_API_KEY` set nahi hai. Lekin database persistence chal rha hai! ✅\n\n" +
            "Kripya AI Studio dashboard ke *Secrets panel* me ek valid Google Gemini key fill kijiye!"
        }
    }
}
