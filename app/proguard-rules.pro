# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Compose-specific classes
-keep class androidx.compose.** { *; }
-keep class kotlin.** { *; }

# Keep data classes
-keepclassmembers class * {
    public <init>(...);
}

# If your project uses Coil
-dontwarn coil.**
-keep class coil.** { *; }
