"""Quick look: python3 preview.py <module> <generator> <scale> <seed...>  ->  /tmp/claude-0/art/"""
import importlib, sys
from artlib import *

mod = importlib.import_module(sys.argv[1])
fn = mod.GENERATORS[sys.argv[2]]
scale = float(sys.argv[3])
for sd in sys.argv[4:]:
    im = fn(int(sd), int(W * scale), int(H * scale))
    out = finish(im, f"/tmp/claude-0/art/{sys.argv[2]}_{sd}.webp", Rng(int(sd) + 99))
    print(sys.argv[2], sd, "black %.1f%%" % (100 * black_ratio(out)))
