# Wallpaper preview (desktop)

Renders the real wallpaper generators in `app/.../gen` to PNG files on a normal JVM, so you can look at the
art without an Android phone or SDK. It uses Robolectric's native graphics (the same Skia the phone uses).

## One-time setup
Download the Android 15 framework jar next to this file (about 200 MB, not committed):

```bash
mkdir -p deps
curl -L -o deps/android-all-instrumented-15-robolectric-13954326-i7.jar \
  https://repo.maven.apache.org/maven2/org/robolectric/android-all-instrumented/15-robolectric-13954326-i7/android-all-instrumented-15-robolectric-13954326-i7.jar
```

## Render
```bash
./gradlew test --console=plain -q \
  -Dpreview.only=koi_pond,gatti   # style ids, empty = every style
  -Dpreview.seed=1,2,3            # seeds
  -Dpreview.palette=auto          # palette id
# PNGs land in tools/preview/out/
```
The `src/test/java/androidx/**` files are tiny stand-ins for androidx.test classes that Robolectric expects.
