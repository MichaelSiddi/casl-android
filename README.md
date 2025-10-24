# CASL Android Authorization Library

[![](https://jitpack.io/v/michaelsiddi/casl-android.svg)](https://jitpack.io/#michaelsiddi/casl-android)

Attribute-based access control (ABAC) authorization library for Android with full Java and Kotlin compatibility.

## Features

- 🔐 **Attribute-Based Access Control (ABAC)** - Define permissions based on resource attributes
- 🎯 **Field-Level Permissions** - Control access to specific fields
- 🚀 **High Performance** - Sub-millisecond permission checks
- 🔄 **Isomorphic** - JSON-serializable rules work across client and server
- 🧵 **Thread-Safe** - Concurrent permission checks supported
- 📦 **Zero Dependencies** - No external dependencies beyond Android SDK
- 💎 **Kotlin & Java** - Idiomatic APIs for both languages

## Installation

### Via JitPack (Recommended)

Add JitPack repository to your root `build.gradle.kts` or `settings.gradle.kts`:

```kotlin
repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}
```

Then add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.michaelsiddi:casl-android:v1.0.0")
}
```

### Via Local Maven (For Testing)

```kotlin
dependencies {
    implementation("com.casl:casl-android:1.0.0")
}
```

See [JITPACK.md](JITPACK.md) for detailed integration instructions.

**Requirements:**
- Android API 21+ (Android 5.0 Lollipop)
- Kotlin 1.9+ or Java 11+

## Quick Start

### Kotlin

```kotlin
import com.casl.Ability

// Define rules
val ability = Ability.builder()
    .can("read", "BlogPost")
    .can("update", "BlogPost", conditions = mapOf("authorId" to currentUserId))
    .cannot("delete", "BlogPost", conditions = mapOf("published" to true))
    .build()

// Check permissions
if (ability.can("read", blogPost)) {
    // Allow access
}
```

### Java

```java
import com.casl.Ability;
import java.util.Map;

// Define rules
Ability ability = Ability.builder()
    .can("read", "BlogPost")
    .can("update", "BlogPost", Map.of("authorId", currentUserId), null)
    .cannot("delete", "BlogPost", Map.of("published", true), null)
    .build();

// Check permissions
if (ability.can("read", blogPost)) {
    // Allow access
}
```

## Core Concepts

### Rules

Rules define what actions are permitted on which resources:

```kotlin
Ability.builder()
    .can("read", "BlogPost")           // Anyone can read blog posts
    .can("update", "BlogPost",         // Only author can update
        conditions = mapOf("authorId" to userId))
    .cannot("delete", "BlogPost",      // Can't delete published posts
        conditions = mapOf("published" to true))
    .build()
```

### Field-Level Permissions

Control access to specific fields:

```kotlin
Ability.builder()
    .can("read", "User",
        fields = listOf("name", "email"))  // Everyone can read basic fields
    .can("read", "User",
        conditions = mapOf("id" to currentUserId),
        fields = listOf("phoneNumber"))     // Only owner can read phone
    .build()

// Check field access
if (ability.can("read", user, field = "phoneNumber")) {
    // Show phone number
}
```

### Dynamic Rules

Load rules from your backend:

```kotlin
// Backend sends rules as JSON
val rulesJson = api.getUserPermissions(userId)
val rawRules = RawRule.listFromJson(rulesJson)
val ability = Ability.fromRules(rawRules)

// Or update existing ability
ability.update(rawRules)
```

## Advanced Usage

### Async Permission Checks

```kotlin
suspend fun checkAccess() {
    if (ability.canAsync("update", post)) {
        // Allowed
    }
}
```

### Export Rules

```kotlin
val rules = ability.exportRules()
val json = RawRule.listToJson(rules)
// Send to backend or store locally
```

## Performance

- Permission checks: <1ms for 100 rules
- Concurrent checks: 1000+ simultaneous operations
- Rule serialization: <10ms for 100 rules
- Package size: <100KB

## Thread Safety

All `Ability` methods are thread-safe and can be called from any thread. Rule updates are atomic - ongoing permission checks use the old rules while new checks use the updated rules immediately.

## Documentation

- [JitPack Integration Guide](./JITPACK.md)
- [Quick Start Guide](./specs/001-casl-android-port/quickstart.md)
- [Release Notes](./RELEASE_NOTES.md)
- [Changelog](./CHANGELOG.md)

## License

MIT License - see [LICENSE](LICENSE) file for details

## Support

- GitHub Issues: [Report bugs](https://github.com/michaelsiddi/casl-android/issues)
- Documentation: See [JITPACK.md](JITPACK.md) for integration guide
