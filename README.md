# Zamoled

An Android wallpaper app that **generates** pitch-black AMOLED wallpapers on your phone. It's built with Kotlin, Jetpack Compose and Material 3. You don't download images: every wallpaper is drawn from a seed, so you get an endless supply, rendered sharp at any resolution up to 4K.

## Features

- **61 wallpapers across 13 worlds**, each with its own art direction
  | World     | Styles |
  |-----------|--------|
  | Doodle    | Doodle Scatter, Doodle Wall, Doodle Spotlight, Doodle Stickers, Gatti and Gatti Sketch (cute cats), Space Doodles (astronaut pets), Doodle Bomb (packed sketchbook monsters) |
  | Minimal   | Japanese ink: Lone Tree (sakura on a cliff), Koi Pond, Moon Fisher |
  | Geometric | Crystal Cave, Art Deco (tower poster), Floating Isle (isometric island with a cottage and waterfall) |
  | Space     | Astronaut over Earth, Ringed World from a cratered moon, Black Hole, Eclipse |
  | Nature    | Aurora, Howling Woods (wolf over misty pines), Mountain Lake (mirrored peaks and a canoe), Night Bloom (monstera, ferns, glowing flowers) |
  | Abstract  | Starry Swirl (a Van Gogh homage), Liquid Marble, Ink Smoke |
  | Neon      | Neon Sign on a brick wall, Neon Alley (Tokyo signs and wet streets), Synthwave |
  | Audio     | Vinyl turntable, Boombox, Studio Mic with an ON AIR sign |
  | Mystic    | The Moon tarot card, All-Seeing Eye, Crystal Ball, Moon Phases |
  | City      | Rooftops (water tower, string lights, a cat on the ledge), Skyline, Neon Rain, Night Highway |
  | Glitch    | Aesthetic (glitched marble bust), Error.exe windows, Data Rain |
  | Pixel     | Pixel Castle, Pixel Quest (hero, slime, chest, HUD), Pixel Space, Pixel Peaks |
  | Light     | Lantern Festival, Candlelight, Jellyfish, Fireflies |
  | Portraits | 14 AI-generated portraits cut out onto pure AMOLED black, each in three looks: natural colour, colour with a halo and rim light, and noir |
- **True black backgrounds.** Every wallpaper starts from `#000000`, and the editor shows a live **Pitch black %**: the share of pixels your OLED screen can switch off completely.
- **Material 3 / Material You.** It uses the dynamic colour theme (Android 12+) with surfaces forced to true black. There's also a **Material You palette** that tints wallpapers with your system colours.
- **14 palettes** (Mono, Neon Cyan, Sunset, Aurora, Sakura, Lava, Ocean, Acid, Violet, Gold, Candy, Graffiti, Hue Shift, Material You), or Auto, which picks palettes that suit each style.
- **Detail slider.** Turn the density of stars, doodles, lines and particles up or down.
- **Save** as a lossless PNG at screen size, QHD+ (1440×3200) or 4K (2160×3840). Files go to `Pictures/Zamoled`, and no storage permission is needed.
- **Apply** to the home screen, lock screen or both.
- **Update banner:** the app checks this repo's latest release and offers a one-tap download when a newer version is out.
- **Favorites**, **Shuffle** (new seed) and **Surprise me**. The grid loads more as you scroll.
- **Expressive Material 3 UI:** a bottom navigation bar with Explore, Categories and Favorites; a "Today's picks" carousel; category icons in their own morphing shapes; shared-element transitions from thumbnail to full screen; a floating editing toolbar; and spring motion throughout.

## Get the APK

Every push runs the **Build APK** workflow (`.github/workflows/build.yml`):

1. Open the repo's **Actions** tab, then the latest *Build APK* run.
2. Download the `zamoled-apk` artifact. It holds `zamoled-release.apk` (small, optimised) and `zamoled-debug.apk`.
3. Install it on your phone. You may need to allow installs from your browser or file manager.

Each push to the release branch also publishes both APKs as the GitHub Release `v<versionName>` (bump `versionName` in `app/build.gradle.kts` for a new release). Pushing a tag like `v1.0.1` works too.

### Signing (so updates install over the old app)

Android only accepts an update when it's signed with the same key as the installed app. CI signs every build with one permanent key, which it reads from two repository secrets (**Settings → Secrets and variables → Actions**):

| Secret | Value |
|---|---|
| `ZAMOLED_KEYSTORE_BASE64` | the keystore file (`.jks`), base64-encoded |
| `ZAMOLED_KEY_PASSWORD` | the keystore and key password (key alias `zamoled`) |

These secrets are optional. Without them, CI creates its own key on the first build and keeps it in the repository's private Actions cache, so builds still share one key and update over each other. Add the secrets if you want a key you control and back up yourself. Note that switching keys later means one more uninstall.

To create a key: `keytool -genkeypair -keystore zamoled.jks -storetype PKCS12 -alias zamoled -keyalg RSA -keysize 2048 -validity 36500`, then `base64 -w0 zamoled.jks`. Keep the `.jks` file and password safe: losing them means users have to uninstall before they can install your next build.

The debug build uses the app id `com.nikopick.zamoled.debug`, so it installs alongside the release build.

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

## Previewing the art on a computer

`tools/preview` renders the generators to PNGs without a phone. See `tools/preview/README.md`.

## Portraits

The Portraits world uses AI-generated photographs, cut out and placed on pure black. They are bundled as WebP files in `app/src/main/assets/portraits/` (about 2 MB in total). See `tools/portraits` for how they are made.
