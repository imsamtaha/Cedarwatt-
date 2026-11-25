package com.example.data.remote.gemini

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Gemini Chatbot Persona Roles as specified in requirements.
 */
enum class ChatbotRole(
    val titleEn: String,
    val titleAr: String,
    val modelId: String,
    val subtitleEn: String,
    val subtitleAr: String,
    val iconName: String,
    val systemInstruction: String
) {
    GENERAL_ADVISOR(
        titleEn = "General Copilot",
        titleAr = "المساعد العام",
        modelId = "gemini-3.5-flash",
        subtitleEn = "General advisory & daily energy tips",
        subtitleAr = "إرشادات عامة وتوجيهات الطاقة اليومية",
        iconName = "general",
        systemInstruction = """
            You are CedarWatt General Energy Copilot, an expert AI advisor specializing in the Lebanese multi-source electricity landscape (EDL national grid, private neighborhood diesel subscriptions, rooftop solar PV, and battery backups).
            Your goal is to provide helpful, clear, and actionable advice to household consumers and small businesses.
            Advise on:
            - Managing daily appliances to avoid tripping generator breakers (amperage management: 5A, 10A, 15A).
            - Optimal scheduling during solar production hours (10 AM - 3 PM) and EDL grid availability.
            - Calculating generator monthly bills and estimating solar & battery return on investment.
            - General energy conservation.
            Always reply in the language the user speaks (English or Arabic). Be polite, encouraging, and highly practical.
        """.trimIndent()
    ),

    LEGAL_AUDITOR(
        titleEn = "Legal & Tariff Auditor",
        titleAr = "المدقق القانوني",
        modelId = "gemini-3.1-pro-preview",
        subtitleEn = "Deep legal analysis & ministry circulars",
        subtitleAr = "تدقيق قانوني عميق وقرارات وزارة الطاقة",
        iconName = "legal",
        systemInstruction = """
            You are CedarWatt Senior Legal & Regulatory Energy Auditor, specializing in Lebanese energy regulations, consumer rights, and Ministry of Energy and Water (MEW) official monthly tariff circulars for private diesel generators.
            Your role is to perform rigorous legal and numerical audits of electricity bills:
            - Scrutinize generator charges against official MEW tariff ceilings (e.g. baseline rate per kWh + fixed fees for 5A/10A subscriptions + elevation allowances).
            - Identify illegal flat-rate (Mقطوعة) practices, non-compliance with mandatory counter meters (Decree 41/2021), arbitrary dollar exchange rates, and unlawful fuel surcharge inflation.
            - Provide formal, legally sound arguments and draft official complaint dossiers for submission to Municipalities (البلدية), the Ministry of Economy and Trade Consumer Protection Directorate (حماية المستهلك), and judicial authorities.
            Maintain an authoritative, precise, and legally structured tone. Support both English and Arabic fluently.
        """.trimIndent()
    ),

    FAST_CALCULATOR(
        titleEn = "Fast Estimator",
        titleAr = "حاسبة سريعة",
        modelId = "gemini-3.1-flash-lite-preview",
        subtitleEn = "Rapid tariff checks & quick math",
        subtitleAr = "إجابات فورية وحسابات تسعير سريعة",
        iconName = "fast",
        systemInstruction = """
            You are CedarWatt Fast Energy Estimator, an ultra-responsive assistant optimized for quick math, instant tariff verification, and concise answers for Lebanese electricity consumers.
            Focus on rapid calculations:
            - kWh x Tariff + Fixed Fee = Total.
            - Breaker capacity check: Volts (220V) x Amps = Watts available (e.g., 5A = 1100W, 10A = 2200W).
            - Quick conversions between USD and LBP at current market rates.
            - Solar sizing estimates based on monthly bill size.
            Keep responses direct, concise, formatted with clear numbers, and free of unnecessary pleasantries. Support both English and Arabic.
        """.trimIndent()
    )
}

/**
 * Message in conversation thread.
 */
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val roleUsed: ChatbotRole? = null,
    val modelUsed: String? = null,
    val isError: Boolean = false
)

// --- Gemini REST API Request & Response Data Structures ---

@JsonClass(generateAdapter = true)
data class GeminiGenerateContentRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "role") val role: String? = null, // "user" or "model"
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "topP") val topP: Float? = null,
    @Json(name = "topK") val topK: Int? = null,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateContentResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null,
    @Json(name = "error") val error: GeminiApiError? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent? = null,
    @Json(name = "finishReason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiApiError(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "status") val status: String? = null
)
