"""
Renders every wallpaper image into the app's assets.

    python3 build_all.py              # render what is missing
    python3 build_all.py --force      # re-render everything
    python3 build_all.py nebula moon  # only these styles

Each entry is (style id used by the app, module, generator, seeds). The seeds are curated: a seed
that gives a poor picture is simply left out of the list.
"""
import importlib
import os
import sys
import time

from artlib import W, H, Rng, finish, black_ratio

OUT = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets", "art")

MANIFEST = [
    # id,            module,     generator, seeds
    ("art_nebula",   "space",    "nebula",  [1, 2, 3, 4, 5, 6, 7, 8]),
    ("art_galaxy",   "space",    "galaxy",  [1, 2, 3, 4, 5, 6]),
    ("art_giant",    "space",    "planet",  [1, 2, 3, 4, 5, 6]),
    ("art_moon",     "space",    "moon",    [1, 2, 3, 4, 5]),
    ("art_aurora",   "nature",   "aurora",  [1, 2, 3, 4, 5, 6]),
    ("art_milkyway", "nature",   "milkyway", [1, 2, 3, 4]),
    ("art_moonlit",  "nature",   "moonlit", [1, 2, 3, 4]),
    ("art_ink",      "abstract", "ink",     [1, 2, 3, 4, 5, 6, 7, 8]),
    ("art_bokeh",    "light",    "bokeh",   [1, 2, 3, 4, 5, 6]),
    ("art_highway",  "light",    "trails",  [1, 2, 3, 4, 5, 6]),
]


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    force = "--force" in sys.argv
    total = 0
    for style, mod, gen, seeds in MANIFEST:
        if args and not any(a in style for a in args):
            continue
        fn = importlib.import_module(mod).GENERATORS[gen]
        folder = os.path.join(OUT, style)
        os.makedirs(folder, exist_ok=True)
        for n, seed in enumerate(seeds, 1):
            path = os.path.join(folder, "%02d.webp" % n)
            if os.path.exists(path) and not force:
                continue
            t0 = time.time()
            out = finish(fn(seed, W, H), path, Rng(seed + 99))
            size = os.path.getsize(path) // 1024
            total += size
            print("%-14s %02d seed=%-3d black=%4.1f%%  %4d KB  %.0fs" % (style, n, seed, 100 * black_ratio(out), size, time.time() - t0), flush=True)
    print("rendered %.1f MB" % (total / 1024))


if __name__ == "__main__":
    main()
