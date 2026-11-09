"""Nature: night skies over mountains and still water."""
import numpy as np
from artlib import *
from space import moon_body


def _line(w, rng, beta, amp):
    return fbm(1, w, rng, beta)[0] * amp


def milky_way(h, w, rng):
    """Returns (glow RGB, star density mask)."""
    ang = rng.f(-0.5, 0.5) + np.pi / 2            # roughly vertical, leaning a little
    x, y = grid(h, w)
    cx, cy = w * rng.f(0.35, 0.65), h * rng.f(0.2, 0.4)
    d = (x - cx) * np.cos(ang) + (y - cy) * np.sin(ang) * 0.0 - (y - cy) * np.sin(ang) * 0.0
    # perpendicular distance to a line through (cx, cy) with direction (sin, -cos)
    dx, dy = x - cx, y - cy
    dist = np.abs(dx * np.cos(ang) + dy * np.sin(ang))
    band = np.exp(-(dist / (w * 0.2)) ** 2)
    band *= 0.55 + 0.6 * smoothstep(-0.6, 0.8, fbm(h, w, rng, 3.2))
    dust = smoothstep(0.1, 0.8, 1 - np.abs(fbm(h, w, rng, 3.0, (1.0, 2.2)))) * np.exp(-(dist / (w * 0.09)) ** 2)
    glow = band * (1 - 0.8 * dust)
    col = ramp(np.clip(dist / (w * 0.3), 0, 1), [(0, (1.0, 0.84, 0.64)), (0.5, (0.82, 0.82, 0.92)), (1, (0.55, 0.68, 1.0))])
    return (glow[..., None] * col * 0.19).astype(np.float32), np.clip(band * 0.9 + 0.12, 0, 1).astype(np.float32)


def aurora(h, w, rng, horizon):
    """Curtains of light: sharp lower edge, rays running up, green fading to violet."""
    img = np.zeros((h, w, 3), np.float32)
    x, y = grid(h, w)
    for c in range(rng.i(1, 3)):
        base = horizon * rng.f(0.42, 0.7)
        curve = base + _line(w, rng, 4.4, horizon * rng.f(0.07, 0.14)) + horizon * 0.025 * np.sin(np.linspace(0, rng.f(2, 5), w) + rng.f(0, 6))
        curve = np.tile(curve[None, :], (h, 1))
        rel = y - curve
        sigma_low = horizon * rng.f(0.01, 0.022)
        up_len = horizon * rng.f(0.1, 0.22)
        lower = np.exp(-(np.clip(rel, 0, None) / sigma_low) ** 2)
        upper = np.exp(np.clip(rel, None, 0) / up_len)
        profile = np.where(rel > 0, lower, upper)
        rays = fbm(h, w, rng, 2.0, (1.0, 60.0))
        rays = 0.25 + 1.0 * np.clip(0.5 + 0.6 * rays, 0, 1) ** 1.6
        wave = 0.5 + 0.5 * np.sin(x * rng.f(0.003, 0.012) + rng.f(0, 6) + fbm(h, w, rng, 3.5) * 2)
        inten = profile * rays * (0.45 + 0.7 * wave) * rng.f(0.7, 1.2)
        height = np.clip(-rel / (horizon * 0.55), 0, 1)           # 0 at the lower edge, 1 high up
        tint = ramp(height, [(0, (0.55, 1.0, 0.7)), (0.12, (0.25, 0.95, 0.5)), (0.45, (0.2, 0.75, 0.75)), (0.8, (0.55, 0.3, 0.85)), (1, (0.75, 0.2, 0.5))])
        if rng.random() < 0.3:      # a few are mostly pink and violet
            tint = tint[..., ::-1] * 0.5 + tint * 0.5
        img += inten[..., None] * tint * 0.42
    return img


def ridge_mask(w, rng, base_y, amp, beta=2.4, sharp=1.0):
    """A skyline: plain fractal noise reads as natural mountains; a ridged term adds a few summits."""
    n = fbm(1, w, rng, beta)[0]
    summit = np.clip(1 - np.abs(fbm(1, w, rng, beta + 0.6)[0]), 0, 1) ** (2.0 * sharp)
    prof = np.clip(0.45 + 0.42 * n + 0.28 * summit * np.clip(n + 0.6, 0, 1), 0, None)
    return base_y - prof * amp


def scene(seed, w=W, h=H, kind=None):
    rng = Rng(seed)
    k = w / 1080.0
    kind = kind or rng.pick(["aurora", "aurora", "milky", "moon"])
    horizon = int(h * rng.f(0.62, 0.7))
    sky = np.zeros((h, w, 3), np.float32)

    if kind == "milky":
        glow, dens = milky_way(h, w, rng)
        sky += glow
        sf = stars(h, w, rng, n_faint=15000, n_bright=rng.i(4, 8), gain=0.9, density_mask=dens)
    else:
        sf = stars(h, w, rng, n_faint=7000, n_bright=rng.i(3, 7), gain=0.85)
    if kind == "aurora":
        sky += aurora(h, w, rng, horizon)
    if kind == "moon":
        mr = int(w * rng.f(0.07, 0.1))
        mx, my = int(w * rng.f(0.3, 0.7)), int(h * rng.f(0.14, 0.3))
        pad = mr * 4
        body, inside, rr, ph = moon_body(pad, pad, pad // 2, pad // 2, mr, rng, mr / 400.0, ph=rng.f(0.7, 1.0))
        body = body * 1.25
        x, y = grid(h, w)
        d = np.sqrt((x - mx) ** 2 + (y - my) ** 2)
        sky += (np.exp(-np.clip(d - mr, 0, None) / (mr * 1.1)) * 0.20 * (d > mr) + np.exp(-np.clip(d - mr, 0, None) / (mr * 6)) * 0.035)[..., None] * np.array([0.7, 0.8, 1.0], np.float32)
        y0, x0 = my - pad // 2, mx - pad // 2
        ys0, xs0 = max(y0, 0), max(x0, 0)
        ys1, xs1 = min(y0 + pad, h), min(x0 + pad, w)
        patch = body[ys0 - y0:ys1 - y0, xs0 - x0:xs1 - x0]
        pin = inside[ys0 - y0:ys1 - y0, xs0 - x0:xs1 - x0][..., None]
        sky[ys0:ys1, xs0:xs1] = np.where(pin, patch, sky[ys0:ys1, xs0:xs1])
        sf *= (1 - np.clip(np.exp(-np.clip(d - mr, 0, None) / (mr * 3)), 0, 1))[..., None]
    # faint airglow: the sky is a hair lighter at the horizon
    yy = np.arange(h, dtype=np.float32)[:, None]
    glow_h = np.exp(-np.abs(yy - horizon) / (h * 0.07)) * 0.022
    sky += glow_h[..., None] * np.array([0.4, 0.6, 0.8], np.float32) * np.ones((1, w, 1), np.float32)
    sky += sf

    # Mountains in layers, far to near: tall and hazy at the back, low and pure black at the front.
    layers = rng.i(3, 5)
    yy = np.arange(h, dtype=np.float32)[:, None]
    mist = np.array([0.30, 0.40, 0.56], np.float32)
    for i in range(layers):
        near = i / max(layers - 1, 1)                          # 0 far .. 1 near
        amp = h * (0.26 - 0.17 * near) * rng.f(0.85, 1.15)
        top = ridge_mask(w, rng, horizon + h * 0.004, amp, beta=2.2 + 0.4 * near, sharp=1.0)
        top = np.minimum(top, horizon - h * 0.01)
        mask = blur((yy >= top[None, :]).astype(np.float32), 0.9)[..., None]
        # Mist lies in a band along the horizon, whatever the skyline does; nearer ranges are darker.
        band = np.exp(-np.clip(horizon - yy, 0, None) / (h * 0.05))
        shade = (1 - near) ** 1.5
        layer_col = mist[None, None, :] * (0.02 + 0.12 * band[..., None]) * shade
        sky = sky * (1 - mask) + layer_col * mask
    # Lake: below the horizon, mirror the scene with ripple distortion and a little loss.
    out = sky.copy()
    ys = np.arange(horizon, h)
    src = 2 * horizon - ys
    valid = src >= 0
    refl = np.zeros((len(ys), w, 3), np.float32)
    refl[valid] = sky[src[valid]]
    ripple_x = fbm(len(ys), w, rng, 2.6, (3.0, 0.4)) * (4 * k)
    depth_f = (np.arange(len(ys), dtype=np.float32) / max(len(ys), 1))[:, None]
    refl = warp(refl, ripple_x * (0.4 + 2.2 * depth_f), np.zeros_like(ripple_x))
    refl = blur(refl, 1.2 * k)
    refl *= (0.55 * np.exp(-depth_f * 1.1))[..., None]
    # A few long ripple glints where the brightest parts reflect.
    out[horizon:] = refl
    # Near shore: a black tree/rock line framing the bottom.
    tree = h - (h * 0.035 + np.abs(fbm(1, w, rng, 2.0)[0]) * h * 0.05)
    tmask = (np.arange(h)[:, None] >= tree[None, :]).astype(np.float32)[..., None]
    out = out * (1 - tmask)
    return out


GENERATORS = {"aurora": lambda s, w=W, h=H: scene(s, w, h, "aurora"),
              "milkyway": lambda s, w=W, h=H: scene(s, w, h, "milky"),
              "moonlit": lambda s, w=W, h=H: scene(s, w, h, "moon")}
