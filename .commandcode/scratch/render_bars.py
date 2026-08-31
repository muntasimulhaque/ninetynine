# -*- coding: utf-8 -*-
"""Render the three bottom-bar variants (A quiet / B floating shadow /
C floating flat) in light + dark, from the app's literal palette, using the
same superellipse n=4 geometry as SquircleShape.kt. Outputs PNGs for review."""

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
OUT_DIR = Path(__file__).resolve().parent

# ------------------------------------------------------------ palette (Color.kt verbatim)
L = dict(
    paper=(0xFA, 0xF5, 0xEA), surface=(0xFF, 0xFC, 0xF4),
    container=(0xF6, 0xF0, 0xE2), high=(0xF0, 0xEA, 0xDA),
    on=(0x21, 0x1C, 0x12), variant=(0x6B, 0x63, 0x53),
    outline=(0x8A, 0x81, 0x70), ov=(0xD8, 0xCF, 0xBB),
    primary=(0x17, 0x62, 0x4E), secondary=(0x85, 0x62, 0x16),
    hero=(0x1F, 0x4E, 0x42), gold=(0xD4, 0xB4, 0x5A),
    htext=(0xF2, 0xED, 0xE2), hsub=(0xBF, 0xD5, 0xCB),
)
D = dict(
    paper=(0x14, 0x12, 0x0D), surface=(0x1B, 0x19, 0x13),
    container=(0x1F, 0x1D, 0x17), high=(0x2A, 0x27, 0x21),
    on=(0xEA, 0xE2, 0xD1), variant=(0xB7, 0xAE, 0x9C),
    outline=(0x83, 0x7B, 0x69), ov=(0x3D, 0x39, 0x2F),
    primary=(0x93, 0xCB, 0xB5), secondary=(0xD8, 0xBC, 0x6A),
    hero=(0x1F, 0x4E, 0x42), gold=(0xD4, 0xB4, 0x5A),
    htext=(0xF2, 0xED, 0xE2), hsub=(0xBF, 0xD5, 0xCB),
)

SS = 3  # supersample

W, H = 390, 780  # 1x design: small phone
DP = 2.0  # render 2x for crispness => actual canvas 780x1560


def squircle_mask(w, h, r, n, ss=1):
    m = Image.new("L", (w, h), 0)
    d = ImageDraw.Draw(m)
    r = min(r, w / 2, h / 2)
    half = math.pi / 2
    quad = 2.0 / n
    pts = [(r, 0), (w - r, 0)]
    for i in range(97):
        t = (1 - i / 96) * half
        pts.append((w - r + r * max(0, math.cos(t)) ** quad, r - r * max(0, math.sin(t)) ** quad))
    pts.append((w, h - r))
    for i in range(97):
        t = i / 96 * half
        pts.append((w - r + r * max(0, math.cos(t)) ** quad, h - r + r * max(0, math.sin(t)) ** quad))
    pts.append((r, h))
    for i in range(97):
        t = (1 - i / 96) * half
        pts.append((r - r * max(0, math.cos(t)) ** quad, h - r + r * max(0, math.sin(t)) ** quad))
    pts.append((0, r))
    for i in range(97):
        t = i / 96 * half
        pts.append((r - r * max(0, math.cos(t)) ** quad, r - r * max(0, math.sin(t)) ** quad))
    d.polygon(pts, fill=255)
    return m


def rounded_mask(w, h, r, ss=1):
    m = Image.new("L", (w, h), 0)
    d = ImageDraw.Draw(m)
    d.rounded_rectangle([0, 0, w - 1, h - 1], radius=r, fill=255)
    return m


def find_font(names, size):
    from PIL import ImageFont
    for base in ("C:/Windows/Fonts/", "C:/Windows/Fonts/"):
        for nm in names:
            try:
                return ImageFont.truetype(base + nm, size)
            except OSError:
                continue
    return ImageFont.load_default()


def draw_phone(theme, variant):
    p = L if theme == "light" else D
    canvas_w, canvas_h = int(W * DP), int(H * DP)
    img = Image.new("RGBA", (canvas_w, canvas_h), p["paper"] + (255,))
    d = ImageDraw.Draw(img)

    # status bar
    st = find_font(["segoeui.ttf", "arial.ttf"], int(11 * DP))
    d.text((18 * DP, 12 * DP), "14:32", font=st, fill=p["on"])

    # top bar title
    tb = find_font(["segoeuib.ttf", "arialbd.ttf"], int(14 * DP))
    d.text((20 * DP, 40 * DP), "The Ninety Nine Names of Allah", font=tb, fill=p["on"])
    # gear icon (simple)
    d.ellipse([W * DP - 46 * DP, 42 * DP, W * DP - 30 * DP, 58 * DP], outline=p["variant"], width=int(1.4 * DP))
    d.ellipse([W * DP - 41 * DP, 47 * DP, W * DP - 35 * DP, 53 * DP], outline=p["variant"], width=int(1.2 * DP))

    # hero card — squircle n=4, radius 28dp at 2x = 56px
    hero_x, hero_y = int(20 * DP), int(74 * DP)
    hero_w, hero_h = int((W - 40) * DP), int(230 * DP)
    hero = Image.new("RGBA", (hero_w, hero_h), p["hero"] + (255,))
    mask = squircle_mask(hero_w, hero_h, 28 * DP, 4)
    img.paste(hero, (hero_x, hero_y), mask)
    # overline
    ov_f = find_font(["segoeuisb.ttf", "arialbd.ttf"], int(8.5 * DP))
    overline = "NAME OF THE DAY"
    # tracked: draw char by char
    tw = sum(d.textlength(c, font=ov_f) + 2.2 * DP for c in overline) - 2.2 * DP
    cx = hero_x + (hero_w - tw) / 2
    for c in overline:
        d.text((cx, hero_y + 26 * DP), c, font=ov_f, fill=p["gold"])
        cx += d.textlength(c, font=ov_f) + 2.2 * DP
    # Arabic
    ar = find_font(["segoeui.ttf", "arial.ttf"], int(34 * DP))
    d.text((hero_x + hero_w / 2, hero_y + 100 * DP), "الإله", font=ar,
           fill=p["gold"], anchor="mm")
    # latin
    lt = find_font(["spectral-light.ttf", "georgia.ttf", "times.ttf"], int(19 * DP))
    d.text((hero_x + hero_w / 2, hero_y + 148 * DP), "Al-ilah", font=lt,
           fill=p["htext"], anchor="mm")
    it = find_font(["spectral-mediumitalic.ttf", "georgiai.ttf", "timesi.ttf"], int(13 * DP))
    d.text((hero_x + hero_w / 2, hero_y + 178 * DP), "The Deity", font=it,
           fill=p["hsub"], anchor="mm")

    # list rows
    rows = [("1", "Allah", "With regard to the name Allah"),
            ("2", "Al-Ahad", "The Unique"),
            ("3", "Al-A'laa", "The Most High"),
            ("4", "Al-Akram", "The Most Generous")]
    nm_f = find_font(["segoeuib.ttf", "arialbd.ttf"], int(13 * DP))
    ti_f = find_font(["segoeuii.ttf", "ariali.ttf"], int(11.5 * DP))
    fo_f = find_font(["segoeui.ttf", "arial.ttf"], int(9.5 * DP))
    ar2 = find_font(["segoeui.ttf", "arial.ttf"], int(19 * DP))
    y = hero_y + hero_h + 14 * DP
    for i, (folio, nm, ti) in enumerate(rows):
        dim = variant == "B" and i >= 2
        col_nm = p["on"] if not dim else tuple(int(c * 0.55 + 255 * 0.45) for c in p["on"])
        col_ti = p["variant"] if not dim else tuple(int(c * 0.55 + 255 * 0.45) for c in p["variant"])
        d.text((44 * DP, y), nm, font=nm_f, fill=col_nm)
        d.text((44 * DP, y + 19 * DP), ti, font=ti_f, fill=col_ti)
        d.text((26 * DP, y + 4 * DP), folio, font=fo_f, fill=p["variant"])
        d.text((W * DP - 30 * DP, y + 2 * DP), {"1": "الله", "2": "الأحد", "3": "الأعلى", "4": "الأكرم"}[folio],
               font=ar2, fill=p["primary"], anchor="ra")
        y += 62 * DP
        if i < len(rows) - 1:
            d.line([44 * DP, y - 6 * DP, W * DP - 20 * DP, y - 6 * DP], fill=p["ov"], width=1)

    # ================= bottom bar =================
    tabs = [("Names", True), ("Memorize", False), ("Bookmarks", False)]
    lab_f = find_font(["segoeuisb.ttf", "arialbd.ttf"], int(7.5 * DP))
    ic_f = 19 * DP

    def draw_tab(x, w, cy, label, sel):
        col = p["primary"] if sel else p["variant"]
        # glyph: book / cards / bookmark as simple line art
        gx = x + w / 2
        if label == "Names":
            d.rounded_rectangle([gx - 9 * DP, cy - 9 * DP, gx + 9 * DP, cy + 9 * DP],
                                radius=3 * DP, outline=col, width=int(1.6 * DP))
            d.line([gx - 9 * DP, cy + 3 * DP, gx - 9 * DP, cy - 9 * DP], fill=col, width=int(1.6 * DP))
        elif label == "Memorize":
            d.rounded_rectangle([gx - 9 * DP, cy - 7 * DP, gx + 9 * DP, cy + 9 * DP],
                                radius=2.5 * DP, outline=col, width=int(1.6 * DP))
            d.line([gx - 5 * DP, cy - 7 * DP, gx - 5 * DP, cy - 11 * DP], fill=col, width=int(1.6 * DP))
        else:
            d.polygon([(gx - 7 * DP, cy - 9 * DP), (gx + 7 * DP, cy - 9 * DP),
                       (gx + 7 * DP, cy + 9 * DP), (gx, cy + 4 * DP),
                       (gx - 7 * DP, cy + 9 * DP)], outline=col, fill=None, width=int(1.6 * DP))
        f2 = find_font(["segoeuib.ttf", "arialbd.ttf"], int(7.5 * DP))
        d.text((gx, cy + 16 * DP), label.upper(), font=f2, fill=col, anchor="ma")
        return col

    if variant == "A":
        bar_h = 64 * DP
        bar_y = canvas_h - int(88 * DP)
        d.rectangle([0, bar_y, canvas_w, bar_y + 3], fill=p["outline"])
        d.rectangle([0, bar_y + 3, canvas_w, canvas_h], fill=p["surface"])
        tab_w = canvas_w / 3
        for i, (label, sel) in enumerate(tabs):
            draw_tab(i * tab_w, tab_w, bar_y + 28 * DP, label, sel)
        # home pill
        d.rounded_rectangle([canvas_w / 2 - 45 * DP, canvas_h - 12 * DP,
                             canvas_w / 2 + 45 * DP, canvas_h - 8 * DP],
                            radius=2 * DP, fill=tuple(int(c * 0.25 + 255 * 0.75) for c in p["on"]))
    else:
        margin = 14 * DP
        bar_h = 60 * DP
        bar_y = canvas_h - int(26 * DP + bar_h + 22 * DP)  # above home strip
        bx0, by0 = int(margin), int(bar_y)
        bx1, by1 = int(canvas_w - margin), int(bar_y + bar_h)
        if variant == "B":
            # shadow: layered translucent squircles (blur approximation)
            from PIL import ImageFilter
            sh = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
            sd = ImageDraw.Draw(sh)
            for i, (grow, alpha) in enumerate([(8, 22), (18, 14), (30, 8)]):
                sd.rounded_rectangle(
                    [bx0 - grow * DP, by0 - grow * DP * 0.6, bx1 + grow * DP, by1 + grow * DP],
                    radius=(26 + grow * 0.8) * DP, fill=(20, 16, 8, alpha))
            sh = sh.filter(ImageFilter.GaussianBlur(10 * DP / 2))
            img.alpha_composite(sh)
        plate = Image.new("RGBA", (bx1 - bx0, by1 - by0), p["container"] + (255,))
        m = squircle_mask(bx1 - bx0, by1 - by0, 26 * DP, 4)
        img.paste(plate, (bx0, by0), m)
        if variant == "C" or theme == "dark":
            border = Image.new("RGBA", (bx1 - bx0, by1 - by0), (0, 0, 0, 0))
            bd = ImageDraw.Draw(border)
            # outline mask ring: outer minus inner
            outer = squircle_mask(bx1 - bx0, by1 - by0, 26 * DP, 4)
            inner = squircle_mask(bx1 - bx0 - int(1.0 * DP), by1 - by0 - int(1.0 * DP), 25 * DP, 4)
            ring = Image.new("L", (bx1 - bx0, by1 - by0), 0)
            ring.paste(Image.new("L", (bx1 - bx0, by1 - by0), 255), (0, 0), outer)
            ring.paste(Image.new("L", (bx1 - bx0, by1 - by0), 0), (int(0.5 * DP), int(0.5 * DP)),
                       squircle_mask(bx1 - bx0 - int(1.0 * DP), by1 - by0 - int(1.0 * DP), 25.4 * DP, 4))
            bd2 = ImageDraw.Draw(img)
            ring_img = Image.new("RGBA", (bx1 - bx0, by1 - by0), p["ov"] + (255,))
            img.paste(ring_img, (bx0, by0), ring)
        tab_w = (bx1 - bx0) / 3
        for i, (label, sel) in enumerate(tabs):
            draw_tab(bx0 + i * tab_w, tab_w, (by0 + by1) / 2 - 8 * DP, label, sel)
        # home pill
        d.rounded_rectangle([canvas_w / 2 - 45 * DP, canvas_h - 12 * DP,
                             canvas_w / 2 + 45 * DP, canvas_h - 8 * DP],
                            radius=2 * DP, fill=tuple(int(c * 0.25 + 255 * 0.75) for c in p["on"]))

    return img


def main():
    outs = []
    for theme in ("light", "dark"):
        for variant in ("A", "B", "C"):
            im = draw_phone(theme, variant)
            path = OUT_DIR / f"bar-{variant.lower()}-{theme}.png"
            im.save(path)
            outs.append(str(path))
    print("\n".join(outs))


if __name__ == "__main__":
    main()
