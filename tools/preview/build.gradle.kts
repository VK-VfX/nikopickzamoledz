plugins { kotlin("jvm") version "2.1.0" }
repositories { mavenCentral() }
kotlin { jvmToolchain(21) }
sourceSets {
    main { kotlin.srcDir("../../app/src/main/java/com/nikopick/zamoled/gen") }
}
configurations.all {
    // These live only on Google Maven (blocked here) and are not needed to draw bitmaps.
    exclude(group = "androidx.test")
    exclude(group = "androidx.test.ext")
    exclude(group = "androidx.annotation")
    exclude(group = "androidx.lifecycle")
    exclude(group = "androidx.core")
    exclude(group = "androidx.test.espresso")
    exclude(group = "androidx.annotation")
    exclude(group = "androidx.arch.core")
    exclude(group = "androidx.versionedparcelable")
    exclude(group = "androidx.collection")
    exclude(group = "androidx.concurrent")
    exclude(group = "androidx.tracing")
    exclude(group = "androidx.window")
}
dependencies {
    compileOnly("org.robolectric:android-all:15-robolectric-13954326")
    testImplementation("org.robolectric:android-all:15-robolectric-13954326")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17")
}
tasks.test {
    useJUnit()
    systemProperty("preview.out", "$projectDir/out")
    systemProperty("preview.only", System.getProperty("preview.only") ?: "")
    systemProperty("preview.seed", System.getProperty("preview.seed") ?: "1")
    systemProperty("preview.palette", System.getProperty("preview.palette") ?: "auto")
    systemProperty("preview.layout", System.getProperty("preview.layout") ?: "auto")
    testLogging { showStandardStreams = true }
    maxHeapSize = "3g"
    systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
    systemProperty("robolectric.offline", "true")
    systemProperty("robolectric.dependency.dir", System.getenv("ROBO_DEPS") ?: "$projectDir/deps")
}
