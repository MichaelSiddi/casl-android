# CASL Android Library ProGuard Rules

# Keep public API
-keep public class com.casl.** {
    public *;
}

# Keep extension functions
-keep class com.casl.extensions.** {
    public *;
}

# Preserve annotations
-keepattributes *Annotation*

# Keep Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }

# Keep companion objects
-keepclassmembers class * {
    public static ** Companion;
}

# Preserve generic signatures
-keepattributes Signature

# Keep source file names and line numbers for better stack traces
-keepattributes SourceFile,LineNumberTable

# Optimization settings
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
