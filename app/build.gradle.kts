plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.beerdeal.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.beerdeal.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }
}

dependencies {
    // Platform-only UI for now (no androidx) — keeps the app buildable in
    // network-restricted environments where Google's Maven repo is blocked.
    // Re-add androidx/material when camera + OCR (roadmap #1) lands.
    implementation(project(":core"))
}
