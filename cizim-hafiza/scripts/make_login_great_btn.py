#!/usr/bin/env python3
"""Makes the painted "Great!" button that replaces the painted claim button once the daily login reward is taken
(it used to turn into a plain pill, which looked like a different dialog).

It takes each language's claim button, wipes the lettering (the gift stays), and letters the new word in the same cream-and-brown style.
Usage: python3 scripts/make_login_great_btn.py   -> res/drawable*/ls_btn_great.webp and hosting/oyna/img/ls_btn_great*.webp
"""
import os
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

HERE = os.path.dirname(__file__)
RES = os.path.join(HERE, "..", "app/src/main/res")
WEB = os.path.join(HERE, "..", "hosting/oyna/img")
FONT = os.path.join(RES, "font/baloo2_extrabold.ttf")
# (android folder, web suffix, word)
LANGS = [("drawable-nodpi", "_en", "Great!"), ("drawable-tr-nodpi", "", "Harika!"), ("drawable-pt-nodpi", "_pt", "Ótimo!"), ("drawable-es-nodpi", "_es", "¡Genial!")]
X0, X1 = 312, 838     # the lettering's span (the gift ends near 275); filled by interpolating the clean columns either side
Y0, Y1 = 22, 200


def wipe(im):
    # the button is a vertical gradient with a gloss band: repeat the clean column left of the lettering across it and blend
    # towards the (smoothed) column on its right so there is no seam; the raw right column carries the paint splash's colours,
    # which dragged into streaks when interpolated row by row
    a = np.array(im).astype(float)
    left = a[:, X0 - 6:X0, :].mean(axis=1)
    right = a[:, X1 + 2:X1 + 6, :].mean(axis=1)
    k = np.exp(-0.5 * (np.arange(-30, 31) / 14.0) ** 2); k /= k.sum()
    right = np.stack([np.convolve(np.pad(right[:, c], 30, mode="edge"), k, mode="valid") for c in range(4)], axis=1)
    for x in range(X0, X1):
        t = ((x - X0) / (X1 - X0)) ** 3
        a[Y0:Y1, x, :] = left[Y0:Y1] * (1 - t) + right[Y0:Y1] * t
    return Image.fromarray(a.clip(0, 255).astype("uint8"), "RGBA")


def letter(im, word):
    size = 112
    font = ImageFont.truetype(FONT, size)
    while font.getlength(word) > (X1 - X0) - 30:
        size -= 4; font = ImageFont.truetype(FONT, size)
    cx, cy = (X0 + X1) / 2 + 8, im.height / 2 - 4
    layer = Image.new("RGBA", im.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.text((cx, cy + 7), word, font=font, fill=(110, 50, 15, 150), anchor="mm", stroke_width=9, stroke_fill=(110, 50, 15, 150))
    layer = layer.filter(ImageFilter.GaussianBlur(2))
    d = ImageDraw.Draw(layer)
    d.text((cx, cy), word, font=font, fill=(255, 244, 222, 255), anchor="mm", stroke_width=9, stroke_fill=(122, 59, 18, 255))
    d.text((cx, cy), word, font=font, fill=(255, 244, 222, 255), anchor="mm")
    return Image.alpha_composite(im, layer)


for folder, sfx, word in LANGS:
    src = os.path.join(RES, folder, "ls_btn_claim.webp")
    im = Image.open(src).convert("RGBA")
    out = letter(wipe(im), word)
    for dest in (os.path.join(RES, folder, "ls_btn_great.webp"), os.path.join(WEB, f"ls_btn_great{sfx}.webp")):
        out.save(dest, "WEBP", quality=92, alpha_quality=100, method=6)
        print(dest, os.path.getsize(dest) // 1024, "KB")
