import sys, os, json, numpy as np, cv2
import mediapipe as mp
from PIL import Image, ImageFilter

CW, CH = 1080, 2400          # wallpaper canvas
CUT_Y = 1030                 # everything below this row is dropped (the logo lives at 1040+)
FADE_END = 1000              # alpha reaches zero here
FADE_LEN = 250

def face_center(img_rgb):
    with mp.solutions.face_detection.FaceDetection(model_selection=1, min_detection_confidence=0.4) as fd:
        r = fd.process(img_rgb)
    if not r.detections: return None
    d = max(r.detections, key=lambda d: d.location_data.relative_bounding_box.width)
    b = d.location_data.relative_bounding_box
    h, w = img_rgb.shape[:2]
    return (b.xmin + b.width / 2) * w, (b.ymin + b.height / 2) * h, b.width * w

def keep_subject(alpha, face):
    binm = (alpha > 0.5).astype(np.uint8)
    n, lab, stats, _ = cv2.connectedComponentsWithStats(binm, connectivity=8)
    if n <= 1: return alpha
    target = 0
    if face is not None:
        fx, fy = int(face[0]), int(face[1])
        target = lab[min(max(fy, 0), lab.shape[0] - 1), min(max(fx, 0), lab.shape[1] - 1)]
    if target == 0:
        target = 1 + int(np.argmax(stats[1:, cv2.CC_STAT_AREA]))
    region = (lab == target).astype(np.uint8)
    region = cv2.dilate(region, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (13, 13))) > 0
    return alpha * region

# Hand-placed clean-ups in source pixel coordinates: polygons to erase, and bokeh lights to remove.
ERASE = {
    'p01': [[(0, 860), (42, 860), (42, 1152), (0, 1152)], [(0, 956), (340, 956), (340, 1152), (0, 1152)]],
    'p03': [[(190, 540), (217, 540), (217, 745), (190, 745)], [(283, 650), (293, 650), (293, 772), (283, 772)]],
    'p12': [[(0, 640), (205, 640), (205, 745), (185, 800), (160, 830), (140, 880), (0, 880)], [(500, 838), (768, 838), (768, 1152), (500, 1152)]],
}
BOKEH = {'p04': [(255, 346, 34)]}
HALO = {'p04': [(255, 346, 80)]}   # drop translucent glow around a removed light

def clean(name, rgb, alpha):
    h, w = alpha.shape
    for poly in ERASE.get(name, []):
        m = np.zeros((h, w), np.uint8); cv2.fillPoly(m, [np.array(poly, np.int32)], 1)
        alpha = alpha * (1 - m)
    for (cx, cy, r) in BOKEH.get(name, []):
        hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV).astype(np.float32)
        yy, xx = np.ogrid[:h, :w]
        inside = (xx - cx) ** 2 + (yy - cy) ** 2 < r * r
        pale = (hsv[..., 1] < 0.55 * 255) & (hsv[..., 2] > 0.6 * 255)
        alpha = alpha * (1 - (inside & pale).astype(np.float32))
    for (cx, cy, r) in HALO.get(name, []):
        yy, xx = np.ogrid[:h, :w]
        inside = (xx - cx) ** 2 + (yy - cy) ** 2 < r * r
        alpha = np.where(inside & (alpha < 0.7), 0, alpha)
    return alpha

def smooth(t):
    t = np.clip(t, 0, 1)
    return t * t * (3 - 2 * t)

def compose(name, outdir):
    src = np.array(Image.open(f'src/{name}.jpg').convert('RGB'))
    rgba = np.array(Image.open(f'out/{name}_rgba.png'))
    rgb, alpha = rgba[..., :3], rgba[..., 3].astype(np.float32) / 255
    face = face_center(src)
    alpha = keep_subject(alpha, face)
    alpha = clean(name, src, alpha)
    h, w = alpha.shape
    ys = np.arange(h, dtype=np.float32)[:, None]
    fade = 1.0 - smooth((ys - (FADE_END - FADE_LEN)) / FADE_LEN)
    fade[ys >= CUT_Y] = 0
    alpha = alpha * fade
    # Bounding box of the visible subject.
    cols = np.where((alpha > 0.25).any(0))[0]; rows = np.where((alpha > 0.25).any(1))[0]
    x0, x1, y_top = cols.min(), cols.max(), rows.min()
    bw = x1 - x0 + 1
    s = float(np.clip(min(1.62, 1050.0 / bw), 1.2, 1.62))
    # Larger images are placed higher so the fade ends at the bottom of the canvas.
    oy = CH - FADE_END * s
    head_top = oy + y_top * s
    if head_top < 0.22 * CH: oy += 0.22 * CH - head_top
    cx_src = (x0 + x1) / 2
    ox = CW / 2 - cx_src * s
    ox = min(max(ox, CW - w * s - 20), 20) if w * s > CW else ox
    big = Image.fromarray(np.dstack([rgb, (alpha * 255).astype(np.uint8)]), 'RGBA').resize((int(w * s), int(h * s)), Image.LANCZOS)
    # Light sharpening on colour only; alpha untouched.
    c = big.convert('RGB').filter(ImageFilter.UnsharpMask(radius=1.3, percent=70, threshold=2))
    a = big.getchannel('A')
    canvas = Image.new('RGBA', (CW, CH), (0, 0, 0, 0))
    layer = c.convert('RGBA'); layer.putalpha(a)
    canvas.alpha_composite(layer, (int(round(ox)) if ox >= 0 else 0, int(round(oy))) if ox >= 0 else (0, int(round(oy))), ) if ox >= 0 else None
    if ox < 0:
        crop = layer.crop((int(-ox), 0, int(-ox) + CW, layer.height))
        canvas.alpha_composite(crop, (0, int(round(oy))))
    # Preview on pure black.
    prev = Image.new('RGB', (CW, CH), (0, 0, 0)); prev.paste(canvas, (0, 0), canvas)
    prev.save(f'{outdir}/{name}_prev.png')
    canvas.save(f'{outdir}/{name}.webp', 'WEBP', quality=88, alpha_quality=100, method=6)
    # Head position for the glow behind the head, as fractions of the canvas.
    top_row = int(y_top); band = alpha[top_row:top_row + 70]
    xs = np.where((band > 0.3).any(0))[0]
    hx = (xs.mean() * s + ox) / CW if len(xs) else 0.5
    hy = (oy + (y_top + 150) * s) / CH
    hr = 0.17 * w * s / CW
    return dict(asset=name, head_x=round(float(hx), 3), head_y=round(float(hy), 3), head_r=round(float(hr), 3), scale=round(s, 2))

if __name__ == '__main__':
    outdir = 'final'; os.makedirs(outdir, exist_ok=True)
    meta = [compose(n, outdir) for n in sys.argv[1:]]
    json.dump(meta, open(f'{outdir}/meta_{sys.argv[1]}.json', 'w'))
    for m in meta: print(m, os.path.getsize(f"{outdir}/{m['asset']}.webp") // 1024, 'KB')
