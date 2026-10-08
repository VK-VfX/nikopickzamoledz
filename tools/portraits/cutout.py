import sys, os, numpy as np, cv2
import mediapipe as mp
from PIL import Image
from pymatting import estimate_alpha_cf, estimate_foreground_ml

def seg_mask(rgb):
    """Average of two bundled MediaPipe person models, soft 0..1 mask at image size."""
    h, w = rgb.shape[:2]
    with mp.solutions.selfie_segmentation.SelfieSegmentation(model_selection=0) as s:
        m1 = s.process(rgb).segmentation_mask
    with mp.solutions.pose.Pose(static_image_mode=True, model_complexity=2, enable_segmentation=True) as p:
        r = p.process(rgb)
    m2 = r.segmentation_mask if r.segmentation_mask is not None else m1
    return np.clip((m1 + m2) / 2.0, 0, 1).astype(np.float32), m1, m2

def trimap_from(mask, r_fg=9, r_bg=12):
    fg = (mask > 0.75).astype(np.uint8)
    bg = (mask < 0.25).astype(np.uint8)
    kf = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (2 * r_fg + 1, 2 * r_fg + 1))
    kb = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (2 * r_bg + 1, 2 * r_bg + 1))
    sure_fg = cv2.erode(fg, kf)
    sure_bg = cv2.erode(bg, kb)
    tri = np.full(mask.shape, 0.5, np.float32)
    tri[sure_fg > 0] = 1.0
    tri[sure_bg > 0] = 0.0
    return tri

def cutout(path):
    img = np.array(Image.open(path).convert('RGB'))
    h, w = img.shape[:2]
    mask, m1, m2 = seg_mask(img)
    tri = trimap_from(mask)
    x = img.astype(np.float64) / 255.0
    alpha = estimate_alpha_cf(x, tri.astype(np.float64))
    alpha = np.clip(alpha, 0, 1)
    fg = estimate_foreground_ml(x, alpha)
    # Keep only the large connected pieces (drops stray bits of background).
    binm = (alpha > 0.5).astype(np.uint8)
    n, lab, stats, _ = cv2.connectedComponentsWithStats(binm, connectivity=8)
    keep = np.zeros(n, bool)
    if n > 1:
        big = stats[1:, cv2.CC_STAT_AREA].max()
        for i in range(1, n):
            if stats[i, cv2.CC_STAT_AREA] > max(0.12 * big, 0.004 * h * w): keep[i] = True
    region = keep[lab]
    region = cv2.dilate(region.astype(np.uint8), np.ones((15, 15), np.uint8)) > 0
    alpha = alpha * region
    return img, np.clip(fg, 0, 1), alpha, mask

if __name__ == '__main__':
    for name in sys.argv[1:]:
        img, fg, alpha, mask = cutout(f'src/{name}.jpg')
        rgb = (fg * alpha[..., None] * 255).astype(np.uint8)       # premultiplied onto black
        Image.fromarray(rgb).save(f'out/{name}_black.png')
        rgba = np.dstack([(fg * 255).astype(np.uint8), (alpha * 255).astype(np.uint8)])
        Image.fromarray(rgba, 'RGBA').save(f'out/{name}_rgba.png')
        Image.fromarray((mask * 255).astype(np.uint8)).save(f'out/{name}_mask.png')
        print('done', name, 'alpha coverage %.2f' % (alpha > 0.5).mean())
