"""Light: bokeh and long-exposure light trails."""
import numpy as np
from PIL import Image, ImageDraw, ImageChops
from artlib import *

WARM = [(1.0, 0.62, 0.20), (1.0, 0.80, 0.40), (1.0, 0.45, 0.30), (1.0, 0.90, 0.70), (0.95, 0.35, 0.45)]
COOL = [(0.30, 0.55, 1.0), (0.45, 0.85, 1.0), (0.70, 0.45, 1.0), (0.25, 0.95, 0.85), (1.0, 0.45, 0.8)]


def bokeh(seed, w=W, h=H):
    """Out-of-focus lights: soft discs with a brighter rim, in depth layers, a few in focus."""
    rng = Rng(seed)
    k = w / 1080.0
    pal = rng.pick([WARM, COOL, WARM + COOL[:2]])
    img = np.zeros((h, w, 3), np.float32)
    cx0, cy0 = w * rng.f(0.3, 0.7), h * rng.f(0.35, 0.65)
    spread = rng.f(0.25, 0.42)
    n = rng.i(90, 170)
    for i in range(n):
        depth = rng.random() ** 1.3                           # near lights are big, far ones small
        r = (14 + 130 * depth) * k
        cx = cx0 + rng.standard_normal() * w * spread
        cy = cy0 + rng.standard_normal() * h * spread * 0.8
        col = np.array(pal[rng.i(0, len(pal))], np.float32)
        R = int(r * 1.5) + 2
        y0, y1 = int(max(cy - R, 0)), int(min(cy + R, h))
        x0, x1 = int(max(cx - R, 0), ), int(min(cx + R, w))
        if y1 <= y0 or x1 <= x0:
            continue
        gx, gy = np.meshgrid(np.arange(x0, x1) - cx, np.arange(y0, y1) - cy)
        d = np.sqrt(gx * gx + gy * gy) / r
        soft = 0.04 + 0.10 * depth                            # near lights are blurrier at the edge
        edge = smoothstep(1.0 + soft, 1.0 - soft * 2, d)
        rim = 0.5 + 0.9 * np.exp(-((d - 0.93) / 0.10) ** 2)    # real bokeh is brighter at the edge
        grain = 1 + 0.14 * np.sin(gx * 0.9 + rng.f(0, 6)) * np.sin(gy * 0.7 + rng.f(0, 6))
        disc = edge * rim * (0.7 + 0.3 * d) * grain
        a = float(np.exp(rng.standard_normal() * 0.75 - 1.6)) * (0.55 + 0.6 * (1 - depth))
        img[y0:y1, x0:x1] += disc[..., None] * col * min(a, 1.4)
    # A few lights that are actually in focus.
    sharp = np.zeros_like(img)
    for i in range(rng.i(8, 20)):
        x_, y_ = int(rng.uniform(10, w - 10)), int(rng.uniform(10, h - 10))
        c = np.array(pal[rng.i(0, len(pal))], np.float32)
        sharp[y_ - 1:y_ + 2, x_ - 1:x_ + 2] += c * rng.f(0.6, 1.6)
    img += bloom(sharp, gains=(0.9, 0.5, 0.3), sigmas=(2 * k, 7 * k, 20 * k))
    # Fade to black at the edges so the cluster sits in the dark.
    x, y = grid(h, w)
    vig = np.clip(1.25 - np.sqrt(((x - w / 2) / (w * 0.62)) ** 2 + ((y - h / 2) / (h * 0.62)) ** 2), 0, 1)
    return bloom(img * (vig ** 1.2)[..., None] * 1.35, gains=(0.12, 0.1), sigmas=(8 * k, 30 * k))


def trails(seed, w=W, h=H):
    """A long exposure of a highway at night: streaks of tail lights and headlights bending to the horizon."""
    rng = Rng(seed)
    k = w / 1080.0
    vx, vy = w * rng.f(0.38, 0.62), h * rng.f(0.38, 0.5)
    bend = rng.f(-0.35, 0.35) * w
    streaks = []

    def road(offset, t):
        """Position along a lane at parameter t (0 near camera, 1 at the horizon), with a bend."""
        depth = max(t, 0.0) ** 0.55
        y = h * 1.05 + (vy - h * 1.05) * depth
        x = vx + (offset * (1 - depth)) + bend * np.sin(np.pi * depth * 0.8) * depth
        return x, y

    canvas = np.zeros((h, w, 3), np.float32)
    for lane_side, base_col, n_streak in ((-1, (1.0, 0.16, 0.10), rng.i(16, 26)), (1, (1.0, 0.92, 0.75), rng.i(12, 22))):
        layer = Image.new("RGB", (w, h), (0, 0, 0))
        dr = ImageDraw.Draw(layer)
        for _ in range(n_streak):
            off = lane_side * rng.uniform(0.04, 0.9) * w
            t0, t1 = rng.uniform(-0.04, 0.22), rng.uniform(0.6, 1.0)
            ts = np.linspace(t0, t1, 110)
            bright = rng.uniform(0.35, 1.0)
            col = np.array(base_col) * (0.8 + 0.4 * rng.random())
            for a, b in zip(ts[:-1], ts[1:]):
                xa, ya = road(off, a)
                xb, yb = road(off, b)
                width = max(1, int((1 - b) ** 1.5 * rng.uniform(7, 17) * k + 1))
                fade = (1 - 0.7 * b) * bright
                dr.line([(xa, ya), (xb, yb)], fill=tuple(int(np.clip(c * fade * 255, 0, 255)) for c in col), width=width)
        canvas += np.asarray(layer, np.float32) / 255.0
    # Streaks glow: sharp core plus soft halo.
    # Wet road: the lights smear downward as a faint reflection.
    wet = blur(canvas, 14 * k) * 0.35
    img = bloom(canvas * 1.15 + wet, gains=(0.9, 0.7, 0.5), sigmas=(1.5 * k, 6 * k, 22 * k))
    # Distant city lights at the vanishing point, out of focus.
    far = np.zeros_like(img)
    for _ in range(rng.i(60, 140)):
        x_ = int(np.clip(vx + rng.standard_normal() * w * 0.22, 6, w - 7))
        y_ = int(np.clip(vy + rng.standard_normal() * h * 0.018 - h * 0.006, 6, h - 7))
        c = np.array(rng.pick(WARM + [(1, 1, 1)]), np.float32)
        far[y_ - 1:y_ + 2, x_ - 1:x_ + 2] += c * rng.f(0.3, 1.0)
    img += bloom(far, gains=(1.2, 0.9, 0.6), sigmas=(2.5 * k, 7 * k, 18 * k)) * 0.7
    # A glow where the road meets the sky.
    x, y = grid(h, w)
    sky = np.exp(-((y - vy) / (h * 0.03)) ** 2) * np.exp(-((x - vx) / (w * 0.3)) ** 2) * 0.06
    img += sky[..., None] * np.array([1.0, 0.55, 0.3], np.float32)
    return img


GENERATORS = {"bokeh": bokeh, "trails": trails}
