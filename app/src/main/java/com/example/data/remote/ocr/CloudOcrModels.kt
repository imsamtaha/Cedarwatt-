package com.example.data.remote.ocr

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class CloudOcrProvider(
    val id: String,
    val displayName: String,
    val description: String
) {
    GOOGLE_CLOUD_VISION(
        id = "google_cloud_vision",
        displayName = "Google Cloud Vision",
        description = "High-precision document text detection via Google Cloud Vision API"
    ),
    GEMINI_DOCUMENT_AI(
        id = "gemini_document_vision",
        displayName = "Gemini Multimodal OCR",
        description = "Intelligent Lebanese bill extraction with Gemini 3.5 Flash"
    ),
    AWS_TEXTRACT(
        id = "aws_textract",
        displayName = "AWS Textract Engine",
        description = "Structured receipt & document analysis via AWS Textract pipeline"
    )
}

// --- Google Cloud Vision REST Models ---

@JsonClass(generateAdapter = true)
data class VisionBatchAnnotateRequest(
    @Json(name = "requests") val requests: List<VisionAnnotateRequest>
)

@JsonClass(generateAdapter = true)
data class VisionAnnotateRequest(
    @Json(name = "image") val image: VisionImage,
    @Json(name = "features") val features: List<VisionFeature>
)

@JsonClass(generateAdapter = true)
data class VisionImage(
    @Json(name = "content") val content: String // Base64 encoded JPEG
)

@JsonClass(generateAdapter = true)
data class VisionFeature(
    @Json(name = "type") val type: String = "DOCUMENT_TEXT_DETECTION",
    @Json(name = "maxResults") val maxResults: Int = 50
)

@JsonClass(generateAdapter = true)
data class VisionBatchAnnotateResponse(
    @Json(name = "responses") val responses: List<VisionAnnotateImageResponse>? = null
)

@JsonClass(generateAdapter = true)
data class VisionAnnotateImageResponse(
    @Json(name = "fullTextAnnotation") val fullTextAnnotation: VisionFullTextAnnotation? = null,
    @Json(name = "textAnnotations") val textAnnotations: List<VisionEntityAnnotation>? = null,
    @Json(name = "error") val error: VisionError? = null
)

@JsonClass(generateAdapter = true)
data class VisionFullTextAnnotation(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class VisionEntityAnnotation(
    @Json(name = "description") val description: String? = null,
    @Json(name = "confidence") val confidence: Float? = null,
    @Json(name = "score") val score: Float? = null
)

@JsonClass(generateAdapter = true)
data class VisionError(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "message") val message: String? = null
)

// --- AWS Textract REST Models ---

@JsonClass(generateAdapter = true)
data class TextractDetectDocumentTextRequest(
    @Json(name = "Document") val document: TextractDocumentPayload
)

@JsonClass(generateAdapter = true)
data class TextractDocumentPayload(
    @Json(name = "Bytes") val bytes: String // Base64 encoded document bytes
)

@JsonClass(generateAdapter = true)
data class TextractDetectDocumentTextResponse(
    @Json(name = "Blocks") val blocks: List<TextractBlock>? = null,
    @Json(name = "DocumentMetadata") val documentMetadata: TextractDocumentMetadata? = null
)

@JsonClass(generateAdapter = true)
data class TextractDocumentMetadata(
    @Json(name = "Pages") val pages: Int? = null
)

@JsonClass(generateAdapter = true)
data class TextractBlock(
    @Json(name = "BlockType") val blockType: String? = null,
    @Json(name = "Confidence") val confidence: Float? = null,
    @Json(name = "Text") val text: String? = null,
    @Json(name = "RowIndex") val rowIndex: Int? = null,
    @Json(name = "ColumnIndex") val columnIndex: Int? = null
)

// --- Parsed Bill Domain Model from OCR ---

data class ParsedBillData(
    val providerName: String,
    val billMonth: String,
    val kwhConsumption: Double,
    val amperage: Int,
    val ratePerKwhUsd: Double,
    val fixedFeeUsd: Double,
    val fuelSurchargeUsd: Double,
    val totalAmountUsd: Double,
    val totalAmountLbp: Double,
    val meterNumber: String,
    val invoiceNumber: String,
    val dueDate: String,
    val substation: String,
    val confidenceScore: Double,
    val rawExtractedText: String,
    val cloudProvider: CloudOcrProvider
)

data class CloudOcrResult(
    val isSuccess: Boolean,
    val parsedData: ParsedBillData,
    val rawText: String,
    val providerUsed: CloudOcrProvider,
    val errorMessage: String? = null
)
