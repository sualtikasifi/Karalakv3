package com.sualtikasifi.cizimhafiza.presentation.reports.console

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
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
    val searched: Boolean = false,
    /** What this one answer cost, in US dollars, as OpenRouter billed it; null when unknown. */
    val costUsd: Double? = null,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0
)

/** The key's spend as OpenRouter reports it, in US dollars. [limitRemaining] is null for a key with no limit. */
data class KeyUsage(val daily: Double, val monthly: Double, val total: Double, val limit: Double?, val limitRemaining: Double?)

/**
 * The developer panel's line to a model through OpenRouter (chat completions).
 *
 * The key is the maintainer's own OpenRouter key, typed into the panel and kept only on that
 * phone — it is never in the APK, never in Firestore. It spends real credit, so the panel tells
 * whoever sets it up to put a spending limit on it; every answer shows what it cost and the
 * settings card shows the key's spend for the day and the month.
 *
 * Model names come and go (OpenRouter publishes a removal date for each); the panel's choice is
 * tried first, then [FALLBACK_MODELS], and web search is dropped automatically when it is refused.
 */
class OpenRouterClient(private val apiKey: String) {

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
                        // a bad key or an empty balance will not get better with another model
                        e.code == 401 || e.code == 402 || e.code == 403 -> return@withContext r
                        // search refused for this model: same model without it
                        search && e.code == 400 -> continue
                        // model gone or renamed, busy, or down: next model
                        else -> break
                    }
                }
            }
            Result.failure(lastError)
        }

    /** The key's spend so far, from GET /key. */
    suspend fun usage(): Result<KeyUsage> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL("$BASE/key").openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("Authorization", "Bearer $apiKey")
            }
            try {
                val code = conn.responseCode
                val raw = read(conn, code)
                if (code !in 200..299) throw ApiError(code, errorMessage(code, raw))
                val d = Json.parseToJsonElement(raw).jsonObject["data"]?.jsonObject ?: throw ApiError(code, "Beklenmeyen cevap")
                fun num(name: String) = (d[name] as? JsonPrimitive)?.doubleOrNull
                KeyUsage(
                    daily = num("usage_daily") ?: 0.0,
                    monthly = num("usage_monthly") ?: 0.0,
                    total = num("usage") ?: 0.0,
                    limit = num("limit"),
                    limitRemaining = num("limit_remaining")
                )
            } finally {
                conn.disconnect()
            }
        }
    }

    class ApiError(val code: Int, message: String) : Exception(message)

    private fun call(model: String, system: String, prompt: String, search: Boolean): Result<AiAnswer> = runCatching {
        val body = buildJsonObject {
            put("model", model)
            putJsonArray("messages") {
                addJsonObject { put("role", "system"); put("content", system) }
                addJsonObject { put("role", "user"); put("content", prompt) }
            }
            put("temperature", 0.9)
            put("max_tokens", 8192)
            // a little thinking is enough for plans and posts, and every thought is billed as output
            putJsonObject("reasoning") { put("effort", "low"); put("exclude", true) }
            if (search) putJsonArray("plugins") { addJsonObject { put("id", "web"); put("max_results", 5) } }
        }
        val conn = (URL("$BASE/chat/completions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 180_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $apiKey")
            // shows up as the app's name on the OpenRouter activity page
            setRequestProperty("HTTP-Referer", "https://karalak.com")
            setRequestProperty("X-OpenRouter-Title", "Karalak Panel")
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val raw = read(conn, code)
            if (code !in 200..299) throw ApiError(code, "$model: " + errorMessage(code, raw))
            parse(raw, model, search)
        } finally {
            conn.disconnect()
        }
    }

    private fun read(conn: HttpURLConnection, code: Int): String =
        (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

    private fun errorMessage(code: Int, raw: String): String {
        val msg = runCatching {
            Json.parseToJsonElement(raw).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
        }.getOrNull() ?: raw.take(300)
        val hint = when (code) {
            401 -> "Anahtar geçersiz. "
            402 -> "Bakiye yetersiz; openrouter.ai'den kredi yükle ya da anahtarın limitini artır. "
            403 -> "Anahtarın limiti doldu ya da istek engellendi. "
            429 -> "Çok sık istek; biraz sonra dene. "
            else -> ""
        }
        return hint + msg
    }

    // internal for the unit test: the response shape is the part a device run would otherwise be the first to check
    internal fun parse(raw: String, model: String, search: Boolean): AiAnswer {
        val root = Json.parseToJsonElement(raw).jsonObject
        (root["error"] as? JsonObject)?.let { err ->
            throw ApiError(err["code"]?.jsonPrimitive?.intOrNull ?: 500, "$model: " + (err["message"]?.jsonPrimitive?.contentOrNull ?: "hata"))
        }
        val choice = (root["choices"] as? JsonArray)?.firstOrNull()?.jsonObject
            ?: throw ApiError(500, "$model: boş cevap")
        (choice["error"] as? JsonObject)?.let { err ->
            throw ApiError(err["code"]?.jsonPrimitive?.intOrNull ?: 500, "$model: " + (err["message"]?.jsonPrimitive?.contentOrNull ?: "hata"))
        }
        val message = choice["message"]?.jsonObject
        val text = (message?.get("content") as? JsonPrimitive)?.contentOrNull.orEmpty()
        if (text.isBlank()) throw ApiError(500, "$model: cevap metni yok (${choice["finish_reason"]?.jsonPrimitive?.contentOrNull ?: "?"})")
        val sources = (message?.get("annotations") as? JsonArray).orEmpty().mapNotNull { a ->
            val cite = a.jsonObject["url_citation"]?.jsonObject ?: return@mapNotNull null
            val url = cite["url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            (cite["title"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: url) to url
        }.distinctBy { it.second }
        val usage = root["usage"] as? JsonObject
        return AiAnswer(
            text = text.trim(),
            model = root["model"]?.jsonPrimitive?.contentOrNull ?: model,
            sources = sources,
            searched = search,
            costUsd = (usage?.get("cost") as? JsonPrimitive)?.doubleOrNull,
            promptTokens = (usage?.get("prompt_tokens") as? JsonPrimitive)?.intOrNull ?: 0,
            completionTokens = (usage?.get("completion_tokens") as? JsonPrimitive)?.intOrNull ?: 0
        )
    }

    companion object {
        private const val BASE = "https://openrouter.ai/api/v1"

        /**
         * Gemini Flash Lite through OpenRouter: the cheapest Gemini that writes a whole plan well.
         * 2.5 Flash Lite was the first choice, but OpenRouter lists it for removal on 2026-10-20,
         * so its successor is the default; 2.5 can still be typed in while it lasts.
         */
        const val DEFAULT_MODEL = "google/gemini-3.1-flash-lite"
        /** Tried in order when the chosen model fails (OpenRouter model list, October 2026). */
        val FALLBACK_MODELS = listOf("google/gemini-3.1-flash-lite", "google/gemini-3.5-flash-lite", "google/gemini-3.8-flash")

        /** "$0.012" style, with enough digits that a fraction of a cent still reads. */
        fun usd(v: Double): String = when {
            v >= 1 -> String.format(java.util.Locale.US, "$%.2f", v)
            v >= 0.01 -> String.format(java.util.Locale.US, "$%.3f", v)
            else -> String.format(java.util.Locale.US, "$%.4f", v)
        }
    }
}
