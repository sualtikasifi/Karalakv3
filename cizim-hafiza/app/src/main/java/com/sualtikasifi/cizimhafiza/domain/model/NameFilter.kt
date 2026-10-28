package com.sualtikasifi.cizimhafiza.domain.model

import java.text.Normalizer
import java.util.Locale

/**
 * Keeps profanity, slurs and impersonation out of the names other players see (usernames: UsernameRepository.change,
 * the league, rooms, friend lists). Turkish, English, Portuguese and Spanish.
 *
 * A name is compared after folding it to plain a–z: case and accents dropped (ş→s, ı→i, ñ→n), digits and symbols read
 * the way they are used to dodge filters (0→o, 1→i, 3→e, 4→a, 5→s, 7→t, 8→b, @→a, $→s), runs of three or more of the
 * same letter cut to one ("fuuuck"). Each check runs twice, once with digits read as letters and once with them read as
 * separators, so "amk34" is caught as well as "s1kt1r". Then:
 *  - [SUBSTRINGS] are refused anywhere in the name, separators ignored ("o.r.o.s.p.u");
 *  - [TOKENS] are words too short or too common inside innocent ones ("pic" in "epic", "puta" in "disputa", "yarak" in
 *    "okuyarak") — refused only as a whole word of the name or as the whole name.
 *
 * The web game uses the same lists: scripts/gen_web_namefilter.js copies them into hosting/oyna/namefilter_data.js.
 * Edit them here and run it. A filter is never complete; reports (playerReports) and moderation catch the rest.
 */
object NameFilter {

    val SUBSTRINGS: List<String> = listOf(
        // Turkish
        "orospu", "orosbu", "siktir", "sikis", "sikim", "sikik", "sikerim", "sikeyim", "sikiyim", "sikici", "sikicem",
        "yarrak", "dalyarak", "amcik", "aminak", "aminako", "kahpe", "pezevenk", "gavat", "ibne", "kaltak",
        "fahise", "surtuk", "yavsak", "tasak", "gotveren", "gotlek", "kevase", "kerhane",
        // English
        "fuck", "shit", "bitch", "cunt", "nigger", "nigga", "faggot", "whore", "slut", "porn", "pussy", "asshole",
        "bastard", "hitler", "penis", "vagina", "retard", "motherf",
        // Spanish
        "hijodeputa", "mierda", "pendejo", "cabron", "maricon", "joder", "chinga", "culero", "gilipollas", "malparido",
        // Portuguese
        "caralho", "fodase", "foder", "buceta", "cuzao", "arrombado", "piroca", "vagabunda", "punheta", "xoxota",
        "boquete", "merda",
        // Impersonation
        "moderator", "moderater", "administrator"
    )

    val TOKENS: Set<String> = setOf(
        // Turkish
        "amk", "aq", "amq", "sik", "got", "pic", "oc", "mk", "yarak", "pust", "sg",
        // English
        "fuk", "fck", "fk", "rape", "dick", "cock", "cum", "sex", "nazi", "kys", "ass", "fag", "tit", "tits", "kkk",
        // Spanish
        "puta", "puto", "hdp", "verga", "polla", "zorra", "cono",
        // Portuguese
        "porra", "foda", "fdp", "viado", "cu", "vsf", "pqp",
        // Impersonation
        "admin", "mod", "yetkili", "destek", "support", "karalakdestek"
    )

    /** False when [name] holds any of the refused words (see the class doc for how it is compared). */
    fun isAllowed(name: String): Boolean {
        val plain = plain(name)
        for (variant in listOf(leet(plain), plain)) {
            val letters = collapse(variant.filter { it in 'a'..'z' })
            if (SUBSTRINGS.any { letters.contains(it) } || letters in TOKENS) return false
            val tokens = variant.split(NON_LETTERS).filter { it.isNotEmpty() }
            if (tokens.any { it in TOKENS || collapse(it) in TOKENS }) return false
        }
        return true
    }

    /** Lower case with accents dropped (Turkish dotted and dotless i both become i). */
    fun plain(name: String): String {
        val lower = name.replace('İ', 'i').replace('I', 'i').replace('ı', 'i').lowercase(Locale.ROOT)
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replace(MARKS, "")
    }

    /** Look-alike digits and symbols read as the letters they stand in for. */
    fun leet(plain: String): String =
        buildString(plain.length) {
            plain.forEach { c ->
                append(
                    when (c) {
                        '0' -> 'o'; '1' -> 'i'; '3' -> 'e'; '4' -> 'a'; '5' -> 's'; '7' -> 't'; '8' -> 'b'
                        '@' -> 'a'; '$' -> 's'; '!' -> 'i'; 'ß' -> 's'
                        else -> c
                    }
                )
            }
        }

    /** Runs of three or more of one letter cut to one: "fuuuck" → "fuck", while "yarrak" keeps its double r. */
    fun collapse(s: String): String = s.replace(REPEATS, "$1")

    private val NON_LETTERS = Regex("[^a-z]+")
    private val MARKS = Regex("\\p{M}+")
    private val REPEATS = Regex("([a-z])\\1{2,}")
}
