# Consumer ProGuard rules for CASL Android Library
# These rules are automatically applied to apps that depend on this library

# Keep all public API classes and methods
-keep public class com.casl.Ability {
    public *;
}

-keep public class com.casl.AbilityBuilder {
    public *;
}

-keep public class com.casl.RawRule {
    public *;
}

# Keep extension functions (optional for consumers)
-keep class com.casl.extensions.** {
    public *;
}

# Preserve Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }
-keepclassmembers class com.casl.** {
    <init>(...);
}
