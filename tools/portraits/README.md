# Portrait pipeline

Turns generated portrait photos into the AMOLED wallpapers bundled in `app/src/main/assets/portraits/`.

1. `cutout.py`: finds the person with MediaPipe's bundled segmentation models, then refines the edge (hair) with
   closed-form alpha matting (`pymatting`) and removes background colour spill. Writes `out/<name>_rgba.png`.
2. `compose.py`: keeps only the subject (face-based), applies hand-placed clean-ups (`ERASE`, `BOKEH`, `HALO`),
   cuts everything below row 1030 and fades the bottom to transparent (this also removes a corner watermark),
   places the figure on a 1080x2400 canvas and exports lossy WebP with alpha plus the head position used
   for the halo in `Portraits.kt`.

## Setup
MediaPipe's legacy `solutions` (and its bundled models) need an older build, so use Python 3.12:

```bash
python3.12 -m venv venv && ./venv/bin/pip install "mediapipe==0.10.21" pymatting opencv-python-headless pillow scipy
mkdir -p src out final   # put source JPEGs in src/ as pNN.jpg (768x1152 works well)
./venv/bin/python cutout.py p01 p02 ...
./venv/bin/python compose.py p01 p02 ...
```
Only use images you have the right to publish. Source files are not committed.
