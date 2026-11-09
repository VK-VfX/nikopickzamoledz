"""Abstract: liquid ink and satin."""
import numpy as np
from artlib import *

JEWELS = {
    "sapphire": (0.10, 0.28, 0.95), "ruby": (0.95, 0.10, 0.25), "emerald": (0.08, 0.80, 0.45),
    "amethyst": (0.62, 0.20, 0.95), "gold": (1.0, 0.68, 0.18), "rose": (0.98, 0.35, 0.60), "teal": (0.05, 0.75, 0.80),
}


def _cos_palette(t, a, b, c, d):
    return np.stack([a[i] + b[i] * np.cos(2 * np.pi * (c[i] * t + d[i])) for i in range(3)], -1)


def ink(seed, w=W, h=H):
    """Marbled ink: nested domain warps, iridescent film lines on the surface, black between the flows."""
    rng = Rng(seed)
    k = w / 1080.0
    def F(beta, a=(1, 1)):
        return fbm(h, w, rng, beta, a)
    q1 = F(4.8)
    q2 = F(4.8)
    f = warp(F(4.4), q1 * 330 * k, q2 * 330 * k)
    f = warp(f, F(4.6) * 140 * k, F(4.6) * 140 * k)
    f = 0.5 + 0.5 * f / (np.abs(f).max() + 1e-6)
    body = smoothstep(rng.f(0.42, 0.5), rng.f(0.58, 0.68), f)
    # Thin-film interference: bands of hue that follow the flow.
    film = _cos_palette(f * rng.f(3.0, 5.5) + q1 * 0.6, (0.5, 0.5, 0.5), (0.5, 0.5, 0.5), (1.0, 1.0, 1.0), (rng.f(0, 1), rng.f(0, 1), rng.f(0, 1)))
    base = np.array(JEWELS[rng.pick(list(JEWELS))], np.float32)
    mix = rng.f(0.3, 0.6)
    col = film * mix + (base * (0.5 + 0.8 * f[..., None])) * (1 - mix)
    # Glossy relief from the field's gradient: smooth, so it reads as liquid and not as foil.
    gy, gx = np.gradient(blur(f, 5 * k))
    nrm = np.stack([-gx * 22, -gy * 22, np.ones_like(gx)], -1)
    nrm /= np.linalg.norm(nrm, axis=-1, keepdims=True)
    Ldir = np.array([-0.5, -0.6, 0.62], np.float32)
    Ldir /= np.linalg.norm(Ldir)
    diff = np.clip((nrm * Ldir).sum(-1), 0, 1)
    hv = Ldir + np.array([0, 0, 1], np.float32)
    hv /= np.linalg.norm(hv)
    spec = np.clip((nrm * hv).sum(-1), 0, 1) ** 70
    img = col * (body * (0.22 + 0.85 * diff))[..., None] + (spec * body * 0.8)[..., None]
    return bloom(img, gains=(0.1, 0.1), sigmas=(4 * k, 16 * k))


def silk(seed, w=W, h=H):
    """Satin folds: anisotropic highlights on a dark, deeply shadowed cloth."""
    rng = Rng(seed)
    k = w / 1080.0
    hgt = fbm(h, w, rng, 5.4, (1.0, 2.2)) + 0.4 * fbm(h, w, rng, 4.6, (1.0, 1.6))
    hgt = warp(hgt, fbm(h, w, rng, 4.8) * 160 * k, fbm(h, w, rng, 4.8) * 160 * k)
    folds = hgt * rng.f(2.5, 4.0) + 0.25 * np.sin(hgt * rng.f(4.0, 8.0))
    gy, gx = np.gradient(blur(folds, 3 * k))
    nrm = np.stack([-gx * 55, -gy * 55, np.ones_like(gx)], -1)
    nrm /= np.linalg.norm(nrm, axis=-1, keepdims=True)
    Ld = np.array([rng.f(-0.7, 0.2), rng.f(-0.8, -0.2), 0.55], np.float32)
    Ld /= np.linalg.norm(Ld)
    diff = np.clip((nrm * Ld).sum(-1), 0, 1)
    hv = Ld + np.array([0, 0, 1], np.float32)
    hv /= np.linalg.norm(hv)
    ndh = np.clip((nrm * hv).sum(-1), 0, 1)
    spec = ndh ** 26 * 0.8 + ndh ** 180 * 0.8
    base = np.array(JEWELS[rng.pick(list(JEWELS))], np.float32)
    lit = smoothstep(0.30, 0.98, diff)               # shadows are properly black: colour only where light reaches
    img = base[None, None, :] * (lit ** 1.5 * 0.9)[..., None] + (spec * lit)[..., None] * (base * 0.5 + 0.5)[None, None, :]
    x, y = grid(h, w)
    ang = rng.f(0, 2 * np.pi)
    ramp_ = np.clip(((x / w - 0.5) * np.cos(ang) + (y / h - 0.5) * np.sin(ang)) * 1.7 + 0.6, 0, 1)
    img *= smoothstep(0.0, 0.75, ramp_)[..., None]
    return bloom(img, gains=(0.08, 0.08), sigmas=(5 * k, 20 * k))


GENERATORS = {"ink": ink, "silk": silk}
