# -*- coding: utf-8 -*-
"""
Regenerate res/drawable-nodpi/widget_preview_image.png to match the runtime
widget plate: a true squircle (superellipse n=4, the app's SquircleShape
exponent) at the device-class 24dp radius, hero palette, Ar-Rahmaan in
bundled HAFS.

Recipe per AGENTS.md: uharfbuzz shaping -> font.draw_glyph_with_pen outlines
-> flatten curves -> even-odd fill (parity across a glyph's contours handles
counters; glyphs paint over each other = OR). Positions are y-up; flipped
once at raster. Latin lines render through PIL (no shaping needed).
"""

import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont
import uharfbuzz as hb

ROOT = Path(__file__).resolve().parents[2]
FONT_DIR = ROOT / "app/src/main/res/font"
OUT = ROOT / "app/src/main/res/drawable-nodpi/widget_preview_image.png"

# ---------------------------------------------------------------- config
W, H = 820, 500            # 2x of a 410x250dp slot
SS = 4                     # supersample factor for the vector layers
DP = 2.0                   # design density: 1dp = 2px
RADIUS_PX = 24.0 * DP      # device-class corner radius
N = 4.0                    # SquircleShape exponent

HERO_CONTAINER = (0x1F, 0x4E, 0x42, 255)
HERO_GOLD = (0xD4, 0xB4, 0x5A, 255)
HERO_TEXT = (0xF2, 0xED, 0xE2, 255)
HERO_SUBTEXT = (0xBF, 0xD5, 0xCB, 255)

ARABIC = "\u0627\u0644\u0631\u064E\u0651\u062D\u0645\u0640\u064E\u0670\u0646\u064F"  # الرَّحْمَـٰنُ
NAME = "Ar-Rahmaan"
TITLE = "The Extremely Merciful"


# ---------------------------------------------------------------- squircle
def squircle_points(w, h, r, n, samples=96):
    """Sampled superellipse outline, same construction as SquircleShape.kt."""
    r = min(r, w / 2.0, h / 2.0)
    half_pi = math.pi / 2.0
    quad = 2.0 / n
    pts = [(r, 0.0), (w - r, 0.0)]
    # top-right
    for i in range(samples + 1):
        t = (1.0 - i / samples) * half_pi
        pts.append((w - r + r * math.cos(t) ** quad, r - r * math.sin(t) ** quad))
    # bottom-right
    pts.append((w, h - r))
    for i in range(samples + 1):
        t = i / samples * half_pi
        pts.append((w - r + r * math.cos(t) ** quad, h - r + r * math.sin(t) ** quad))
    # bottom-left
    pts.append((r, h))
    for i in range(samples + 1):
        t = (1.0 - i / samples) * half_pi
        pts.append((r - r * math.cos(t) ** quad, h - r + r * math.sin(t) ** quad))
    # top-left
    pts.append((0.0, r))
    for i in range(samples + 1):
        t = i / samples * half_pi
        pts.append((r - r * math.cos(t) ** quad, r - r * math.sin(t) ** quad))
    return pts


# ---------------------------------------------------------------- arabic
class OutlinePen:
    """fontTools-pen-compatible recorder; curves arrive unflattened."""

    def __init__(self):
        self.contours = []   # list of list[(x, y)] in font units
        self.cur = None

    def moveTo(self, p):
        self.cur = [(p[0], p[1])]
        self.contours.append(self.cur)

    def lineTo(self, p):
        self.cur.append((p[0], p[1]))

    def qCurveTo(self, *points):
        # TrueType: all but last are off-curve; implied on-points between.
        *offs, on = points
        start = self.cur[-1]
        for i in range(len(offs)):
            off = offs[i]
            if i < len(offs) - 1:
                nxt = offs[i + 1]
                implied = ((off[0] + nxt[0]) / 2.0, (off[1] + nxt[1]) / 2.0)
            else:
                implied = on
            self._quad(start, off, implied)
            start = implied

    def _quad(self, p0, p1, p2, steps=12):
        for i in range(1, steps + 1):
            t = i / steps
            mt = 1 - t
            self.cur.append((
                mt * mt * p0[0] + 2 * mt * t * p1[0] + t * t * p2[0],
                mt * mt * p0[1] + 2 * mt * t * p1[1] + t * t * p2[1],
            ))

    def cubicTo(self, p1, p2, p3, steps=16):
        p0 = self.cur[-1]
        for i in range(1, steps + 1):
            t = i / steps
            mt = 1 - t
            self.cur.append((
                mt**3 * p0[0] + 3 * mt**2 * t * p1[0] + 3 * mt * t**2 * p2[0] + t**3 * p3[0],
                mt**3 * p0[1] + 3 * mt**2 * t * p1[1] + 3 * mt * t**2 * p2[1] + t**3 * p3[1],
            ))

    curveTo = cubicTo

    def closePath(self):
        if self.cur and self.cur[0] != self.cur[-1]:
            self.cur.append(self.cur[0])

    endPath = closePath


def shape_text(font, text):
    buf = hb.Buffer()
    buf.add_str(text)
    buf.guess_segment_properties()
    hb.shape(font, buf, {"kern": True, "liga": True})
    return list(zip(buf.glyph_infos, buf.glyph_positions))


def rasterize_glyphs(font, face_upem, text, px_size, color, width, height,
                     center_x, baseline_y):
    """Even-odd rasterize shaped text into an SS-scale RGBA image; y-up flipped."""
    scale = px_size / face_upem
    pen = OutlinePen()
    x = 0.0
    layer = Image.new("L", (width, height), 0)
    for info, pos in shape_text(font, text):
        pen.contours = []
        font.draw_glyph_with_pen(info.codepoint, pen)
        gx = center_x + (x + pos.x_offset) * scale
        gy = baseline_y - pos.y_offset * scale
        for contour in pen.contours:
            # flatten to scanline edges in SS pixel space (flip y once here)
            pts = [((gx + cx * scale) * SS, (gy - cy * scale) * SS)
                   for (cx, cy) in contour]
            fill_contour_evenodd(layer, pts, 255)
        x += pos.x_advance
    # colorize through the coverage mask
    colored = Image.new("RGBA", (width, height), color)
    colored.putalpha(layer)
    return colored


def fill_contour_evenodd(layer, pts, value):
    """Even-odd scanline fill of one polygon onto an 'L' image (max-add)."""
    if len(pts) < 3:
        return
    w, h = layer.size
    ys = [p[1] for p in pts]
    y0, y1 = max(0, int(min(ys))), min(h - 1, int(max(ys)) + 1)
    px = layer.load()
    for y in range(y0, y1 + 1):
        yc = y + 0.5
        xs = []
        n = len(pts)
        for i in range(n):
            (ax, ay), (bx, by) = pts[i], pts[(i + 1) % n]
            if (ay <= yc < by) or (by <= yc < ay):
                xs.append(ax + (yc - ay) / (by - ay) * (bx - ax))
        xs.sort()
        for i in range(0, len(xs) - 1, 2):
            xa, xb = int(math.ceil(xs[i])), int(math.floor(xs[i + 1]))
            for x in range(max(0, xa), min(w - 1, xb) + 1):
                px[x, y] = value


# ---------------------------------------------------------------- main
def main():
    ss_w, ss_h = W * SS, H * SS

    # --- plate: squircle, supersampled ---
    plate = Image.new("RGBA", (ss_w, ss_h), (0, 0, 0, 0))
    pd = ImageDraw.Draw(plate)
    pts = squircle_points(ss_w, ss_h, RADIUS_PX * SS, N)
    pd.polygon(pts, fill=HERO_CONTAINER)
    plate = plate.resize((W, H), Image.LANCZOS)

    # --- content ---
    cx = W / 2.0
    pad_px = 16 * DP

    arabic_px = 30 * DP
    name_size = int(16 * DP)
    title_size = int(12 * DP)

    f_hb = hb.Font(hb.Face(hb.Blob.from_file_path(str(FONT_DIR / "kfgqpc_hafs_uthmanic.ttf"))))
    upem = f_hb.face.upem
    f_hb.scale = (upem, upem)

    # Measure the Arabic's ink box: draw with a mid-canvas baseline so the
    # ink (which extends both above and below it) stays inside the bitmap.
    PROBE_BASE = H  # generous room above and below
    probe = rasterize_glyphs(f_hb, upem, ARABIC, arabic_px, HERO_GOLD, W, H * 2, cx, PROBE_BASE)
    bbox = probe.getbbox()
    arabic_h = (bbox[3] - bbox[1]) if bbox else arabic_px * 1.45

    name_font = ImageFont.truetype(str(FONT_DIR / "spectral_light.ttf"), name_size)
    title_font = ImageFont.truetype(str(FONT_DIR / "spectral_mediumitalic.ttf"), title_size)

    gap = 4 * DP
    stack_h = arabic_h + gap + name_size * 1.3 + gap + title_size * 1.3
    top = (H - stack_h) / 2.0

    # Arabic: the probe was drawn with baseline at PROBE_BASE in an H*2 canvas;
    # ink_top_above_baseline = PROBE_BASE - bbox_top (in that frame).
    ink_top_above_baseline = (PROBE_BASE - bbox[1]) if bbox else arabic_px
    baseline = top + ink_top_above_baseline
    arabic_layer = rasterize_glyphs(f_hb, upem, ARABIC, arabic_px, HERO_GOLD,
                                    W, H, cx, baseline)
    plate.alpha_composite(arabic_layer)

    # Latin lines
    d = ImageDraw.Draw(plate)
    y_name = top + arabic_h + gap
    d.text((cx, y_name), NAME, font=name_font, fill=HERO_TEXT, anchor="ma")
    y_title = y_name + name_size * 1.3 + gap
    d.text((cx, y_title), TITLE, font=title_font, fill=HERO_SUBTEXT, anchor="ma")

    plate.save(OUT)
    print("wrote", OUT, plate.size)


if __name__ == "__main__":
    sys.exit(main())
