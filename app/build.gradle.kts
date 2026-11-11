plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("kotlin-parcelize")
}

android {
    namespace = "com.ghost.video"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.ghost.video"
        minSdk = 24
        targetSdk = 36
        versionCode = 30
        versionName = "3.16"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("${rootProject.projectDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
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

    testOptions {
        unitTests {
            // Plain JVM unit tests (notification action mapping, settings key
            // filtering) plus Robolectric UI tests for the navigation bar.
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }

    configurations.all {
        resolutionStrategy.force(
            "androidx.compose.material3:material3:1.5.0-alpha15",
            "androidx.compose.material3:material3-android:1.5.0-alpha15"
        )
    }

    // Generate per-ABI APKs (arm64-v8a, armeabi-v7a) plus one universal APK that
    // contains all ABIs. This keeps device-specific downloads small while still
    // shipping a universal build for maximum compatibility.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = true
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.01.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3:1.5.0-alpha15")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")

    implementation("androidx.activity:activity-compose:1.12.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")

    implementation("androidx.navigation:navigation-compose:2.9.7")

    implementation("androidx.core:core-ktx:1.15.0")

    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")
    implementation("io.coil-kt.coil3:coil-video:3.3.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    implementation("androidx.datastore:datastore-preferences:1.2.0")

    implementation("androidx.work:work-runtime-ktx:2.10.0")

    implementation("com.github.skydoves:cloudy:0.1.2")

    // Media3 1.11.1 is required by NextLib: its FFmpeg/libass decoders are built
    // against this exact Media3 line, and the player code below is written for its API.
    val media3Version = "1.11.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3Version")
    implementation("androidx.media3:media3-exoplayer-dash:$media3Version")
    implementation("androidx.media3:media3-exoplayer-rtsp:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")
    // Streaming protocols and containers that the core artifacts leave out:
    // RTMP data source (rtmp:// live streams), SmoothStreaming manifests (.ism)
    // and standard MIDI files. All are published next to the artifacts above and
    // carry no extra native code, so they cannot affect decoding paths.
    implementation("androidx.media3:media3-datasource-rtmp:$media3Version")
    implementation("androidx.media3:media3-exoplayer-smoothstreaming:$media3Version")
    implementation("androidx.media3:media3-exoplayer-midi:$media3Version")
    // OkHttp-backed network stack for http(s) playback: HTTP/2, compressed
    // responses and redirect handling that plain HttpURLConnection struggles
    // with on many CDN/stream hosts. OkHttp itself is already in the APK (Coil).
    implementation("androidx.media3:media3-datasource-okhttp:$media3Version")
    implementation("androidx.media:media:1.7.0")

    // Software decoders (FFmpeg: H.264/HEVC/VP8/VP9/AV1 + every common audio codec)
    // and native subtitle rendering (libass for ASS/SSA styling, FFmpeg for PGS,
    // VobSub and DVB bitmap subtitles). Media3 alone can only decode what the
    // device's MediaCodec understands, which is why this replaces the audio-only
    // Jellyfin FFmpeg decoder the app shipped before.
    implementation("io.github.anilbeesetti:nextlib-media3ext:1.11.1-0.18.0")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
