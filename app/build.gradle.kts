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
        versionCode = 5
        versionName = "1.4.0"
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

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.graphics:graphics-shapes:1.0.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
