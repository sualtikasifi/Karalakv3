package com.sualtikasifi.cizimhafiza.presentation.common

import com.sualtikasifi.cizimhafiza.domain.model.AvatarGallery

/**
 * Profile pictures for the league's filler players: each one wears a picture from the avatar gallery (AvatarGallery, the same
 * ones a player can pick), chosen to go with its name — [BY_NAME] is written out by hand: a wolf for "yalnizkurt", a sleepy
 * koala for "uykuluadam", an owl in a graduation cap for "ogretmenim", a lion for "kaptanali" and so on; names that say
 * nothing in particular are spread over the gallery by gender so that no picture piles up. Names that are not league bots get
 * null and keep the generated avatar. The web has the same table (hosting/oyna/bot_photos.json, node scripts/gen_web_botphotos.js).
 */
object BotPhotos {
    private val BY_NAME: Map<String, String> = mapOf(
        "memetcan" to "lion", "ahmet734" to "tiger", "fthylmz" to "wolf", "uykuluadam" to "koala",
        "kraduman" to "lion", "fistikezmesi" to "panther", "kadir007" to "bear", "ceyda8821" to "puppy_girl",
        "cananabaci" to "cat_artist", "yussuf" to "fox", "deliomer" to "tiger", "ruzgargibi" to "robot",
        "brkydmr" to "owl", "burakreis" to "penguin", "gozluklucocuk" to "owl", "mustfcn" to "dino",
        "sagocu99" to "elephant", "yalnizkurt" to "wolf", "ahmmet" to "octopus", "asabiadam" to "frog",
        "simitcay" to "panda", "kaptanali" to "lion", "karabela" to "panther", "gecebekcisi" to "owl",
        "demirhan" to "lion", "hknkrks" to "bunny_astronaut", "yorgunsavasci" to "koala", "zynpcetn" to "unicorn",
        "karakoc" to "panther", "aysenur11" to "bunny_astronaut", "alican1903" to "lion", "siyahinci" to "panther",
        "ssknr" to "robot", "mertcn" to "tiger", "iremsu" to "koala", "bsgul" to "panda",
        "aleyna34" to "puppy_girl", "gorkem543" to "dino", "cnsyksl" to "wolf", "bthnky" to "panther",
        "yusufinho" to "bear", "polatalmdr" to "fox", "minikkus" to "owl", "gamsizbaykus" to "owl",
        "mimarmerve" to "cat_artist", "muhendisbey" to "robot", "soforkemal" to "bear", "issizgucsuz" to "koala",
        "mezunadam" to "owl", "caykolik" to "panda", "kemalkaya" to "elephant", "gizemlikiz" to "cat_artist",
        "kafkef88" to "panda", "poyrazkarayel" to "wolf", "ucanbalik" to "penguin", "isimsizkahraman" to "panther",
        "kacakyolcu" to "panther", "delidolu" to "tiger", "yalnizim" to "wolf", "firtinakemal" to "tiger",
        "gocebe" to "wolf", "krmzblt" to "fox", "karadenizli" to "wolf", "vethasan" to "fox",
        "volkan00" to "tiger", "keloglan" to "fox", "gulyabani" to "frog", "tosuncuk" to "dino",
        "karaeylem" to "unicorn", "ogretmenim" to "owl", "hemsiremelisa" to "puppy_girl", "avukatbey" to "owl",
        "ogrenciyiz" to "owl", "tekbasina" to "wolf", "krdsler" to "panther", "sariyildiz" to "elephant",
        "merve742" to "bunny_astronaut", "farukeczanesi" to "owl", "cemal33" to "robot", "komsukizi" to "koala",
        "bakkalamca" to "bear", "uykucu" to "koala", "sessizkalan" to "wolf", "gokhantepe" to "elephant",
        "ahemt98" to "owl", "yanlizadam" to "wolf", "herkezgitsin" to "wolf", "orjinall" to "dino",
        "suprizci" to "fox", "yalnizdegil" to "wolf", "mnyk" to "robot", "fth123" to "robot",
        "qweasd" to "robot", "tofask" to "robot", "passatci" to "tiger", "hondacivic" to "tiger",
        "cbf150" to "tiger", "broadwayci" to "tiger", "doganslx" to "tiger", "izmir35" to "penguin",
        "bursa1616" to "dino", "kordonboyu" to "elephant", "kemalpasali" to "octopus", "mudanyali" to "frog",
        "adana01" to "bunny_astronaut", "cikkofteci" to "bear", "caykasigi" to "panda", "sekersiz" to "panda",
        "bolacili" to "bear", "sarmisakli" to "bear", "uykumvar" to "koala", "nebilimben" to "panda",
        "bosver" to "panda", "falanfilan" to "fox", "ivirzivir" to "frog", "baksanabana" to "octopus",
        "belkide" to "octopus", "veterinerbey" to "owl", "yirmi8" to "dino", "hekimsami" to "owl",
        "98tayfa" to "penguin", "mormadenci" to "bear", "ustaeller" to "bear", "kafkef" to "panda",
        "pesimist" to "panda", "cimbom1905" to "lion", "fenerli1907" to "fox", "bjk1903" to "panther",
        "ronaldo7" to "lion", "ts61" to "penguin", "messi10" to "tiger", "spinci" to "robot",
        "lufersesi" to "frog", "amatorbalikci" to "penguin", "sahteyem" to "fox", "yagmurlu" to "cat_artist",
        "lodos" to "octopus", "ametist" to "unicorn", "hsncn" to "dino", "brk98" to "lion",
        "glsh" to "panda", "mstyfa" to "tiger", "ahmet8520" to "wolf", "cufcuf" to "frog",
        "wqewqe" to "robot", "bumbum" to "panther", "laylaylom" to "unicorn", "laylon" to "unicorn",
        "soley" to "bunny_astronaut", "hicbiri" to "octopus", "sonsoz" to "octopus", "oburki" to "octopus",
        "isimsiz" to "octopus", "siyahgiyen" to "panther", "heryeryesil" to "frog", "kdr" to "robot",
        "gokhn" to "elephant", "voldemort" to "frog", "padisah" to "lion", "vezir" to "fox",
        "kayiboyu" to "wolf", "ineksaban" to "bear"
    )

    /** The gallery picture for league bot [name], or null when it is not one of them. */
    fun drawableFor(name: String): Int? = AvatarGallery.find(BY_NAME[name])?.res

    /** Every picture there is, for the reviewer's picker (developer panel). */
    val all: List<Int> get() = AvatarGallery.entries.map { it.res }

    /** The stable key a chosen picture is stored under (its resource name). */
    fun keyOf(context: android.content.Context, res: Int): String = context.resources.getResourceEntryName(res)

    /** The picture stored under [key], or null when there is none (a stale key, or null). */
    fun drawableForKey(context: android.content.Context, key: String?): Int? =
        if (key.isNullOrBlank()) null else all.firstOrNull { keyOf(context, it) == key }
}

/** The face of a recorded round's author: the picture the reviewer chose for it, or else the one its name picks. */
@androidx.compose.runtime.Composable
fun opponentPhoto(photoKey: String?, nickname: String): AvatarPhoto {
    val context = androidx.compose.ui.platform.LocalContext.current
    val chosen = androidx.compose.runtime.remember(photoKey) { BotPhotos.drawableForKey(context, photoKey) }
    return if (chosen != null) AvatarPhoto.Res(chosen) else AvatarPhoto.Persona(nickname)
}
