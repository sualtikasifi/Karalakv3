package com.sualtikasifi.cizimhafiza.presentation.reports.console

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.net.HttpURLConnection
import java.net.URL

data class AiAnswer(
    val text: String,
    /** The model that actually answered (a fallback may have stepped in). */
    val model: String,
    /** Pages the answer leaned on when web search was used: title to address. */
    val sources: List<Pair<String, String>> = emptyList(),
    val searched: Boolean = false
)

/**
 * The developer panel's line to Gemini through the Gemini Developer API (generateContent).
 *
 * The key is the maintainer's own free key from Google AI Studio, typed into the panel and kept
 * only on that phone — it is never in the APK, never in Firestore. A project with no billing
 * account stays on the free tier, so the worst a leaked key can do is use up the free quota.
 *
 * Model names move every few months; the panel's choice is tried first, then [FALLBACK_MODELS],
 * and Google Search grounding is dropped automatically when a model or the free tier refuses it.
 */
class GeminiClient(private val apiKey: String) {

    suspend fun generate(system: String, prompt: String, model: String, webSearch: Boolean): Result<AiAnswer> =
        withContext(Dispatchers.IO) {
            val models = (listOf(model.trim()) + FALLBACK_MODELS).filter { it.isNotBlank() }.distinct()
            var lastError: Throwable = IllegalStateException("Model yok")
            for (m in models) {
                for (search in if (webSearch) listOf(true, false) else listOf(false)) {
                    val r = call(m, system, prompt, search)
                    if (r.isSuccess) return@withContext r
                    val e = r.exceptionOrNull() as? ApiError ?: return@withContext r
                    lastError = e
                    when {
                        // a bad key will not get better with another model
                        e.code == 400 && e.message.orEmpty().contains("API key", ignoreCase = true) -> return@withContext r
                        e.code == 401 -> return@withContext r
                        // search refused (not on this model / tier): same model without it
                        search && (e.code == 400 || e.code == 403 || e.code == 429) -> continue
                        // model gone, not on this tier, or out of quota: next model
                        else -> break
                    }
                }
            }
            Result.failure(lastError)
        }

    class ApiError(val code: Int, message: String) : Exception(message)

    private fun call(model: String, system: String, prompt: String, search: Boolean): Result<AiAnswer> = runCatching {
        val body = buildJsonObject {
            putJsonObject("systemInstruction") { putJsonArray("parts") { addJsonObject { put("text", system) } } }
            putJsonArray("contents") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("parts") { addJsonObject { put("text", prompt) } }
                }
            }
            if (search) putJsonArray("tools") { addJsonObject { putJsonObject("google_search") {} } }
            putJsonObject("generationConfig") {
                put("temperature", 0.9)
                put("maxOutputTokens", 8192)
            }
        }
        val conn = (URL("$BASE/$model:generateContent").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 150_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("x-goog-api-key", apiKey)
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val raw = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val msg = runCatching {
                    Json.parseToJsonElement(raw).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
                }.getOrNull() ?: raw.take(300)
                throw ApiError(code, "$model: $msg")
            }
            parse(raw, model, search)
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(raw: String, model: String, search: Boolean): AiAnswer {
        val root = Json.parseToJsonElement(raw).jsonObject
        val cand = (root["candidates"] as? JsonArray)?.firstOrNull()?.jsonObject
            ?: throw ApiError(200, "Boş cevap: " + (root["promptFeedback"]?.toString() ?: raw.take(200)))
        val parts = cand["content"]?.jsonObject?.get("parts") as? JsonArray ?: JsonArray(emptyList())
        val text = parts.mapNotNull { p ->
            val o = p.jsonObject
            if (o["thought"]?.jsonPrimitive?.booleanOrNull == true) null else o["text"]?.jsonPrimitive?.contentOrNull
        }.joinToString("")
        if (text.isBlank()) throw ApiError(200, "Cevap metni yok (${cand["finishReason"]?.jsonPrimitive?.contentOrNull ?: "?"})")
        val sources = (cand["groundingMetadata"] as? JsonObject)?.get("groundingChunks")?.let { it as? JsonArray }.orEmpty().mapNotNull { c ->
            val web = c.jsonObject["web"]?.jsonObject ?: return@mapNotNull null
            val uri = web["uri"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            (web["title"]?.jsonPrimitive?.contentOrNull ?: uri) to uri
        }.distinctBy { it.second }
        return AiAnswer(text = text.trim(), model = model, sources = sources, searched = search)
    }

    companion object {
        private const val BASE = "https://generativelanguage.googleapis.com/v1beta/models"
        const val DEFAULT_MODEL = "gemini-3.8-flash"
        /** Free-tier Flash models, newest first (Firebase AI Logic model list, October 2026). */
        val FALLBACK_MODELS = listOf("gemini-3.8-flash", "gemini-3.5-flash", "gemini-3.5-flash-lite", "gemini-3.1-flash-lite")
    }
}
