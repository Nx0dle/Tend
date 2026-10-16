plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.tend.wear"
    compileSdk = 36

    defaultConfig {
        // Must match the phone applicationId so the Wear Data Layer can share data
        // (GMS requires the same package name and signature on both devices).
        applicationId = "com.tend.app"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.12.00")
    implementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")

    implementation(project(":core"))

    // Round-screen list that scales towards the edges and handles the rotating/virtual bezel
    // (including the per-item haptic tick) out of the box.
    implementation("androidx.wear.compose:compose-foundation:1.6.2")

    // Phone -> watch data channel.
    implementation("com.google.android.gms:play-services-wearable:20.0.1")

    // Quick-view Tile ("widget").
    implementation("androidx.wear.tiles:tiles:1.6.2")
    implementation("androidx.concurrent:concurrent-futures:1.3.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
