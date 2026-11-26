#!/usr/bin/env python3
"""Turns the avatar artwork (round badges on a white square, as the image generator draws them) into the app's gallery files.

Each picture is cut to a circle a little inside the badge's own outline (the avatar frame already has a ring, so a second one would
double up), the white corners become transparent, and the result is a 512 x 512 webp: res/drawable-nodpi/avatar_gallery_<id>.webp.

Usage: python3 scripts/make_avatar_gallery.py <id>=<source.png>[@inset] ...   (inset: circle radius as a share of the width; art that fills the whole square, with no badge outline, takes 0.5)
Then list the id in domain/model/AvatarGallery.kt (the order there is the order on screen).
"""
import sys, os
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(__file__), "..", "app/src/main/res/drawable-nodpi")
SIZE = 512
INSET = 0.468   # circle radius as a share of the picture's width: just inside the badge's outline and rim


def make(src, dest, inset=INSET):
    im = Image.open(src).convert("RGB")
    w, h = im.size
    r = inset * w
    sheet = Image.new("L", (w * 2, h * 2), 0)            # a supersampled mask keeps the edge smooth
    ImageDraw.Draw(sheet).ellipse(((w / 2 - r) * 2, (h / 2 - r) * 2, (w / 2 + r) * 2, (h / 2 + r) * 2), fill=255)
    mask = sheet.resize((w, h), Image.LANCZOS)
    rgba = im.convert("RGBA"); rgba.putalpha(mask)
    box = (int(w / 2 - r), int(h / 2 - r), int(w / 2 + r), int(h / 2 + r))
    rgba = rgba.crop(box).resize((SIZE, SIZE), Image.LANCZOS)
    rgba.save(dest, "WEBP", quality=90, alpha_quality=100, method=6)


if __name__ == "__main__":
    for arg in sys.argv[1:]:
        name, src = arg.split("=", 1)
        inset = INSET
        if "@" in src:
            src, v = src.rsplit("@", 1); inset = float(v)
        dest = os.path.join(OUT, f"avatar_gallery_{name}.webp")
        make(src, dest, inset)
        print(name, os.path.getsize(dest) // 1024, "KB")
