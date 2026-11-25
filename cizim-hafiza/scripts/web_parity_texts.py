# Which Android Turkish texts a screen uses are NOT in the web code (neither by key nor word for word, placeholders aside). Read-only; run from anywhere:
#   python3 -I cizim-hafiza/scripts/web_parity_texts.py [screen name]
# A hit is a CANDIDATE: texts the web leaves out on purpose (ads, Google sign-in, the what-is-new tour) are listed in docs/web-parity.md.
# For every Android Turkish text a screen uses: is it in the web code, by its key or word for word (placeholders aside)?
import re, os, sys, glob, html
import os
ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
RES = f"{ROOT}/app/src/main/res"
P = f"{ROOT}/app/src/main/java/com/sualtikasifi/cizimhafiza/presentation"
W = f"{ROOT}/hosting/oyna"

def norm(s):
    s = html.unescape(s).replace("\\'", "'").replace("\\n", " ").replace("’", "'")
    return re.sub(r"[^a-z0-9ğüşöçıİ ]+", " ", s.lower().replace("i̇", "i")).split()

def parse_xml(path):
    out = {}
    t = open(path, encoding="utf-8").read()
    for m in re.finditer(r'<string name="([^"]+)"[^>]*>(.*?)</string>', t, re.S): out[m.group(1)] = m.group(2)
    for m in re.finditer(r'<plurals name="([^"]+)">(.*?)</plurals>', t, re.S):
        items = re.findall(r"<item[^>]*>(.*?)</item>", m.group(2), re.S)
        out[m.group(1)] = " | ".join(items)
    return out
tr = parse_xml(f"{RES}/values-tr/strings.xml")

web = ""
for f in os.listdir(W):
    if f == "strings.json": continue
    if f.endswith((".js", ".html")) or f in ("runes.json", "store_strings.json", "result_strings.json", "chat_strings.json", "achievements.json", "pens.json", "frames.json", "entry_quotes.json"):
        web += open(f"{W}/{f}", errors="ignore").read() + "\n"
web_words = " " + " ".join(norm(web)) + " "
tokens = set(re.findall(r"[\"'`]([a-z][a-z0-9_]{3,})[\"'`]", web)) | set(re.findall(r"\b([a-z]+_[a-z0-9_]+)\b", web))
prefixes = set(re.findall(r"([a-z][a-z0-9_]*_)\$\{", web)) | set(re.findall(r"[\"']([a-z][a-z0-9_]*_)[\"']\s*\+", web))

def present(key, val):
    if key in tokens or any(key.startswith(p) for p in prefixes): return True
    frags = [f for f in re.split(r"%\d?\$?[sdf]|%%|\|", html.unescape(val).replace("\\n", "\n")) if len(re.sub(r"[^A-Za-zğüşöçıİĞÜŞÖÇ]", "", f)) >= 4]
    if not frags: return True   # nothing but placeholders / symbols
    hit = 0
    for f in frags:
        w = " ".join(norm(f))
        if w and (" " + w + " ") in web_words: hit += 1
    return hit >= max(1, int(len(frags) * 0.85 + 0.999))

SCREENS = {
 "Ana ekran": [f"{P}/mainmenu/*.kt"], "Kasalar": [f"{P}/chests/*.kt"], "Mağaza": [f"{P}/store/*.kt"], "Hesap": [f"{P}/account/*.kt"],
 "Arkadaşlar": [f"{P}/friends/*.kt"], "Lig": [f"{P}/league/*.kt"], "Ayarlar": [f"{P}/settings/*.kt"], "Başarımlar": [f"{P}/achievements/*.kt"],
 "Düellolar": [f"{P}/duel/*.kt"], "Çevrimiçi": [f"{P}/online/*.kt"], "Hızlı eşleş/sonuç": [f"{P}/quickmatch/*.kt"], "Oyun/çizim": [f"{P}/game/*.kt"],
 "Haritalar": [f"{P}/levelmap/*.kt", f"{P}/worldmap/*.kt", f"{P}/wordcount/*.kt"], "Eğitim": [f"{P}/tutorial/*.kt"], "Sorun bildir": [f"{P}/reportbug/*.kt"],
 "Tahmin (Bil Bakalım)": [f"{P}/guess/*.kt"], "Ortak": [f"{P}/common/*.kt"], "Rünler": [f"{P}/runes/*.kt"],
}
only = sys.argv[1] if len(sys.argv) > 1 else None
total_missing = 0
for name, globs in SCREENS.items():
    if only and only != name: continue
    src = ""
    for g in globs:
        for f in glob.glob(g): src += open(f, errors="ignore").read() + "\n"
    keys = sorted(set(re.findall(r"R\.(?:string|plurals)\.([a-z][a-z0-9_]*)", src)))
    miss = [k for k in keys if k in tr and not present(k, tr[k])]
    total_missing += len(miss)
    print(f"\n== {name}: {len(keys)} metin, webde olmayan {len(miss)}")
    for k in miss: print(f"   {k}: {html.unescape(tr[k]).replace(chr(92)+chr(39), chr(39)).replace(chr(10), ' ')[:110]}")
print("\nTOPLAM eksik:", total_missing)
