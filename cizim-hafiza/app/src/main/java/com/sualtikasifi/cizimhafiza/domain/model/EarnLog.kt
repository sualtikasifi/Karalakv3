package com.sualtikasifi.cizimhafiza.domain.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * The running totals of XP and gold by [EarnSource] for ONE league month: they start again when the month changes, so the panel
 * reads "this month". Kept as a small JSON string in the settings file (no database, nothing to migrate) and written with the
 * XP / gold themselves, so it cannot drift away from them.
 *
 * It only counts what is earned from now on: an account that has not played since this was added shows nothing for the past.
 */
object EarnLog {
    /** What the log holds: the month it belongs to and the total of each source, for XP and for gold. */
    data class Totals(val period: Long, val xp: Map<String, Long>, val gold: Map<String, Long>) {
        val isEmpty: Boolean get() = xp.isEmpty() && gold.isEmpty()
    }

    val EMPTY = Totals(-1L, emptyMap(), emptyMap())

    fun parse(raw: String?): Totals {
        if (raw.isNullOrBlank()) return EMPTY
        return runCatching {
            val o = Json.parseToJsonElement(raw).jsonObject
            fun read(name: String): Map<String, Long> =
                (o[name] as? JsonObject)?.mapValues { it.value.jsonPrimitive.longOrNull ?: 0L }.orEmpty().filterValues { it > 0L }
            Totals(o["p"]?.jsonPrimitive?.longOrNull ?: -1L, read("xp"), read("gold"))
        }.getOrDefault(EMPTY)
    }

    fun encode(t: Totals): String = buildJsonObject {
        put("p", t.period)
        put("xp", buildJsonObject { t.xp.forEach { (k, v) -> put(k, v) } })
        put("gold", buildJsonObject { t.gold.forEach { (k, v) -> put(k, v) } })
    }.toString()

    /** [raw] with [amount] of [kind] ("xp" or "gold") added to [source] in [period]; a log of an earlier month starts again. */
    fun add(raw: String?, period: Long, kind: String, source: EarnSource, amount: Int): String {
        if (amount <= 0) return raw.orEmpty()
        val current = parse(raw).let { if (it.period == period) it else Totals(period, emptyMap(), emptyMap()) }
        fun bump(m: Map<String, Long>) = m + (source.key to (m[source.key] ?: 0L) + amount)
        return encode(if (kind == "xp") current.copy(xp = bump(current.xp)) else current.copy(gold = bump(current.gold)))
    }
}
