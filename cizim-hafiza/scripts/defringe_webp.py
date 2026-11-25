#!/usr/bin/env python3
"""Removes the pale halo around cut-out artwork (webp with alpha).

Cut-outs made on a white matte keep light RGB in their half-transparent edge pixels; on a dark or coloured scene they read as a
white outline around the frame. The cure is to keep each edge pixel's transparency but give it the colour of the artwork right
behind it: the colours of the solid pixels are spread outward over the edge, a few pixels deep.

Usage: python3 scripts/defringe_webp.py [--check] file.webp ...   (--check only counts the light edge pixels)
"""
import sys
import numpy as np
from PIL import Image

SOLID = 250      # alpha from which a pixel counts as artwork
DEPTH = 6        # how many pixels outward the artwork colour is spread


def light_edge(rgba):
    a = rgba[..., 3]
    return (a > 0) & (a < SOLID) & (rgba[..., :3].astype(int).sum(-1) / 3 > 200)


def defringe(im):
    rgba = np.array(im.convert("RGBA"))
    rgb = rgba[..., :3].astype(np.float32)
    a = rgba[..., 3]
    known = a >= SOLID                      # pixels whose colour is trusted
    out = rgb.copy()
    h, w = a.shape
    for _ in range(DEPTH):
        pad_rgb = np.pad(out * known[..., None], ((1, 1), (1, 1), (0, 0)))
        pad_k = np.pad(known.astype(np.float32), 1)
        s = np.zeros_like(out); n = np.zeros((h, w), np.float32)
        for dy in (0, 1, 2):
            for dx in (0, 1, 2):
                if dy == 1 and dx == 1:
                    continue
                s += pad_rgb[dy:dy + h, dx:dx + w]
                n += pad_k[dy:dy + h, dx:dx + w]
        fill = (~known) & (a > 0) & (n > 0)
        out[fill] = s[fill] / n[fill][:, None]
        known = known | fill
    # only the edge pixels change; the solid artwork is left exactly as it was
    edge = (a > 0) & (a < SOLID)
    res = rgba.copy()
    res[..., :3][edge] = np.clip(out[edge], 0, 255).astype(np.uint8)
    return Image.fromarray(res, "RGBA")


if __name__ == "__main__":
    args = sys.argv[1:]
    check = "--check" in args
    for f in [x for x in args if not x.startswith("--")]:
        im = Image.open(f)
        before = int(light_edge(np.array(im.convert("RGBA"))).sum())
        if check:
            print(f"{f}: {before} light edge pixels"); continue
        new = defringe(im)
        after = int(light_edge(np.array(new)).sum())
        new.save(f, "WEBP", quality=95, alpha_quality=100, method=6)
        print(f"{f}: light edge pixels {before} -> {after}")
