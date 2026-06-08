# ProGuard rules for GHOST PLAY

# Keep Kotlin metadata
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# Keep Jetpack Compose
-keep class androidx.compose.** { *; }

# Keep AndroidX
-keep class androidx.** { *; }

# Keep Media3/ExoPlayer
-keep class androidx.media3.** { *; }

# Keep Coil
-keep class coil.** { *; }

# Keep app classes
-keep class com.fireplay.com.** { *; }

# Keep enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Remove logging
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
