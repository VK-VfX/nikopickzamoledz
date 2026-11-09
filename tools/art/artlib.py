"""
Shared toolkit for the offline wallpaper renderer.

The app draws its live wallpapers with vector paths, which tops out at illustration. These
helpers render real images instead: spectral noise, domain warping, optical effects (bloom,
diffraction spikes, depth of field), filmic tone mapping. There is no per-frame time limit,
so the output can carry the kind of detail a phone cannot compute.

Everything works on float32 RGB arrays shaped (H, W, 3) in linear light.
"""
import numpy as np
from scipy import ndimage as ndi
from PIL import Image

W, H = 1080, 2340


class Rng:
    def __init__(self, seed):
        self.r = np.random.default_rng(seed)

    def __getattr__(self, name):
        return getattr(self.r, name)

    def f(self, a, b):
        return float(self.r.uniform(a, b))

    def i(self, a, b):
        return int(self.r.integers(a, b))

    def pick(self, seq):
        return seq[int(self.r.integers(0, len(seq)))]


# ---------- noise ----------

def fbm(h, w, rng, beta=2.6, aniso=(1.0, 1.0), kmin=0.0):
    """Spectral (1/f^beta) noise normalised to roughly [-1, 1]. Higher beta is smoother.
    aniso=(ax, ay) stretches the features by ax along x and ay along y."""
    white = rng.standard_normal((h, w)).astype(np.float32)
    F = np.fft.rfft2(white)
    # aniso = elongation: features are stretched by that factor along the axis. Stretching a field
    # by a factor a means its spectrum shrinks by a, so the frequency along that axis is *multiplied*.
    ky = np.fft.fftfreq(h)[:, None] * h * max(aniso[1], 1e-3)
    kx = np.fft.rfftfreq(w)[None, :] * w * max(aniso[0], 1e-3)
    k = np.sqrt(kx * kx + ky * ky)
    k[0, 0] = 1.0
    F *= (k ** (-beta / 2.0)).astype(np.float32)
    if kmin > 0:      # drop the very lowest modes, which otherwise show up as one big stripe
        F *= smoothstep(kmin * 0.5, kmin, k).astype(np.float32)
    F[0, 0] = 0
    out = np.fft.irfft2(F, s=(h, w)).astype(np.float32)
    out /= (np.abs(out).max() + 1e-8)
    return out * 1.6


def grid(h, w):
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    return x, y


def warp(field, dx, dy, order=1):
    """Sample [field] at (x + dx, y + dy) in pixels, wrapping at the edges."""
    h, w = field.shape[:2]
    x, y = grid(h, w)
    coords = np.array([y + dy, x + dx])
    if field.ndim == 2:
        return ndi.map_coordinates(field, coords, order=order, mode="wrap")
    return np.stack([ndi.map_coordinates(field[..., c], coords, order=order, mode="wrap") for c in range(field.shape[2])], -1)


def smoothstep(a, b, x):
    t = np.clip((x - a) / (b - a + 1e-8), 0, 1)
    return t * t * (3 - 2 * t)


def ramp(t, stops):
    """Colour ramp: stops is [(pos, (r, g, b)), ...] with pos ascending in [0, 1]."""
    pos = [p for p, _ in stops]
    cols = np.array([c for _, c in stops], np.float32)
    t = np.clip(t, 0, 1)
    return np.stack([np.interp(t, pos, cols[:, k]) for k in range(3)], -1).astype(np.float32)


def blur(img, sigma):
    if img.ndim == 2:
        return ndi.gaussian_filter(img, sigma, mode="constant")
    return np.stack([ndi.gaussian_filter(img[..., c], sigma, mode="constant") for c in range(3)], -1)


# ---------- optics ----------

def bloom(img, gains=(0.5, 0.35, 0.25, 0.18), sigmas=(3, 9, 24, 60), threshold=0.0):
    """Light scattering: add progressively wider blurs of the bright parts of the image."""
    hi = np.maximum(img - threshold, 0)
    out = img.copy()
    for g, s in zip(gains, sigmas):
        out += g * blur(hi, s)
    return out


BLACKBODY = [
    (0.00, (0.62, 0.74, 1.00)),   # hot, blue-white
    (0.30, (0.85, 0.90, 1.00)),
    (0.55, (1.00, 0.97, 0.90)),   # sun-like
    (0.78, (1.00, 0.80, 0.55)),
    (1.00, (1.00, 0.55, 0.32)),   # cool, orange-red
]


def stars(h, w, rng, n_faint=3500, n_bright=36, gain=1.0, density_mask=None, tint=None):
    """A star field with a realistic brightness distribution and diffraction spikes on the brightest."""
    img = np.zeros((h, w, 3), np.float32)
    n = n_faint
    xs = rng.uniform(0, w - 1, n)
    ys = rng.uniform(0, h - 1, n)
    if density_mask is not None:
        keep = rng.random(n) < density_mask[ys.astype(int), xs.astype(int)]
        xs, ys = xs[keep], ys[keep]
        n = len(xs)
    # Power-law flux: many dim stars, few bright ones.
    flux = (rng.random(n) ** 9.0) * 0.8 + 0.008
    col = ramp(rng.random(n), BLACKBODY)
    if tint is not None:
        col = col * 0.6 + np.array(tint, np.float32) * 0.4
    x0 = np.floor(xs).astype(int)
    y0 = np.floor(ys).astype(int)
    fx = xs - x0
    fy = ys - y0
    sig = 0.85
    for dy in range(-2, 4):
        for dx in range(-2, 4):
            wgt = np.exp(-(((dx - fx) ** 2 + (dy - fy) ** 2) / (2 * sig * sig))) / (2 * np.pi * sig * sig)
            xi = np.clip(x0 + dx, 0, w - 1)
            yi = np.clip(y0 + dy, 0, h - 1)
            for c in range(3):
                np.add.at(img[..., c], (yi, xi), flux * wgt * col[:, c] * 2.4 * gain)
    # The bright few get a tight core, a faint halo and thin diffraction spikes. Sizes scale with
    # the image so a half-size preview looks like the full render.
    k = w / 1080.0
    R = int(70 * k) + 2
    yy, xx = np.mgrid[-R:R + 1, -R:R + 1].astype(np.float32)
    rr = np.sqrt(xx * xx + yy * yy) + 1e-3
    for _ in range(n_bright):
        cx = int(rng.uniform(R, w - R))
        cy = int(rng.uniform(R, h - R))
        if density_mask is not None and rng.random() > density_mask[cy, cx] * 1.2:
            continue
        mag = rng.uniform(0.25, 1.0) ** 3.0
        c = ramp(np.array([rng.random()]), BLACKBODY)[0]
        core = 1.0 / (1.0 + (rr / (1.3 * k)) ** 3.2)
        halo = np.exp(-rr / (9.0 * k)) * 0.08 + np.exp(-rr / (30.0 * k)) * 0.025
        arm_len = 15.0 * k
        spike = (np.exp(-np.abs(xx) / (0.7 * k)) / (1 + (np.abs(yy) / arm_len) ** 1.6) +
                 np.exp(-np.abs(yy) / (0.7 * k)) / (1 + (np.abs(xx) / arm_len) ** 1.6)) * 0.32
        patch = (core + halo + spike) * mag * 2.6 * gain
        sl = (slice(cy - R, cy + R + 1), slice(cx - R, cx + R + 1))
        img[sl] += patch[..., None] * c[None, None, :]
    return img


# ---------- finishing ----------

def tonemap(img, exposure=1.0):
    """ACES filmic curve, then sRGB."""
    x = img * exposure
    a, b, c, d, e = 2.51, 0.03, 2.43, 0.59, 0.14
    y = np.clip((x * (a * x + b)) / (x * (c * x + d) + e), 0, 1)
    return np.where(y <= 0.0031308, 12.92 * y, 1.055 * np.power(y, 1 / 2.4) - 0.055)


def finish(img, path, rng=None, exposure=1.0, crush=0.010, quality=84, grain=0.35):
    """Tone map, lift nothing, make the dark parts true #000000, dither, and save as WebP."""
    srgb = tonemap(np.maximum(img, 0), exposure)
    # Anything this dark is a rounding artefact, not picture: make it exactly black.
    srgb = np.clip((srgb - crush) / (1 - crush), 0, 1)
    out = srgb * 255.0
    if rng is not None and grain > 0:
        n = rng.standard_normal(out.shape[:2]).astype(np.float32)[..., None] * grain
        out = out + n * (out > 4)          # grain only where there is light
    out = np.clip(np.round(out), 0, 255).astype(np.uint8)
    Image.fromarray(out, "RGB").save(path, "WEBP", quality=quality, method=6)
    return out


def black_ratio(arr):
    return float(((arr[..., 0] <= 3) & (arr[..., 1] <= 3) & (arr[..., 2] <= 3)).mean())
