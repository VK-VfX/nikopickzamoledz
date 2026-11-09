"""Space: nebulae, galaxies, a gas giant and the moon."""
import numpy as np
from artlib import *

PALETTES = {
    # emission lines mapped to colour, the way the big space telescopes do it
    "hubble": [(1.00, 0.30, 0.10), (1.00, 0.78, 0.25), (0.10, 0.55, 1.00)],
    "rose":   [(1.00, 0.25, 0.45), (1.00, 0.55, 0.65), (0.30, 0.45, 1.00)],
    "ember":  [(1.00, 0.10, 0.03), (1.00, 0.42, 0.08), (1.00, 0.68, 0.30)],
    "teal":   [(0.10, 0.75, 0.70), (0.30, 0.90, 0.95), (0.55, 0.40, 1.00)],
    "violet": [(0.75, 0.20, 1.00), (1.00, 0.45, 0.90), (0.25, 0.60, 1.00)],
}


def nebula(seed, w=W, h=H, palette=None):
    rng = Rng(seed)
    pal = PALETTES[palette or rng.pick(list(PALETTES))]
    k = w / 1080.0
    s = rng.f(0.85, 1.2)

    def F(beta, a=(1, 1)):
        return fbm(h, w, rng, beta, a)

    # Domain warping gives gas its curling structure. Smooth noise (high beta) is what makes it
    # read as gas rather than static, with only a trace of fine grain on top.
    wx1, wy1 = F(4.0) * 330 * s * k, F(4.0) * 330 * s * k
    base = warp(F(3.6), wx1, wy1)
    wx2, wy2 = F(3.6) * 150 * s * k, F(3.6) * 150 * s * k
    mid = warp(F(3.1), wx2 + wx1 * 0.5, wy2 + wy1 * 0.5)
    fine = warp(F(2.5), wx2, wy2)
    ridge = 1.0 - np.abs(warp(F(3.2, (1.0, 1.7)), wx1 * 0.8, wy1 * 0.8))
    ridge = smoothstep(0.35, 1.0, ridge) ** 1.5
    dens = np.clip(0.45 + 0.6 * base + 0.22 * mid + 0.05 * fine, 0, 1.3)

    # Where the nebula lives: one or two lobes, with real darkness around them.
    lobe = F(4.6) * 0.95 + F(3.6) * 0.35
    mask = smoothstep(rng.f(-0.1, 0.1), rng.f(0.75, 1.05), lobe)
    dens = np.clip(dens * mask, 0, None) ** rng.f(1.15, 1.5)

    ha = dens * (0.55 + 0.6 * mid)
    oiii = np.clip(warp(dens, wx2 * 0.5 + 24 * k, wy2 * 0.5 - 16 * k) * (0.4 + 0.9 * ridge), 0, None) ** 1.2
    sii = np.clip(dens ** 1.6 * (0.4 + 0.9 * ridge), 0, None)
    img = (sii[..., None] * np.array(pal[0], np.float32) * 0.9 +
           ha[..., None] * np.array(pal[1], np.float32) * 0.7 +
           oiii[..., None] * np.array(pal[2], np.float32) * 0.8) * 0.62

    # Dust: dark lanes sit in front of the glow and carve it into pillars.
    dust = smoothstep(0.35, 0.95, ridge * 0.8 + warp(F(3.4), wx2, wy2) * 0.55)
    img *= np.exp(-dust * mask * rng.f(1.6, 2.8))[..., None]

    thin = np.clip(1.0 - dust * mask * 0.9, 0.05, 1)
    sf = stars(h, w, rng, n_faint=int(6500 * s), n_bright=rng.i(4, 9), gain=0.9, density_mask=thin, tint=pal[2])
    haze = blur(dens, 70 * k)[..., None] * (np.array(pal[1], np.float32) * 0.5 + np.array(pal[2], np.float32) * 0.5) * 0.55
    img = bloom(img, gains=(0.28, 0.26, 0.24), sigmas=(6 * k, 20 * k, 60 * k)) + haze + sf
    return img


def galaxy(seed, w=W, h=H):
    rng = Rng(seed)
    cx, cy = w * rng.f(0.36, 0.64), h * rng.f(0.34, 0.5)
    inc = rng.f(0.35, 0.62)                         # cos(inclination): 1 is face on
    rot = rng.f(0, np.pi)
    x, y = grid(h, w)
    dx, dy = x - cx, y - cy
    c, s_ = np.cos(rot), np.sin(rot)
    u, v = dx * c + dy * s_, (-dx * s_ + dy * c) / inc
    r = np.sqrt(u * u + v * v) + 1e-3
    th = np.arctan2(v, u)
    R0 = w * rng.f(0.40, 0.56)
    arms = rng.pick([2, 2, 3, 4])
    pitch = rng.f(0.22, 0.38)
    wind = np.log(r / (R0 * 0.1) + 1e-3) / pitch
    arm = 0.5 + 0.5 * np.cos(arms * (th - wind))
    arm = arm ** rng.f(2.2, 3.6)
    clump = np.clip(0.55 + 0.7 * fbm(h, w, rng, 2.2) + 0.4 * fbm(h, w, rng, 1.6), 0, 1.6)
    disk = np.exp(-r / (R0 * 0.42))
    bulge = np.exp(-(r / (R0 * 0.11)) ** 0.6 * 1.9) * 1.8
    lum = disk * (0.18 + arm * clump * 1.5) * smoothstep(R0 * 1.25, R0 * 0.3, r)
    # Dust lanes hug the inner edge of each arm.
    lane = np.clip(0.5 + 0.5 * np.cos(arms * (th - wind) - 0.5), 0, 1) ** 4 * smoothstep(0, R0 * 0.25, r) * clump
    young = ramp(np.clip(r / R0, 0, 1) ** 0.8, [(0, (1.0, 0.78, 0.45)), (0.3, (0.62, 0.72, 1.0)), (0.7, (0.38, 0.52, 1.0)), (1, (0.3, 0.42, 0.95))])
    old = np.array([1.0, 0.84, 0.58], np.float32)
    img = lum[..., None] * young * 0.85
    img += bulge[..., None] * old * 0.6
    img *= (1.0 - 0.75 * lane)[..., None]
    # Pink star-forming knots strung along the arms.
    knots = np.clip(fbm(h, w, rng, 1.2) - 0.55, 0, 1) * arm * disk * 8
    img += knots[..., None] * np.array([1.0, 0.25, 0.5], np.float32) * 1.1
    img = bloom(img, gains=(0.35, 0.3, 0.25), sigmas=(4, 14, 45))
    # Faint distant galaxies as little smudges.
    far = np.zeros_like(img)
    for _ in range(rng.i(14, 30)):
        gx, gy = rng.uniform(40, w - 40), rng.uniform(40, h - 40)
        a_, b_ = rng.uniform(2, 7), rng.uniform(1, 3)
        ang = rng.uniform(0, np.pi)
        X = (x[int(gy) - 30:int(gy) + 30, int(gx) - 30:int(gx) + 30] - gx)
        Y = (y[int(gy) - 30:int(gy) + 30, int(gx) - 30:int(gx) + 30] - gy)
        p = (X * np.cos(ang) + Y * np.sin(ang)) / a_
        q = (-X * np.sin(ang) + Y * np.cos(ang)) / b_
        patch = np.exp(-(p * p + q * q)) * rng.uniform(0.15, 0.6)
        far[int(gy) - 30:int(gy) + 30, int(gx) - 30:int(gx) + 30] += patch[..., None] * rng.pick([[1, .85, .6], [.7, .8, 1], [1, .7, .7]])
    return img + far * 0.8 + stars(h, w, rng, n_faint=2600, n_bright=14, gain=0.9)




# ---------- bodies ----------

def _sphere(h, w, cx, cy, R):
    x, y = grid(h, w)
    nx = (x - cx) / R
    ny = (y - cy) / R
    r2 = nx * nx + ny * ny
    inside = r2 < 1.0
    nz = np.sqrt(np.clip(1.0 - r2, 0, 1))
    return nx, ny, nz, inside, np.sqrt(r2)


GIANTS = {
    "jupiter": [(0.0, (0.36, 0.20, 0.12)), (0.25, (0.62, 0.38, 0.22)), (0.5, (0.90, 0.78, 0.60)), (0.75, (0.78, 0.55, 0.38)), (1.0, (0.95, 0.88, 0.76))],
    "saturn":  [(0.0, (0.55, 0.42, 0.22)), (0.4, (0.80, 0.66, 0.40)), (0.7, (0.93, 0.82, 0.58)), (1.0, (0.98, 0.92, 0.74))],
    "neptune": [(0.0, (0.05, 0.12, 0.40)), (0.45, (0.12, 0.32, 0.78)), (0.8, (0.32, 0.58, 0.95)), (1.0, (0.62, 0.82, 1.0))],
    "violet":  [(0.0, (0.20, 0.10, 0.36)), (0.45, (0.46, 0.28, 0.70)), (0.8, (0.78, 0.58, 0.90)), (1.0, (0.95, 0.82, 0.96))],
    "ember":   [(0.0, (0.28, 0.06, 0.04)), (0.45, (0.65, 0.20, 0.10)), (0.8, (0.92, 0.48, 0.22)), (1.0, (0.99, 0.80, 0.55))],
}


def planet(seed, w=W, h=H, kind=None):
    rng = Rng(seed)
    kind = kind or rng.pick(list(GIANTS))
    k = w / 1080.0
    R = w * rng.f(0.36, 0.5)
    cx, cy = w * rng.f(0.42, 0.58), h * rng.f(0.38, 0.6)
    nx, ny, nz, inside, rr = _sphere(h, w, cx, cy, R)

    # Spin axis tilted a little; band latitude comes from the tilted y axis.
    tilt = rng.f(-0.35, 0.35)
    ct, st = np.cos(tilt), np.sin(tilt)
    px = nx * ct + ny * st
    py = -nx * st + ny * ct
    lat = np.arcsin(np.clip(py, -1, 1))
    lon = np.arctan2(px, nz)

    # Equirectangular noise, stretched along longitude so it forms jet streams.
    th, tw = 2048, 4096
    streak = fbm(th, tw, rng, 2.6, (24.0, 1.0))
    swirl = fbm(th, tw, rng, 3.0, (5.0, 1.0))
    fine = fbm(th, tw, rng, 2.2, (16.0, 1.0))
    tex_b = fbm(th, tw, rng, 3.0, (6.0, 1.0))
    u = (lon / (2 * np.pi) + 0.5) * tw
    v = (lat / np.pi + 0.5) * th
    def samp(m):
        return ndi.map_coordinates(m, np.array([v, u]), order=1, mode="wrap")
    f1, f2 = rng.f(10, 15), rng.f(26, 40)
    p1 = lat * f1 + 1.3 * samp(swirl) + 0.35 * samp(tex_b)
    p2 = lat * f2 + 2.2 * samp(swirl) + 0.8 * samp(streak)
    band = 0.55 + 0.30 * np.sin(p1) + 0.17 * np.sin(p2)
    t = np.clip(band + 0.30 * samp(streak) + 0.20 * samp(fine) * (0.5 + 0.5 * np.sin(p1)), 0, 1)
    col = ramp(t, GIANTS[kind])

    # A storm: a vortex that bends the bands around it.
    sx, sy = rng.f(-0.25, 0.25), rng.f(0.15, 0.4)
    d = np.sqrt((px - sx) ** 2 + ((py - sy) * 1.7) ** 2)
    storm = np.exp(-(d / 0.11) ** 2)
    col = col * (1 - storm[..., None] * 0.55) + storm[..., None] * np.array(GIANTS[kind][1][1], np.float32) * 0.9

    # Light from the upper left, partly toward the viewer: a lit face with a soft terminator.
    L = np.array([rng.f(-0.9, -0.55), rng.f(-0.55, -0.2), rng.f(0.25, 0.6)], np.float32)
    L /= np.linalg.norm(L)
    ndl = nx * L[0] + ny * L[1] + nz * L[2]
    lit = np.clip((ndl + 0.30) / 1.30, 0, 1) ** 1.35
    limb = 0.55 + 0.45 * nz ** 0.6
    night = 0.035                                   # faint glow so the dark side still reads as a disc
    surf = col * ((lit * 1.45 + night) * limb)[..., None]
    atm_col = np.array(GIANTS[kind][3][1], np.float32) * 0.5 + np.array([0.5, 0.7, 1.0], np.float32) * 0.5
    rim = (np.clip(1 - nz, 0, 1) ** 3.2 * np.clip(ndl + 0.35, 0, 1))[..., None] * atm_col * 1.3
    body = np.where(inside[..., None], surf + rim, 0)

    img = body.copy()
    # Atmosphere haze just outside the limb.
    out = ~inside
    glow = np.exp(-(rr - 1.0) / 0.018) * out * np.clip(ndl_edge(nx, ny, L) + 0.4, 0, 1)
    img += glow[..., None] * atm_col * 0.7

    # Rings.
    if kind in ("saturn", "jupiter", "violet") or rng.random() < 0.3:
        img = rings(img, h, w, cx, cy, R, rng, L, inside, nx, ny, kind)
    sf = stars(h, w, rng, n_faint=3200, n_bright=rng.i(3, 7), gain=0.85)
    mask = (~inside).astype(np.float32)[..., None]
    img = img + sf * mask
    return bloom(img, gains=(0.18, 0.16), sigmas=(5 * k, 22 * k))


def ndl_edge(nx, ny, L):
    r = np.sqrt(nx * nx + ny * ny) + 1e-6
    return (nx / r) * L[0] + (ny / r) * L[1]


def rings(img, h, w, cx, cy, R, rng, L, inside, nx, ny, kind):
    tau = rng.f(0.16, 0.34)                    # how open the rings are: small is nearly edge-on
    roll = rng.f(-0.28, 0.28)
    x, y = grid(h, w)
    dx, dy = (x - cx) / R, (y - cy) / R
    c, s = np.cos(-roll), np.sin(-roll)
    a = dx * c - dy * s
    b = (dx * s + dy * c) / np.sin(tau)
    rho = np.sqrt(a * a + b * b)
    r0, r1 = rng.f(1.28, 1.5), rng.f(2.2, 2.7)
    band = smoothstep(r0, r0 + 0.02, rho) * (1 - smoothstep(r1 - 0.03, r1, rho))
    n1 = fbm(1, 4096, rng, 1.1)[0] + 0.6 * fbm(1, 4096, rng, 0.5)[0]
    prof = np.interp(((rho - r0) / (r1 - r0)).clip(0, 1) * 4095, np.arange(4096), n1)
    gap = 1 - 0.9 * np.exp(-((rho - (r0 + (r1 - r0) * rng.f(0.52, 0.62))) / 0.018) ** 2)
    dens = band * np.clip(0.5 + 0.75 * prof, 0.03, 1) * gap
    ring_col = ramp(np.clip((rho - r0) / (r1 - r0), 0, 1), [(0, (0.62, 0.50, 0.36)), (0.35, (0.80, 0.68, 0.48)), (0.7, (0.74, 0.62, 0.44)), (1, (0.45, 0.38, 0.32))])
    # Planet's shadow falls across the ring on the far side from the light.
    Pz = b * np.cos(tau)
    Px, Py = a, b * np.sin(tau)
    # undo the roll for 3D shading
    PXr = Px * np.cos(roll) - Py * np.sin(roll)
    PYr = Px * np.sin(roll) + Py * np.cos(roll)
    pl = PXr * L[0] + PYr * L[1] + Pz * L[2]
    disc = pl * pl - (PXr ** 2 + PYr ** 2 + Pz ** 2 - 1.0)
    shadow = ((disc > 0) & (pl < 0)).astype(np.float32)
    shadow = blur(shadow, 3)
    ring_lit = (1 - 0.92 * shadow)
    ring = ring_col * (dens * ring_lit * 0.95)[..., None]
    in_front = (Pz > 0)
    behind_planet = inside & (~in_front)
    ring_vis = np.where(behind_planet[..., None], 0, ring)
    out = img.copy()
    # Front of the ring over the planet, back of the ring over empty space, nothing behind the disc.
    alpha = np.clip(dens * 1.3, 0, 1)[..., None]
    out = np.where(in_front[..., None] | (~inside)[..., None], out * (1 - alpha) + ring_vis * 1.0, out)
    return out


def moon_body(h, w, cx, cy, R, rng, k, ph=None, ang=None):
    """A lit, cratered moon of radius R on an (h, w) canvas. Returns (rgb, inside mask, radius map)."""
    nx, ny, nz, inside, rr = _sphere(h, w, cx, cy, R)

    # Height field: maria are smooth, highlands are cratered.
    hgt = fbm(h, w, rng, 2.8) * 0.03
    maria = smoothstep(0.02, 0.38, fbm(h, w, rng, 3.6, kmin=3.0) * 0.9 + fbm(h, w, rng, 3.0, kmin=4.0) * 0.35)
    n_craters = int(2600 * k * k + 900)
    sizes = (rng.random(n_craters) ** 4.2) * 80 * k + 1.8 * k
    cxs = rng.uniform(cx - R, cx + R, n_craters)
    cys = rng.uniform(cy - R, cy + R, n_craters)
    for xi, yi, r in zip(cxs, cys, sizes):
        if (xi - cx) ** 2 + (yi - cy) ** 2 > (R * 1.02) ** 2:
            continue
        mu = maria[int(np.clip(yi, 0, h - 1)), int(np.clip(xi, 0, w - 1))]
        if mu > 0.5 and r < 30 * k and rng.random() < 0.85:
            continue
        ext = int(r * 2.3) + 2
        x0, x1 = int(max(xi - ext, 0)), int(min(xi + ext, w))
        y0, y1 = int(max(yi - ext, 0)), int(min(yi + ext, h))
        if x1 <= x0 or y1 <= y0:
            continue
        gx, gy = np.meshgrid(np.arange(x0, x1) - xi, np.arange(y0, y1) - yi)
        d = np.sqrt(gx * gx + gy * gy) / r
        depth = 0.30 * min(r / (20 * k), 1.0) * 0.5 + 0.04
        bowl = -depth * np.clip(1 - d * d, 0, None)
        rim = depth * 0.30 * np.exp(-((d - 1.02) / 0.22) ** 2)
        eject = 0.04 * depth * np.exp(-np.clip(d - 1, 0, None) / 0.9) * (d > 1)
        hgt[y0:y1, x0:x1] += (bowl + rim + eject).astype(np.float32) * (r * 0.09)

    gy_, gx_ = np.gradient(hgt)
    n = np.stack([nx - gx_ * 1.25 * 3.2, ny - gy_ * 1.25 * 3.2, nz], -1)
    n /= np.linalg.norm(n, axis=-1, keepdims=True) + 1e-6
    ph = rng.f(0.1, 0.95) if ph is None else ph      # phase: 0 new, 1 full
    ang = rng.f(-2.7, -0.6) if ang is None else ang
    L = np.array([np.cos(ang) * (1 - ph * 0.55), np.sin(ang) * (1 - ph * 0.55), 0.25 + ph * 0.75], np.float32)
    L /= np.linalg.norm(L)
    ndl = np.clip((n * L).sum(-1), 0, 1)
    albedo = (0.44 - 0.22 * maria + 0.06 * fbm(h, w, rng, 2.0) + 0.04 * fbm(h, w, rng, 1.4))
    tint = ramp(maria, [(0, (1.0, 0.98, 0.93)), (1, (0.88, 0.92, 1.0))])
    body = tint * (albedo * (ndl * 0.95 + 0.015))[..., None]
    return np.where(inside[..., None], body, 0), inside, rr, ph


def moon(seed, w=W, h=H):
    rng = Rng(seed)
    k = w / 1080.0
    R = w * rng.f(0.40, 0.5)
    cx, cy = w * rng.f(0.38, 0.62), h * rng.f(0.3, 0.5)
    body, inside, rr, ph = moon_body(h, w, cx, cy, R, rng, k)
    sf = stars(h, w, rng, n_faint=3000, n_bright=rng.i(2, 5), gain=0.85)
    img = body + sf * (~inside)[..., None].astype(np.float32)
    img += (np.exp(-np.clip(rr - 1, 0, None) / 0.04) * (~inside) * 0.05 * ph)[..., None] * np.array([0.9, 0.93, 1.0], np.float32)
    return bloom(img, gains=(0.08, 0.06), sigmas=(5 * k, 25 * k))


GENERATORS = {"nebula": nebula, "galaxy": galaxy, "planet": planet, "moon": moon}
