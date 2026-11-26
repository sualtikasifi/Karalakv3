#!/usr/bin/env python3
"""Picks a gallery avatar for every Hızlı Eşleş (ghost) opponent name, in all four languages, and prints the Kotlin table that goes
into BotPhotos.GHOST_BY_NAME (names are lower-cased there). Rules, in order: a theme word in the name (a wolf for "lobo_solitario", an owl
for a teacher or doctor, a lion for a captain or a football team that has one…), then a girl's first name or a feminine word picks a
girl's avatar, then everything else is spread over the male/neutral pool by a stable hash, so no picture piles up.
The result is written out by hand into the Kotlin file once; names the table does not know fall back to a stable hash there.
Usage: python3 scripts/assign_ghost_avatars.py > /tmp/ghost_table.txt
"""
import json, os, re, zlib

HERE = os.path.dirname(__file__)
names = json.load(open(os.path.join(HERE, "..", "hosting/oyna/ghost_names.json")))
league = set(json.load(open(os.path.join(HERE, "..", "hosting/oyna/league_names.json"))))

# theme words (substrings of the lower-cased name), first match wins
THEMES = [
    ("wolf", "lone_wolf lobo_solitario lobo yalnizkurt"),
    ("owl", "night_owl professor profe doutor doctor doc_ mestre maestro ogretmen hekim veteriner avukat ingeniero muhendis coach_ preacher pastor deacon book_worm wise_guy"),
    ("koala", "uyku sleepy cloud_nine lazy coffee_lover cafe_com_leite cafe_de_olla noite_estrelada noche_estrellada"),
    ("lion", "captain capitan capitao kaptan major_ colonel sargento sarge_ sheriff chief_ boss_ iron_mike king leo_ gunners_fan lfc_red reds_army united_devil spurs_forever toffees_blue mengao timao verdao tricolor vasco galo fogao santista bahea cruzeiro colorado gremista boca_juniors river_plate chivas america_aguilas pumas tigres colo_colo nacional_atl millos cruzazul penarol racing cimbom fenerli bjk ts61 ronaldo messi neymar ronaldinho romario r9_ el_chicharito james_10 chilavert"),
    ("tiger", "maverick cool_hand fast_eddie smooth_talker rebel wild_card storm_rider lucky_charm trevor derek brock chad tanner brody colt"),
    ("panther", "silent_bob quiet_storm siyah gece night_ shadow karabela karakoc motoboy"),
    ("bear", "uncle_dave old_joe old_man old_timer grandpa big_steve tio_ tio_pepe seu_ze dona_ abuela don_ vo_maria padeiro panadero taquero bakkal mormadenci ustaeller baker taxista chofer taxi sargent"),
    ("frog", "lily_pad forest_gump jabuti ajolote gulyabani voldemort lufersesi"),
    ("robot", "gamer_br gamer_latam noob_master qweasd wqewqe nerd"),
    ("penguin", "winter_rose pe_na_areia pies_en_la_arena playa_ praia_ beach_bum ocean_wave"),
    ("dino", "capivara carpincho bicho_do_mato"),
    ("octopus", "art_soul music_mind movie_buff nashville_note crystal_ball"),
    ("elephant", "mate_amargo empanada alfajor arepa pao_de_queijo brigadeiro acai coxinha guarana"),
    ("fox", "fulano ciclano beltrano fulanito mengano zutano nadie_sabe ze_ninguem anonimo wise_ sahteyem krmzblt vezir keloglan"),
    ("unicorn", "rainbow_dust sugar_plum cherry_blossom honey_bee sunny_days dream_chaser star_gazer moon_child luna_light luna_llena lua_cheia sonhadora sonadora"),
    ("bunny_astronaut", "sky_watcher cloud_ spring_bloom colibri beija_flor quetzal tucano tucan sabia"),
]

# a girl's given name or a clearly feminine word inside the name
FEMALE = set("""ceyda cana aysen aysenur zynp gizem merve aleyna irem bsgul melis melisnur ilayda ipek damla zeynep duygu kubra aslihan sumeyye meltem
gulcan feride cansu idil zeliha basak necla ozlem ebru yeliz sibel nese sevgili hemsire komsukizi soley laylay layla ametist yagmur glsh karaeylem
juliana ana_clara beatriz mari_ larissa isa_bela camila duda leticia gabi fernandinha bia_ carol lu_ rafaela nanda manu_ jessica vivi taty
valentina sofi isabella luciana martina vale_ dani_ mariana fer_ ximena paula renata abril lupita jime antonella majo gaby romi
daisy misty rose_ rosa luna sunny summer autumn winter_rose spring ocean_wave sky_blue cloud rainbow golden honey sugar cherry lily violet ivy
hazel amber coral pearl ruby jade opal crystal silver copper maple willow cedar birch fern clover poppy tulip
sarah emma olivia ava_ mia_ zoe chloe grace sophie ella_ maya hannah lucy amelia charlotte harper evelyn scarlett aria nora brooklyn_rose chicago_belle
sunshine dawn rain_ heat_ peach belle gal_ pride_ girl desert_rose country_roads free_spirit wild_heart dream coffee_lover music_mind art_soul""".split())

FEMALE_POOL = ["puppy_girl", "cat_artist", "unicorn", "bunny_astronaut", "koala", "panda"]
MALE_POOL = ["lion", "tiger", "wolf", "panther", "bear", "fox", "robot", "owl", "penguin", "dino", "elephant", "octopus", "frog", "panda"]


def pick(name):
    n = name.lower()
    for aid, words in THEMES:
        if any(w in n for w in words.split()):
            return aid
    h = zlib.crc32(n.encode()) & 0x7fffffff
    if any(tok in n for tok in FEMALE):
        return FEMALE_POOL[h % len(FEMALE_POOL)]
    return MALE_POOL[h % len(MALE_POOL)]


if __name__ == "__main__":
    seen, rows = set(), []
    for lang in ("tr", "en", "pt", "es"):
        for nm in names[lang]:
            k = nm.lower()
            if k in seen or k in {x.lower() for x in league}:
                continue
            seen.add(k); rows.append((k, pick(k)))
    line, out = [], []
    for k, a in rows:
        line.append(f'"{k}" to "{a}"')
        if len(line) == 4: out.append("        " + ", ".join(line) + ","); line = []
    if line: out.append("        " + ", ".join(line) + ",")
    print("\n".join(out).rstrip(","))
    import sys, collections
    print(len(rows), "names", dict(collections.Counter(a for _, a in rows)), file=sys.stderr)
