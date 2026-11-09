pluginManagement {
    repositories {
        // Maven Central is rate-limited per IP here; Google's mirror of it is not.
        maven("https://maven-central.storage-download.googleapis.com/maven2")
        gradlePluginPortal()
    }
}
rootProject.name = "preview"
