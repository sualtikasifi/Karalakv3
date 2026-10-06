package com.sualtikasifi.cizimhafiza.presentation.common

import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.LeagueBots

/**
 * Profile pictures for the league's filler players. A bot's name decides which kind of picture it gets — a woman's
 * name a woman's photo, a man's name a man's, and a joke or object name (simitcay, bumbum, ucanbalik…) a pet or a
 * view — and each bot always gets the same one: its place within its group, in the order of [LeagueBots.NAMES],
 * picks the picture, so two bots of one group only share a picture once the group has run out of them.
 * Names that are not league bots get null and keep the generated avatar.
 */
object BotPhotos {
    private val FEMALE_NAMES = setOf(
        "ceyda8821", "cananabaci", "aysenur11", "zynpcetn", "gizemlikiz", "mimarmerve", "hemsiremelisa", "merve742",
        "komsukizi", "aleyna34", "bsgul", "iremsu", "glsh", "karaeylem", "soley", "yagmurlu", "laylaylom", "laylon",
        "ametist"
    )
    private val NEUTRAL_NAMES = setOf(
        "minikkus", "gamsizbaykus", "ucanbalik", "amatorbalikci", "simitcay", "caykolik", "cikkofteci", "caykasigi",
        "sekersiz", "bolacili", "sarmisakli", "uykumvar", "nebilimben", "bosver", "falanfilan", "ivirzivir",
        "baksanabana", "belkide", "hicbiri", "sonsoz", "oburki", "isimsiz", "heryeryesil", "lufersesi", "sahteyem",
        "cufcuf", "wqewqe", "bumbum", "qweasd", "tofask", "mnyk", "kdr", "orjinall", "suprizci", "yalnizdegil",
        "herkezgitsin", "sessizkalan", "uykucu", "ogrenciyiz", "tekbasina", "gocebe", "yalnizim", "kafkef", "kafkef88",
        "pesimist", "spinci", "lodos", "sariyildiz", "gulyabani", "krdsler", "fth123"
    )

    private val MEN = listOf(
        R.drawable.bot_photo_a01, R.drawable.bot_photo_a04, R.drawable.bot_photo_a08, R.drawable.bot_photo_a11,
        R.drawable.bot_photo_a12, R.drawable.bot_photo_a13, R.drawable.bot_photo_a17, R.drawable.bot_photo_a21,
        R.drawable.bot_photo_a23, R.drawable.bot_photo_a24, R.drawable.bot_photo_a26, R.drawable.bot_photo_b01,
        R.drawable.bot_photo_b04, R.drawable.bot_photo_b06, R.drawable.bot_photo_b09, R.drawable.bot_photo_b12,
        R.drawable.bot_photo_b14, R.drawable.bot_photo_b15, R.drawable.bot_photo_b17, R.drawable.bot_photo_b20,
        R.drawable.bot_photo_b21, R.drawable.bot_photo_b23, R.drawable.bot_photo_b26, R.drawable.bot_photo_b29,
        R.drawable.bot_photo_b32, R.drawable.bot_photo_b34
    )
    private val WOMEN = listOf(
        R.drawable.bot_photo_a02, R.drawable.bot_photo_a05, R.drawable.bot_photo_a07, R.drawable.bot_photo_a10,
        R.drawable.bot_photo_a15, R.drawable.bot_photo_a19, R.drawable.bot_photo_a25, R.drawable.bot_photo_a27,
        R.drawable.bot_photo_a29, R.drawable.bot_photo_b02, R.drawable.bot_photo_b05, R.drawable.bot_photo_b11,
        R.drawable.bot_photo_b13, R.drawable.bot_photo_b16, R.drawable.bot_photo_b19, R.drawable.bot_photo_b22,
        R.drawable.bot_photo_b24, R.drawable.bot_photo_b28, R.drawable.bot_photo_b31, R.drawable.bot_photo_b35
    )
    /** Pets and views, interleaved so neighbours in the list are not all cats. */
    private val NEUTRAL = listOf(
        R.drawable.bot_photo_a03, R.drawable.bot_photo_a09, R.drawable.bot_photo_a06, R.drawable.bot_photo_a16,
        R.drawable.bot_photo_a14, R.drawable.bot_photo_a18, R.drawable.bot_photo_a20, R.drawable.bot_photo_a28,
        R.drawable.bot_photo_a22, R.drawable.bot_photo_b08, R.drawable.bot_photo_a30, R.drawable.bot_photo_b25,
        R.drawable.bot_photo_b03, R.drawable.bot_photo_b07, R.drawable.bot_photo_b10, R.drawable.bot_photo_b18,
        R.drawable.bot_photo_b27, R.drawable.bot_photo_b30, R.drawable.bot_photo_b33
    )

    private val assigned: Map<String, Int> by lazy {
        var m = 0; var w = 0; var n = 0
        LeagueBots.NAMES.associateWith { name ->
            when (name) {
                in FEMALE_NAMES -> WOMEN[w++ % WOMEN.size]
                in NEUTRAL_NAMES -> NEUTRAL[n++ % NEUTRAL.size]
                else -> MEN[m++ % MEN.size]
            }
        }
    }

    /** The drawable for league bot [name], or null when it is not one of them. */
    fun drawableFor(name: String): Int? = assigned[name]
}
