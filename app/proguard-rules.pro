-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
