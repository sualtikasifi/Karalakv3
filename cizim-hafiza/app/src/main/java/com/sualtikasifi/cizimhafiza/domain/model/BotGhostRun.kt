package com.sualtikasifi.cizimhafiza.domain.model

import com.sualtikasifi.cizimhafiza.util.GameConstants
import kotlin.random.Random

/**
 * What Sude scored in a round she was never actually in.
 *
 * The offer and the result screen are two separate journeys through the app
 * — the opponent is handed to the game screen as a route argument and the
 * drawings are fetched much later, from a different collection — so both
 * have to arrive at the identical answer with nothing passed between them.
 * That is why every field here is derived, not stored.
 */
data class BotGhostOutcome(
    /** Per word, in the run's own order: did she recall her own drawing? */
    val correctness: List<Boolean>,
    val totalScore: Int,
    val correctCount: Int,
    val fastestCorrectMs: Long?
)

/**
 * A Hızlı Eşleş opponent assembled from the hand-trained drawing set
 * (`botTrainedWords`, see BotTrainingRepository) rather than from a round
 * anybody played.
 *
 * The pool grows with games played, which is a slow way to start: for the
 * first player of the day there is nothing to be matched against, and being
 * told "havuz henüz boş" is exactly the moment somebody stops opening the
 * mode — so the pool never gets the rounds that would have filled it. There
 * are already hundreds of real, hand-drawn words sitting in Firestore for
 * the online bot room; this lets that same data answer the empty-pool case,
 * and it costs one small document read to do it.
 *
 * These are a FALLBACK, never a preference: [GhostRunRepository.findOpponent]
 * reaches for one only after the real pool has been walked band by band and
 * come back with nobody. As real rounds accumulate they are offered less and
 * less often, without anything having to be switched off.
 *
 * The player each round is presented as comes from [GhostPersonas] — a
 * generated name and a level near the challenger's, not one fixed character.
 * A single recurring opponent is transparently not a pool, and the point of
 * this mode is to feel like there is one.
 *
 * ### Why the run id carries the whole round
 *
 * A real opponent's drawings live in `ghostRunItems/{runId}`, so the id is
 * enough to find them later. These do not exist as a round at all — they
 * are individual trained words — so the id has to carry what a stored
 * document would have: which words, and which roll of the dice. Everything
 * else is re-derived from those two, identically, on both sides.
 */
object BotGhostRuns {

    private const val ID_PREFIX = "ghost:"

    /**
     * How often she rides a correct answer's speed bonus. Same idea as
     * BotRoomEngine's, and the same reason: a bot whose score is always an
     * exact multiple of five is a bot.
     */
    private const val SPEED_BONUS_PERCENT = 40

    fun isBotRun(runId: String): Boolean = runId.startsWith(ID_PREFIX)

    fun idFor(seed: Long, wordIds: List<Int>): String =
        ID_PREFIX + seed + ":" + wordIds.joinToString(",")

    /** The word ids and dice roll packed into [idFor], or null if this is not one of hers. */
    fun parse(runId: String): Pair<Long, List<Int>>? {
        if (!isBotRun(runId)) return null
        val body = runId.removePrefix(ID_PREFIX)
        val seed = body.substringBefore(':', "").toLongOrNull() ?: return null
        val wordIds = body.substringAfter(':', "")
            .split(',')
            .map { it.toIntOrNull() ?: return null }
            .takeIf { it.isNotEmpty() }
            ?: return null
        return seed to wordIds
    }

    /**
     * Sude always DRAWS her trained strokes — that half is genuinely hers —
     * but she does not always recall her own drawing afterwards, exactly as
     * a real player forgets one of theirs.
     *
     * The distribution is deliberately harsher than BotRoomEngine's, whose
     * 40% chance of a clean sweep would hand a beginner an unbeatable score
     * in four matches out of ten. A quick match is somebody's first taste of
     * playing against another person, and losing every time to a perfect
     * stranger is the version of this feature nobody plays twice.
     */
    fun outcomeFor(seed: Long, wordIds: List<Int>): BotGhostOutcome {
        // Seeded, so the offer screen and the result screen — which never
        // speak to each other — cannot disagree about what she scored.
        val random = Random(seed)
        val wrongCount = sampleWrongCount(random, wordIds.size)
        val wrongIndices = wordIds.indices.shuffled(random).take(wrongCount).toSet()
        val correctness = wordIds.indices.map { it !in wrongIndices }

        val correctCount = correctness.count { it }
        val speedBonuses = (0 until correctCount).count { random.nextInt(100) < SPEED_BONUS_PERCENT }
        return BotGhostOutcome(
            correctness = correctness,
            totalScore = correctCount * GameConstants.POINTS_CORRECT +
                speedBonuses * GameConstants.SPEED_BONUS_POINTS,
            correctCount = correctCount,
            fastestCorrectMs = if (correctCount > 0) random.nextLong(1_200, 3_501) else null
        )
    }

    private fun sampleWrongCount(random: Random, wordCount: Int): Int {
        val roll = random.nextInt(100)
        val target = when {
            roll < 15 -> 0
            roll < 50 -> 1
            roll < 80 -> 2
            else -> 3
        }
        return target.coerceAtMost(wordCount)
    }
}

/**
 * Who a synthesized round is presented as playing against.
 *
 * Every fallback round used to be the same character at the same level,
 * which reads as exactly what it was — one bot, over and over — and told
 * the player there was nobody else here. A pool has to look like a pool
 * before anyone believes it is worth adding to.
 *
 * Names are built rather than listed: two word tables and a few shapes give
 * thousands of combinations from a few dozen lines, and none of them can
 * collide with a real person the way a list of real first names could. They
 * are deliberately gamer handles, not names — a stranger who turns out to
 * be "MaviTilki42" is a player, while one called "Ayşe" invites the
 * question of who that is.
 *
 * Derived from the run's own seed, like the score, so a round is entirely
 * reproducible from its id.
 */
object GhostPersonas {

    /**
     * Offsets the seed away from the score's, so a name and a result drawn
     * from the same run are not two readings of one dice roll.
     */
    private const val NAME_SALT = 0x9E37_79B9L
    private const val LEVEL_SALT = 0x7F4A_7C15L

    /** How far from the challenger's own level an opponent may be drawn. */
    private const val LEVEL_SPREAD = 6

    /**
     * How far above the challenger a synthesised opponent sits, by how much
     * of the round they got right.
     *
     * A stranger who sweeps ten out of ten and is level 3 does not read as a
     * player; it reads as a number that was made up separately from the score
     * beside it — which is exactly what it was. Skill and level move together
     * in a real account, so they move together here.
     *
     * Keyed on the FRACTION, not the count, so the bands still mean the same
     * thing if GhostRuns.RUN_WORD_COUNT ever changes.
     */
    private fun levelBonusFor(correctFraction: Float): Int = when {
        correctFraction >= 1f -> 30
        correctFraction >= 0.9f -> 10
        correctFraction >= 0.8f -> 5
        correctFraction >= 0.6f -> 0
        correctFraction >= 0.4f -> -4
        else -> -8
    }

    /**
     * The names a synthesised opponent can carry, in the device's language —
     * see [nicknamesFor]. A curated list, not a generator: an earlier
     * generator that crossed a handful of prefixes with a handful of roots
     * produced names that were all recognisably the same joke (kalemusta23,
     * boyaavci45, ...), which is exactly how a player works out that nobody
     * is really there. This list is real-looking usernames instead — no
     * shared theme, half title-cased and half not, exactly how actual
     * handles look next to each other. It cannot be translated word-for-word
     * either: an English speaker's "real usernames" lean on different clubs
     * and slang than a Turkish one's, so this is its own list in the same
     * spirit, not a translation of the one below it.
     */
    private val NICKNAMES_TR = listOf(
        "Memetcan", "ahmet734", "Fthylmz", "uykuluadam",
        "Kraduman", "fistikezmesi", "Kadir007", "ceyda8821",
        "Cananabaci", "yussuf", "Deliomer", "ruzgargibi",
        "Brkydmr", "burakreis", "Gozluklucocuk", "mustfcn",
        "Sagocu99", "yalnizkurt", "Ahmmet", "asabiadam",
        "Simitcay", "kaptanali", "Karabela", "gecebekcisi",
        "Demirhan", "hknkrks", "Yorgunsavasci", "zynpcetn",
        "Karakoc", "aysenur11", "Alican1903", "siyahinci",
        "Ssknr", "mertcn", "Iremsu", "bsgul",
        "Aleyna34", "gorkem543", "Cnsyksl", "bthnky",
        "Yusufinho", "polatalmdr", "Minikkus", "gamsizbaykus",
        "Mimarmerve", "muhendisbey", "Soforkemal", "issizgucsuz",
        "Mezunadam", "caykolik", "Kemalkaya", "gizemlikiz",
        "Kafkef88", "poyrazkarayel", "Ucanbalik", "isimsizkahraman",
        "Kacakyolcu", "delidolu", "Yalnizim", "firtinakemal",
        "Gocebe", "krmzblt", "Karadenizli", "vethasan",
        "Volkan00", "keloglan", "Gulyabani", "tosuncuk",
        "Karaeylem", "ogretmenim", "Hemsiremelisa", "avukatbey",
        "Ogrenciyiz", "tekbasina", "Krdsler", "sariyildiz",
        "Merve742", "farukeczanesi", "Cemal33", "komsukizi",
        "Bakkalamca", "uykucu", "Sessizkalan", "gokhantepe",
        "Ahemt98", "yanlizadam", "Herkezgitsin", "orjinall",
        "Suprizci", "yalnizdegil", "Mnyk", "fth123",
        "Qweasd", "tofask", "Passatci", "hondacivic",
        "Cbf150", "broadwayci", "Doganslx", "izmir35",
        "Bursa1616", "kordonboyu", "Kemalpasali", "mudanyali",
        "Adana01", "cikkofteci", "Caykasigi", "sekersiz",
        "Bolacili", "sarmisakli", "Uykumvar", "nebilimben",
        "Bosver", "falanfilan", "Ivirzivir", "baksanabana",
        "Belkide", "veterinerbey", "Yirmi8", "hekimsami",
        "98tayfa", "mormadenci", "Ustaeller", "kafkef",
        "Pesimist", "cimbom1905", "Fenerli1907", "bjk1903",
        "Ronaldo7", "ts61", "Messi10", "spinci",
        "Lufersesi", "amatorbalikci", "Sahteyem", "yagmurlu",
        "Lodos", "ametist", "Hsncn", "brk98",
        "Glsh", "mstyfa", "Ahmet8520", "cufcuf",
        "Wqewqe", "bumbum", "Laylaylom", "laylon",
        "Soley", "hicbiri", "Sonsoz", "oburki",
        "Isimsiz", "siyahgiyen", "Heryeryesil", "kdr",
        "Gokhn", "voldemort", "Padisah", "vezir",
        "Kayiboyu", "ineksaban"
    )

    /** The English-locale counterpart to [NICKNAMES_TR] — see its doc comment. */
    private val NICKNAMES_EN = listOf(
        "Jake.99", "chris_ny", "Tyler07", "ryan_ldn",
        "Josh_21", "mike.b", "Dan_uk22", "alex.h",
        "Sam_ldn", "jordan95", "Kyle.p", "connor_88",
        "Liam_09", "noah.k", "Ethan_21", "mason.t",
        "Lucas_07", "logan.c", "Owen_99", "caleb.r",
        "Blake_22", "dylan.m", "austin_07", "cole.b",
        "Hunter_21", "brody.k", "Jaxon_99", "ashton.r",
        "Tanner_07", "colt_88", "Gage.m", "wyatt_21",
        "Trevor_09", "shane.k", "Derek_99", "brett.c",
        "Chad_07", "kyle_88", "Brock.m", "garrett_21",
        "Gunners_fan", "Blues_til_death", "LFC_red", "United_devil",
        "Spurs_forever", "Toffees_blue", "Reds_army", "City_til_i_die",
        "Old_Joe", "Uncle_Dave", "Captain_Leo", "Boss_Man",
        "Big_Steve", "Old_Man_Sam", "Sarge_Tom", "Chief_Rick",
        "Doc_Harry", "Coach_Mike", "Preacher_John", "Deacon_Ray",
        "Sheriff_Bob", "Pastor_Lee", "Colonel_Dan", "Major_Tom",
        "Grandpa_Joe", "Old_Timer", "Rebel_Yell", "Lone_Wolf_77",
        "Night_Owl_22", "Storm_Rider", "Wild_Card_9", "Maverick_88",
        "Lucky_Charm", "Wise_Guy", "Fast_Eddie", "Smooth_Talker",
        "Silent_Bob", "Quiet_Storm", "Cool_Hand_Luke", "Iron_Mike",
        "daisy_dreams", "misty_blue", "rose_petal", "luna_light",
        "sunny_days", "star_gazer", "moon_child", "summer_breeze",
        "autumn_leaf", "winter_rose", "spring_bloom", "ocean_wave",
        "sky_blue_22", "cloud_nine", "rainbow_dust", "golden_hour",
        "honey_bee", "sugar_plum", "cherry_blossom", "lily_pad",
        "violet_sky", "ivy_league", "hazel_eyes", "amber_glow",
        "coral_reef", "pearl_white", "ruby_red", "jade_green",
        "opal_dream", "crystal_ball", "silver_lining", "copper_penny",
        "maple_leaf", "willow_tree", "cedar_wood", "birch_bark",
        "fern_gully", "clover_field", "poppy_field", "tulip_time",
        "sarah_gunners", "emma_reds", "olivia_blues", "ava_united",
        "mia_spurs", "zoe_toffees", "chloe_city", "grace_villa",
        "lily_ldn", "sophie_nyc", "ella_chi", "ruby_la",
        "maya_tex", "hannah_fl", "lucy_wa", "amelia_or",
        "isabella_co", "charlotte_az", "harper_nc", "evelyn_ga",
        "scarlett_va", "aria_pa", "nora_oh", "layla_mi",
        "brooklyn_rose", "chicago_belle", "austin_sunshine", "denver_dawn",
        "seattle_rain", "phoenix_heat", "boston_ivy", "miami_breeze",
        "dallas_star", "vegas_lights", "portland_moss", "nashville_note",
        "detroit_steel", "atlanta_peach", "tampa_bay_gal", "philly_pride",
        "sky_watcher", "coffee_lover", "book_worm_22", "movie_buff",
        "gamer_girl_99", "music_mind", "art_soul", "dream_chaser",
        "free_spirit", "wild_heart", "city_lights", "country_roads",
        "beach_bum_22", "mountain_high", "desert_rose", "forest_gump_9",
        "J.Smith", "K.Brown", "M.Davis", "T.Wilson",
        "R.Taylor", "S.Moore", "L.Clark", "A.Lewis",
        "B.Walker", "C.Hall", "D.Young", "E.King",
        "F.Wright", "G.Scott", "H.Green", "I.Baker",
        "N.Adams", "O.Nelson", "P.Carter", "Q.Mitchell",
        "V.Turner", "W.Parker", "X.Collins", "Z.Edwards"
    )

    /**
     * [language] is the device's own ("tr"/"en", see WordSeeder.currentLanguage)
     * — passed in rather than read here, since this is a plain domain model
     * with no Android Context of its own.
     */
    fun nicknameFor(language: String, seed: Long): String {
        val pool = if (language == "en") NICKNAMES_EN else NICKNAMES_TR
        return pool[Random(seed + NAME_SALT).nextInt(pool.size)]
    }

    /**
     * A level for an opponent who scored [correctCount] out of [wordCount].
     *
     * Anchored on the challenger rather than absolute, because these are
     * strangers with no identity to keep consistent — unlike the lobby bot,
     * who is one recognisable person across the whole game and therefore
     * fixed at BotRoomEngine.BOT_LEVEL. What moves it off that anchor is how
     * the round actually went: see [levelBonusFor].
     *
     * Note this makes the level suggestive of the score, which the offer
     * screen deliberately hides until the match is over. A perfect round
     * lands roughly thirty levels up and nothing else does, so a player who
     * pays attention can read the badge as a warning. That is the cost of
     * having the two numbers agree with each other, and it is the smaller of
     * the two tells.
     */
    fun levelFor(seed: Long, challengerLevel: Int, correctCount: Int, wordCount: Int): Int {
        val random = Random(seed + LEVEL_SALT)
        val spread = random.nextInt(-LEVEL_SPREAD, LEVEL_SPREAD + 1)
        val bonus = if (wordCount > 0) {
            levelBonusFor(correctCount.toFloat() / wordCount)
        } else {
            0
        }
        return (challengerLevel + bonus + spread).coerceIn(1, PlayerLevel.MAX_LEVEL)
    }
}
