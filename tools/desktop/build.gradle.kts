plugins { kotlin("jvm") version "2.1.0" }

repositories {
    // Maven Central rate-limits the large Robolectric framework jars per IP; Google's mirror of it does not.
    maven("https://maven-central.storage-download.googleapis.com/maven2")
    mavenCentral()
}

kotlin { jvmToolchain(21) }

sourceSets {
    // The wallpaper generators are the very same files the Android app compiles.
    main { kotlin.srcDir("../../app/src/main/java/com/nikopick/zamoled/gen") }
}

configurations.all {
    // These live only on Google Maven and are not needed to draw bitmaps.
    exclude(group = "androidx.test")
    exclude(group = "androidx.test.ext")
    exclude(group = "androidx.annotation")
    exclude(group = "androidx.lifecycle")
    exclude(group = "androidx.core")
    exclude(group = "androidx.test.espresso")
    exclude(group = "androidx.arch.core")
    exclude(group = "androidx.versionedparcelable")
    exclude(group = "androidx.collection")
    exclude(group = "androidx.concurrent")
    exclude(group = "androidx.tracing")
    exclude(group = "androidx.window")
}

val androidAll = "android-all-instrumented-15-robolectric-13954326-i7.jar"

val plainAndroidAll by configurations.creating
val nativeDist by configurations.creating

// The app's own classpath only needs the framework's classes, not its resources; the full
// instrumented copy Robolectric runs on is kept in deps/. Likewise only this OS's native library is kept.
val slimAndroidAll = tasks.register<Jar>("slimAndroidAll") {
    archiveFileName.set("android-all-classes.jar")
    destinationDirectory.set(layout.buildDirectory.dir("slim"))
    from(provider { zipTree(plainAndroidAll.singleFile) }) { include("**/*.class") }
}
val isWindows = org.gradle.internal.os.OperatingSystem.current().isWindows
val isMac = org.gradle.internal.os.OperatingSystem.current().isMacOsX
val slimNative = tasks.register<Jar>("slimNative") {
    archiveFileName.set("nativeruntime-slim.jar")
    destinationDirectory.set(layout.buildDirectory.dir("slim"))
    from(provider { zipTree(nativeDist.singleFile) }) {
        val keep = if (isWindows) "windows" else if (isMac) "mac" else "linux"
        listOf("windows", "mac", "linux").filter { it != keep }.forEach { exclude("native/$it/**") }
    }
}

dependencies {
    compileOnly("org.robolectric:android-all:15-robolectric-13954326")
    implementation("junit:junit:4.13.2")
    implementation("org.robolectric:robolectric:4.17") {
        // Replaced below by copies with only what this machine needs.
        exclude(group = "org.robolectric", module = "nativeruntime-dist-compat")
    }
    "plainAndroidAll"("org.robolectric:android-all:15-robolectric-13954326")
    "nativeDist"("org.robolectric:nativeruntime-dist-compat:1.0.19")
    runtimeOnly(files(slimAndroidAll))
    runtimeOnly(files(slimNative))
}

tasks.test {
    useJUnit()
    maxHeapSize = "3g"
    systemProperty("java.awt.headless", "true")
    systemProperty("robolectric.offline", "true")
    systemProperty("robolectric.dependency.dir", System.getenv("ROBO_DEPS") ?: "$projectDir/deps")
    systemProperty("zamoled.code", "$projectDir/../../app/src/main/java/com/nikopick/zamoled/gen")
    systemProperty("zamoled.all", System.getProperty("zamoled.all") ?: "")
    systemProperty("zamoled.out", "$projectDir/build/smoke")
    testLogging { showStandardStreams = true }
}

// ---- Windows (and Linux/macOS) app image: a launcher, a trimmed Java runtime and the wallpaper code ----

val stage = layout.buildDirectory.dir("stage")
val dist = layout.buildDirectory.dir("dist")

val stageLibs = tasks.register<Sync>("stageLibs") {
    dependsOn(tasks.jar)
    from(tasks.jar)
    from(configurations.runtimeClasspath)
    into(stage)
}

val appImage = tasks.register<Exec>("appImage") {
    dependsOn(stageLibs)
    val out = dist.get().asFile
    doFirst { out.deleteRecursively(); out.mkdirs() }
    commandLine(
        "jpackage",
        "--type", "app-image",
        "--name", "Zamoled",
        "--app-version", "1.0.0",
        "--input", stage.get().asFile.absolutePath,
        "--main-jar", tasks.jar.get().archiveFileName.get(),
        "--main-class", "com.nikopick.zamoled.desktop.Launcher",
        "--dest", out.absolutePath,
        "--add-modules", "java.se,jdk.unsupported,jdk.management,jdk.crypto.ec,jdk.charsets,jdk.zipfs,jdk.localedata,jdk.net,jdk.naming.dns",
        "--verbose",
        "--java-options", "-Xmx3g",
        "--java-options", "-Dfile.encoding=UTF-8",
        "--java-options", "-Drobolectric.offline=true",
        "--java-options", "-Drobolectric.dependency.dir=\$APPDIR/deps",
        "--java-options", "-Dzamoled.home=\$APPDIR",
    )
}

// The framework jar and the generator source (shown by the Source Code wallpapers) are copied in beside the app.
// jpackage keeps the app folder at Zamoled/app on Windows and Zamoled/lib/app elsewhere.
val appFolder = if (org.gradle.internal.os.OperatingSystem.current().isWindows) "app" else "lib/app"

val finishImage = tasks.register<Copy>("finishImage") {
    dependsOn(appImage)
    from(layout.projectDirectory.dir("deps")) { include(androidAll); into("$appFolder/deps") }
    from("../../app/src/main/java/com/nikopick/zamoled/gen") { include("*.kt"); into("$appFolder/code") }
    into(dist.map { it.dir("Zamoled") })
    // A zip without the launcher is worse than no zip: stop here, and say what was found instead.
    doLast {
        val root = dist.get().asFile
        val launcher = File(root, if (isWindows) "Zamoled/Zamoled.exe" else "Zamoled/bin/Zamoled")
        if (!launcher.exists()) {
            throw GradleException("The app image has no launcher at $launcher. Found:\n" + root.walkTopDown().maxDepth(3).take(60).joinToString("\n") { it.relativeTo(root).path })
        }
    }
}

tasks.register<Zip>("distZip") {
    dependsOn(finishImage)
    from(dist.map { it.dir("Zamoled") }) { into("Zamoled") }
    archiveFileName.set("Zamoled-Windows-x64.zip")
    destinationDirectory.set(layout.buildDirectory.dir("release"))
}
