# Zamoled for Windows

A desktop window around the same wallpaper generators the phone app uses (`app/.../gen`), drawn at the shape and size of your monitor.

The generators draw with `android.graphics`, so the app runs them on Robolectric's native graphics (the same Skia the phone uses). Every picture matches the phone. It is packaged with `jpackage`, with its own trimmed Java runtime; no Java install is needed to run it.

## Build (any OS)
```bash
mkdir -p deps
curl -L -o deps/android-all-instrumented-15-robolectric-13954326-i7.jar \
  https://maven-central.storage-download.googleapis.com/maven2/org/robolectric/android-all-instrumented/15-robolectric-13954326-i7/android-all-instrumented-15-robolectric-13954326-i7.jar
./gradlew test       # draws a few wallpapers and checks they are not blank
./gradlew distZip    # build/release/Zamoled-Windows-x64.zip (on Windows; a Linux/macOS app image elsewhere)
```
`-Dzamoled.all=1` with `test` draws every style to `build/smoke/all` for a look over the whole catalogue. Running the app with `-Dzamoled.shot=out.png` saves a picture of the window and quits.
