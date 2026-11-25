# Which pictures the Android screens use that the web does not have (a file missing in hosting/oyna/img). Read-only; run from anywhere:
#   python3 -I cizim-hafiza/scripts/web_parity_images.py
import os, re, glob
ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
W = f"{ROOT}/hosting/oyna/img"; R = f"{ROOT}/app/src/main/res"; P = f"{ROOT}/app/src/main/java/com/sualtikasifi/cizimhafiza/presentation"
web = {os.path.splitext(f)[0] for f in os.listdir(W)}
used = {}
for f in glob.glob(P + "/**/*.kt", recursive=True):
    s = open(f, errors="ignore").read()
    for m in set(re.findall(r"R\.drawable\.([a-z][a-z0-9_]*)", s)): used.setdefault(m, set()).add(f.split("/presentation/")[1].split("/")[0])
res = {os.path.splitext(f)[0] for d in glob.glob(R + "/drawable*") for f in os.listdir(d)}
miss = sorted(k for k in used if k in res and k not in web and not k.startswith("bot_photo_"))
print(f"{len(used)} pictures used by Android screens; {len(miss)} have no file on the web (bot photos left out):")
by = {}
for k in miss:
    for s in used[k]: by.setdefault(s, []).append(k)
for s, ks in sorted(by.items()): print(f"  {s}: {', '.join(ks)}")
