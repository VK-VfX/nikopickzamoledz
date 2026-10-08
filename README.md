# Zamoled

An Android wallpaper app that **generates** pitch-black AMOLED wallpapers on your phone. It's built with Kotlin, Jetpack Compose and Material 3. You don't download images: every wallpaper is drawn from a seed, so you get an endless supply, rendered sharp at any resolution up to 4K.

## Features

- **21 generators in 7 categories**
  | Category  | Styles |
  |-----------|--------|
  | Doodle    | Doodle Scatter, Doodle Pattern, Doodle Spotlight (14 hand-drawn icons: stars, hearts, planets, bolts, clouds, moons, notes…) |
  | Minimal   | Halo, Horizon, Silk |
  | Geometric | Polygon Tunnel, Dot Matrix, Isometric |
  | Space     | Starfield, Constellations, Eclipse |
  | Nature    | Aurora (northern lights), Topographic, Mountains |
  | Abstract  | Flow Field, Waves, Mandala |
  | Neon      | Neon Shapes, Circuit, Synthwave |
- **True black backgrounds.** Every wallpaper starts from `#000000`, and the editor shows a live **Pitch black %**: the share of pixels your OLED screen can switch off completely.
- **Material 3 / Material You.** It uses the dynamic colour theme (Android 12+) with surfaces forced to true black. There's also a **Material You palette** that tints wallpapers with your system colours.
- **11 palettes** (Mono, Neon Cyan, Sunset, Aurora, Sakura, Lava, Ocean, Acid, Violet, Gold, Material You), or Auto.
- **Detail slider.** Turn the density of stars, doodles, lines and particles up or down.
- **Save** as a lossless PNG at screen size, QHD+ (1440×3200) or 4K (2160×3840). Files go to `Pictures/Zamoled`, and no storage permission is needed.
- **Apply** to the home screen, lock screen or both.
- **Favorites**, **Shuffle** (new seed) and **Surprise me**. The grid loads more as you scroll.
- **Expressive Material 3 UI:** a bottom navigation bar with Explore, Categories and Favorites; a "Today's picks" carousel; category icons in their own morphing shapes; shared-element transitions from thumbnail to full screen; a floating editing toolbar; and spring motion throughout.

## Get the APK

Every push runs the **Build APK** workflow (`.github/workflows/build.yml`):

1. Open the repo's **Actions** tab, then the latest *Build APK* run.
2. Download the `zamoled-apk` artifact. It holds `zamoled-release.apk` (small, optimised) and `zamoled-debug.apk`.
3. Install it on your phone. You may need to allow installs from your browser or file manager.

Each push to the release branch also publishes both APKs as the GitHub Release `v<versionName>` (bump `versionName` in `app/build.gradle.kts` for a new release). Pushing a tag like `v1.0.1` works too.

> The release APK is signed with the standard debug key so you can sideload it. To publish on the Play Store, set up your own signing config.

## Build locally

You need Android Studio (or the Android SDK with platform 35) and JDK 17.

```bash
./gradlew assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

Requires Android 10 (API 29) or newer.

## How it works

`gen/Core.kt` holds the engine. Each style draws on a virtual canvas that is 1000 units wide, which is scaled to the target bitmap. Because of that, the same seed gives the same picture as a 360 px thumbnail and as a 4K export. Randomness comes from `kotlin.random.Random(seed)` plus a seeded Perlin noise. A wallpaper is fully described by `style | seed | palette | detail`, and favourites are stored as that string.

To add a new style, subclass `Style`, draw with the `Scene` helpers (`stroke`, `glowPath`, `glowDot`, `grad`, `noise`…), and register it in `Styles.all`.
