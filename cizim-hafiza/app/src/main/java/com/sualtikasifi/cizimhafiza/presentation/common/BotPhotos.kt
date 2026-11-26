package com.sualtikasifi.cizimhafiza.presentation.common

import com.sualtikasifi.cizimhafiza.domain.model.AvatarGallery

/**
 * Profile pictures for the league's filler players: each one wears a picture from the avatar gallery (AvatarGallery, the same
 * ones a player can pick), chosen to go with its name — [BY_NAME] is written out by hand: a wolf for "yalnizkurt", a sleepy
 * koala for "uykuluadam", an owl in a graduation cap for "ogretmenim", a lion for "kaptanali" and so on; names that say
 * nothing in particular are spread over the gallery by gender so that no picture piles up. Every other AI player (Hızlı Eşleş, recorded rounds) gets one too, see [GHOST_BY_NAME]. The web has the same table (hosting/oyna/bot_photos.json, node scripts/gen_web_botphotos.js).
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

    /**
     * The Hızlı Eşleş opponents' names (GhostPersonas, all four languages), lower-cased, with the avatar that suits each
     * (scripts/assign_ghost_avatars.py wrote the first draft: a theme word in the name, a girl's name, then a stable spread).
     */
    private val GHOST_BY_NAME: Map<String, String> = mapOf(
        "enesk" to "robot", "melihcan" to "wolf", "gokalp07" to "lion", "zeynepnaz" to "bunny_astronaut",
        "tarikk" to "owl", "sevgiliyim" to "bunny_astronaut", "bulentbey" to "dino", "aycan_m" to "panther",
        "muratcan55" to "panda", "ferhatt" to "octopus", "duygu_k" to "unicorn", "salihk1" to "bear",
        "emirhann" to "dino", "beratcan" to "fox", "kubrag" to "unicorn", "ozanbey" to "bear",
        "aslihan99" to "bunny_astronaut", "yigitcan" to "panther", "ercank" to "frog", "tuncerbey" to "fox",
        "birkank" to "tiger", "kayahan_" to "elephant", "ediz34" to "fox", "melisnur" to "panda",
        "hakanaltin" to "wolf", "sumeyye_k" to "koala", "canerk35" to "robot", "ilayda_r" to "bunny_astronaut",
        "yakupp" to "robot", "selimcan" to "octopus", "meltemx" to "puppy_girl", "huseyinkoc" to "panda",
        "gulcan55" to "koala", "ridvank" to "owl", "ipeknur" to "cat_artist", "tolgaa" to "bear",
        "berkant" to "panther", "ferideh" to "koala", "cansu_yz" to "cat_artist", "mucahitt" to "bear",
        "idilnaz" to "panda", "atillaa" to "panda", "semihk" to "fox", "zeliha_t" to "bunny_astronaut",
        "onurcan" to "robot", "basakk" to "puppy_girl", "irfanbey" to "tiger", "necla_h" to "bunny_astronaut",
        "turgutt" to "owl", "ozlemk" to "puppy_girl", "kenanaydin" to "elephant", "ebruc" to "cat_artist",
        "savasbey" to "frog", "yeliz_d" to "puppy_girl", "erolk" to "dino", "sibelnur" to "unicorn",
        "kadircan" to "penguin", "nese_y" to "koala", "mahiryilmaz" to "wolf", "damla_ceyy" to "puppy_girl",
        "jake.99" to "octopus", "chris_ny" to "tiger", "tyler07" to "wolf", "ryan_ldn" to "fox",
        "josh_21" to "panda", "mike.b" to "robot", "dan_uk22" to "owl", "alex.h" to "fox",
        "sam_ldn" to "owl", "jordan95" to "lion", "kyle.p" to "penguin", "connor_88" to "fox",
        "liam_09" to "lion", "noah.k" to "owl", "ethan_21" to "frog", "mason.t" to "panda",
        "lucas_07" to "panda", "logan.c" to "owl", "owen_99" to "owl", "caleb.r" to "wolf",
        "blake_22" to "bear", "dylan.m" to "robot", "austin_07" to "octopus", "cole.b" to "tiger",
        "hunter_21" to "owl", "brody.k" to "tiger", "jaxon_99" to "bear", "ashton.r" to "octopus",
        "tanner_07" to "tiger", "colt_88" to "tiger", "gage.m" to "penguin", "wyatt_21" to "fox",
        "trevor_09" to "tiger", "shane.k" to "panda", "derek_99" to "tiger", "brett.c" to "elephant",
        "chad_07" to "tiger", "kyle_88" to "panther", "brock.m" to "tiger", "garrett_21" to "wolf",
        "gunners_fan" to "lion", "blues_til_death" to "penguin", "lfc_red" to "lion", "united_devil" to "lion",
        "spurs_forever" to "lion", "toffees_blue" to "lion", "reds_army" to "lion", "city_til_i_die" to "robot",
        "old_joe" to "bear", "uncle_dave" to "bear", "captain_leo" to "lion", "boss_man" to "lion",
        "big_steve" to "bear", "old_man_sam" to "bear", "sarge_tom" to "lion", "chief_rick" to "lion",
        "doc_harry" to "owl", "coach_mike" to "owl", "preacher_john" to "owl", "deacon_ray" to "owl",
        "sheriff_bob" to "lion", "pastor_lee" to "owl", "colonel_dan" to "lion", "major_tom" to "lion",
        "grandpa_joe" to "bear", "old_timer" to "bear", "rebel_yell" to "tiger", "lone_wolf_77" to "wolf",
        "night_owl_22" to "owl", "storm_rider" to "tiger", "wild_card_9" to "tiger", "maverick_88" to "tiger",
        "lucky_charm" to "tiger", "wise_guy" to "owl", "fast_eddie" to "tiger", "smooth_talker" to "tiger",
        "silent_bob" to "panther", "quiet_storm" to "panther", "cool_hand_luke" to "tiger", "iron_mike" to "lion",
        "daisy_dreams" to "unicorn", "misty_blue" to "bunny_astronaut", "rose_petal" to "puppy_girl", "luna_light" to "unicorn",
        "sunny_days" to "unicorn", "star_gazer" to "unicorn", "moon_child" to "unicorn", "summer_breeze" to "puppy_girl",
        "autumn_leaf" to "panda", "winter_rose" to "penguin", "spring_bloom" to "bunny_astronaut", "ocean_wave" to "penguin",
        "sky_blue_22" to "koala", "cloud_nine" to "koala", "rainbow_dust" to "unicorn", "golden_hour" to "panda",
        "honey_bee" to "unicorn", "sugar_plum" to "unicorn", "cherry_blossom" to "unicorn", "lily_pad" to "frog",
        "violet_sky" to "cat_artist", "ivy_league" to "panda", "hazel_eyes" to "unicorn", "amber_glow" to "koala",
        "coral_reef" to "puppy_girl", "pearl_white" to "bunny_astronaut", "ruby_red" to "koala", "jade_green" to "panda",
        "opal_dream" to "koala", "crystal_ball" to "octopus", "silver_lining" to "bunny_astronaut", "copper_penny" to "koala",
        "maple_leaf" to "unicorn", "willow_tree" to "bunny_astronaut", "cedar_wood" to "bunny_astronaut", "birch_bark" to "bunny_astronaut",
        "fern_gully" to "bunny_astronaut", "clover_field" to "puppy_girl", "poppy_field" to "koala", "tulip_time" to "koala",
        "sarah_gunners" to "bunny_astronaut", "emma_reds" to "koala", "olivia_blues" to "puppy_girl", "ava_united" to "unicorn",
        "mia_spurs" to "puppy_girl", "zoe_toffees" to "bunny_astronaut", "chloe_city" to "puppy_girl", "grace_villa" to "unicorn",
        "lily_ldn" to "unicorn", "sophie_nyc" to "puppy_girl", "ella_chi" to "bunny_astronaut", "ruby_la" to "bunny_astronaut",
        "maya_tex" to "unicorn", "hannah_fl" to "bunny_astronaut", "lucy_wa" to "koala", "amelia_or" to "panda",
        "isabella_co" to "unicorn", "charlotte_az" to "panda", "harper_nc" to "puppy_girl", "evelyn_ga" to "puppy_girl",
        "scarlett_va" to "unicorn", "aria_pa" to "koala", "nora_oh" to "bunny_astronaut", "layla_mi" to "unicorn",
        "brooklyn_rose" to "puppy_girl", "chicago_belle" to "cat_artist", "austin_sunshine" to "cat_artist", "denver_dawn" to "puppy_girl",
        "seattle_rain" to "panda", "phoenix_heat" to "dino", "boston_ivy" to "bunny_astronaut", "miami_breeze" to "frog",
        "dallas_star" to "wolf", "vegas_lights" to "tiger", "portland_moss" to "penguin", "nashville_note" to "octopus",
        "detroit_steel" to "fox", "atlanta_peach" to "cat_artist", "tampa_bay_gal" to "wolf", "philly_pride" to "dino",
        "sky_watcher" to "bunny_astronaut", "coffee_lover" to "koala", "book_worm_22" to "owl", "movie_buff" to "octopus",
        "gamer_girl_99" to "bunny_astronaut", "music_mind" to "octopus", "art_soul" to "octopus", "dream_chaser" to "unicorn",
        "free_spirit" to "koala", "wild_heart" to "bunny_astronaut", "city_lights" to "lion", "country_roads" to "panda",
        "beach_bum_22" to "penguin", "mountain_high" to "bear", "desert_rose" to "unicorn", "forest_gump_9" to "frog",
        "j.smith" to "panther", "k.brown" to "lion", "m.davis" to "wolf", "t.wilson" to "penguin",
        "r.taylor" to "tiger", "s.moore" to "fox", "l.clark" to "bear", "a.lewis" to "octopus",
        "b.walker" to "lion", "c.hall" to "octopus", "d.young" to "wolf", "e.king" to "lion",
        "f.wright" to "wolf", "g.scott" to "dino", "h.green" to "robot", "i.baker" to "bear",
        "n.adams" to "frog", "o.nelson" to "dino", "p.carter" to "frog", "q.mitchell" to "wolf",
        "v.turner" to "robot", "w.parker" to "fox", "x.collins" to "panda", "z.edwards" to "panda",
        "joaopedro10" to "lion", "lucas_rj" to "dino", "gabigol_fan" to "koala", "mateus.sp" to "fox",
        "rafinha77" to "tiger", "pedrinho" to "panther", "vitor_bh" to "penguin", "thiagao" to "dino",
        "guizinho" to "bear", "caiozera" to "fox", "felipe_poa" to "wolf", "brunao_22" to "tiger",
        "leozin" to "fox", "diegao" to "lion", "arthur.cwb" to "octopus", "davizinho" to "panda",
        "mengao1981" to "lion", "timao_eterno" to "lion", "verdao1914" to "lion", "tricolor_sp" to "lion",
        "vasco_ate_morrer" to "lion", "galo_doido" to "lion", "gremista93" to "lion", "colorado_poa" to "lion",
        "cruzeiro_cabuloso" to "lion", "fogao_rj" to "lion", "santista10" to "lion", "bahea_bbmp" to "lion",
        "juliana.s" to "cat_artist", "ana_clara" to "puppy_girl", "beatriz_rj" to "puppy_girl", "mari_sp" to "koala",
        "larissa22" to "puppy_girl", "isa.bela" to "tiger", "camila_bh" to "cat_artist", "duda_m" to "cat_artist",
        "leticia.f" to "cat_artist", "gabi_poa" to "cat_artist", "fernandinha" to "panda", "bia_lima" to "unicorn",
        "carol_cwb" to "cat_artist", "lu_santos" to "unicorn", "rafaela_99" to "panda", "nanda_ssa" to "koala",
        "manu.oliveira" to "tiger", "jessica_rec" to "koala", "vivi_floripa" to "unicorn", "taty_m" to "cat_artist",
        "seu_ze" to "bear", "tio_paulo" to "bear", "dona_cida" to "bear", "vo_maria" to "bear",
        "professor_rui" to "owl", "doutor_carlos" to "owl", "mestre_juca" to "owl", "sargento_lima" to "lion",
        "capitao_nunes" to "lion", "padeiro_ze" to "bear", "motoboy_sp" to "panther", "taxista_rj" to "bear",
        "carioca_da_gema" to "lion", "paulistano" to "fox", "mineirinho_uai" to "robot", "gaucho_tche" to "penguin",
        "baiano_arretado" to "fox", "nordestino_pe" to "wolf", "capixaba27" to "lion", "manauara" to "penguin",
        "curitibano41" to "robot", "goiano_62" to "frog", "paraense91" to "dino", "cearense85" to "panda",
        "sonhadora" to "unicorn", "lobo_solitario" to "wolf", "cafe_com_leite" to "koala", "pao_de_queijo" to "elephant",
        "brigadeiro" to "elephant", "acai_na_tigela" to "elephant", "coxinha_sp" to "elephant", "guarana" to "elephant",
        "noite_estrelada" to "koala", "sol_de_verao" to "robot", "chuva_fina" to "bear", "lua_cheia" to "unicorn",
        "praia_e_sol" to "penguin", "pe_na_areia" to "penguin", "bicho_do_mato" to "dino", "beija_flor" to "bunny_astronaut",
        "sabia_laranjeira" to "bunny_astronaut", "tucano_azul" to "bunny_astronaut", "jabuti_lento" to "frog", "capivara_zen" to "dino",
        "gamer_br" to "robot", "noob_master" to "robot", "kkkkkk" to "fox", "ze_ninguem" to "fox",
        "anonimo123" to "fox", "fulano" to "fox", "ciclano_99" to "fox", "beltrano" to "fox",
        "rodrigo.m" to "robot", "andre_f" to "lion", "marcelo77" to "wolf", "renato_bh" to "fox",
        "fabio.sp" to "dino", "gustavo_rj" to "elephant", "henrique22" to "panther", "igor.lima" to "lion",
        "neymar_jr_fa" to "lion", "r9_fenomeno" to "lion", "ronaldinho_10" to "lion", "romario11" to "lion",
        "juanpa_10" to "fox", "santi.mx" to "tiger", "mateo_bsas" to "panther", "diego_cdmx" to "panda",
        "nico.rios" to "wolf", "seba_22" to "wolf", "tomi_cba" to "panda", "andres.bog" to "dino",
        "pancho99" to "lion", "rodri_gdl" to "panda", "lucho.lima" to "bear", "emi_mty" to "panda",
        "fede.ok" to "penguin", "benja_cl" to "dino", "cami_lo" to "octopus", "joaco07" to "fox",
        "boca_juniors_12" to "lion", "river_plate_ok" to "lion", "chivas_campeon" to "lion", "america_aguilas" to "lion",
        "pumas_unam" to "lion", "tigres_uanl" to "lion", "colo_colo_1925" to "lion", "nacional_atl" to "lion",
        "millos_azul" to "lion", "cruzazul_9" to "lion", "penarol_1891" to "lion", "racing_avellaneda" to "lion",
        "valentina.r" to "cat_artist", "sofi_mx" to "unicorn", "luciana.b" to "koala", "martina22" to "panda",
        "vale_cdmx" to "cat_artist", "camila.ch" to "cat_artist", "dani_lima" to "panda", "mariana_gdl" to "panda",
        "fer.ruiz" to "bear", "ximena_mx" to "koala", "paula_bog" to "cat_artist", "renata.07" to "unicorn",
        "abril_ba" to "cat_artist", "lupita_99" to "panda", "jime.sol" to "koala", "antonella_ar" to "koala",
        "majo_cr" to "puppy_girl", "gaby.vzla" to "panda", "romi_uy" to "koala", "don_ramon" to "bear",
        "tio_pepe" to "bear", "dona_lucha" to "bear", "abuela_rosa" to "bear", "profe_jorge" to "owl",
        "doctor_mendez" to "owl", "maestro_chuy" to "owl", "capitan_ortiz" to "lion", "ingeniero_raul" to "owl",
        "panadero_tito" to "bear", "taxista_df" to "bear", "chofer_luis" to "bear", "chilango_de_corazon" to "tiger",
        "regio_mty" to "elephant", "tapatio_33" to "bear", "porteno_bsas" to "lion", "rolo_bogota" to "elephant",
        "paisa_mde" to "octopus", "limeno_ok" to "robot", "santiaguino" to "tiger", "cordobes_fernet" to "panda",
        "rosarino" to "puppy_girl", "tico_pura_vida" to "robot", "charrua_uy" to "owl", "sonadora" to "unicorn",
        "cafe_de_olla" to "koala", "taquero_fiel" to "bear", "mate_amargo" to "elephant", "empanada_saltena" to "elephant",
        "arepa_reina" to "elephant", "alfajor" to "elephant", "noche_estrellada" to "koala", "sol_de_verano" to "panda",
        "lluvia_fina" to "frog", "luna_llena" to "unicorn", "playa_y_sol" to "penguin", "pies_en_la_arena" to "penguin",
        "colibri_azul" to "bunny_astronaut", "quetzal" to "bunny_astronaut", "condor_andino" to "dino", "tucan_feliz" to "bunny_astronaut",
        "ajolote_rosa" to "frog", "carpincho_zen" to "dino", "gamer_latam" to "robot", "jajajaja" to "octopus",
        "fulanito" to "fox", "mengano" to "fox", "zutano_99" to "fox", "nadie_sabe" to "fox",
        "alejandro.m" to "dino", "carlos_f" to "elephant", "ricardo77" to "tiger", "javi_mx" to "octopus",
        "pablo.ar" to "bear", "gonza_uy" to "tiger", "hernan22" to "robot", "ivan.gt" to "lion",
        "messi10_fan" to "lion", "el_chicharito" to "lion", "james_10" to "lion", "chilavert_1" to "lion"
    )

    /** The gallery avatar for [name], any AI player's: the hand-picked one if the name is known, else a stable pick by the name itself. */
    fun avatarIdFor(name: String): String {
        val key = name.lowercase()
        return BY_NAME[key] ?: GHOST_BY_NAME[key]
            ?: AvatarGallery.entries[(key.fold(7) { acc, c -> acc * 31 + c.code } and Int.MAX_VALUE) % AvatarGallery.entries.size].id
    }

    /** Whether [name] has a hand-assigned avatar (a league bot or a Hızlı Eşleş opponent), not the hash fallback. */
    fun isPicked(name: String): Boolean = name.lowercase().let { it in BY_NAME || it in GHOST_BY_NAME }

    /** The gallery picture for AI player [name]: never null, and the same one every time. */
    fun drawableFor(name: String): Int = AvatarGallery.find(avatarIdFor(name))?.res ?: AvatarGallery.entries.first().res

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
