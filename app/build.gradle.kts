plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.nikopick.zamoled"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nikopick.zamoled"
        minSdk = 29
        targetSdk = 35
        versionCode = 28
        versionName = "6.0.2"
    }

    // A permanent key (from GitHub Secrets in CI) signs every build, so each new APK installs as an
    // update over the last one. Without it, builds fall back to the machine's random debug key.
    val keystorePath: String? = System.getenv("ZAMOLED_KEYSTORE_PATH")
    signingConfigs {
        if (keystorePath != null) {
            create("sideload") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("ZAMOLED_KEY_PASSWORD")
                keyAlias = System.getenv("ZAMOLED_KEY_ALIAS") ?: "zamoled"
                keyPassword = System.getenv("ZAMOLED_KEY_PASSWORD")
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }
    val appSigning = signingConfigs.getByName(if (keystorePath != null) "sideload" else "debug")

    buildTypes {
        debug {
            // Separate app id so the debug build installs next to the release build instead of clashing.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            signingConfig = appSigning
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = appSigning
        }
    }

    // The build adds an encrypted "dependency metadata" block to the APK for Google Play. This app is not
    // distributed through Play, and a block nobody can read is exactly what a scanner learns to distrust.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.graphics:graphics-shapes:1.0.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

// The Source Code wallpaper shows this app's real generator code, so the build copies it into the assets.
val copyCodeAssets by tasks.registering(Copy::class) {
    from("src/main/java/com/nikopick/zamoled/gen") { include("*.kt") }
    into(layout.buildDirectory.dir("generated/codeAssets/code"))
}
android.sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/codeAssets"))
// Every other task may read the assets directory (lint models included), so the copy simply runs first.
tasks.configureEach { if (name != "copyCodeAssets" && name != "clean") dependsOn(copyCodeAssets) }
