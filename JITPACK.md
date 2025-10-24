# JitPack Integration Guide

## Overview

CASL Android is configured for easy distribution via [JitPack.io](https://jitpack.io), which builds Android libraries directly from GitHub releases and makes them available as Maven dependencies.

## Prerequisites

To publish your library on JitPack, ensure:

1. ✅ **Gradle wrapper included** (`gradlew` and `gradlew.bat`)
2. ✅ **Maven publishing plugin configured** in `casl/build.gradle.kts`
3. ✅ **Library metadata defined** in `gradle.properties`
4. ✅ **JitPack configuration** in `jitpack.yml`
5. ✅ **Git repository with releases/tags**

All prerequisites are already configured in this project!

## How JitPack Works

1. You create a Git tag/release (e.g., `v1.0.0`)
2. Users request the library via JitPack URL
3. JitPack clones your repo, runs `./gradlew publishToMavenLocal`, and caches the artifacts
4. Users download the built library from JitPack's Maven repository

## Publishing to JitPack

### Step 1: Create a GitHub Release

```bash
# Tag your release
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0

# Or create a release via GitHub UI
```

### Step 2: Verify on JitPack

1. Visit: `https://jitpack.io/#michaelsiddi/casl-android`
2. Click "Look up" next to your tag (e.g., `v1.0.0`)
3. JitPack will build your library (first build takes ~2-5 minutes)
4. Build log shows: `BUILD SUCCESSFUL`

### Step 3: Get the Dependency

After successful build, JitPack provides:

**Gradle (Kotlin DSL):**
```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.michaelsiddi:casl-android:v1.0.0")
}
```

**Gradle (Groovy DSL):**
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.michaelsiddi:casl-android:v1.0.0'
}
```

## Using Specific Versions

JitPack supports multiple version formats:

| Format | Example | Use Case |
|--------|---------|----------|
| Release tag | `v1.0.0` | Stable releases |
| Commit hash | `abc1234` | Specific commit |
| Branch | `main-SNAPSHOT` | Latest from branch |
| Short commit | `abc1234-SNAPSHOT` | Development builds |

## Configuration Files

### jitpack.yml

```yaml
jdk:
  - openjdk17

before_install:
  - sdk install java 17.0.1-open || true
  - sdk use java 17.0.1-open

install:
  - ./gradlew clean build publishToMavenLocal -x test
```

This ensures JitPack uses Java 17 (required for Android Gradle Plugin 8.x).

### gradle.properties

```properties
GROUP=com.casl
VERSION_NAME=1.0.0
POM_DESCRIPTION=CASL Android Authorization Library
POM_URL=https://github.com/michaelsiddi/casl-android
POM_LICENCE_NAME=MIT License
```

These properties populate the Maven POM metadata.

### casl/build.gradle.kts

```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.casl"
                artifactId = "casl-android"
                version = "1.0.0"
            }
        }
    }
}
```

## Testing Locally

Before publishing to JitPack, test locally:

```bash
# Build and publish to local Maven repository
./gradlew :casl:publishToMavenLocal

# Check artifacts
ls ~/.m2/repository/com/casl/casl-android/1.0.0/
```

You should see:
- `casl-android-1.0.0.aar` (library)
- `casl-android-1.0.0.pom` (Maven metadata)
- `casl-android-1.0.0-sources.jar` (source code)
- `casl-android-1.0.0.module` (Gradle metadata)

## Using from Local Maven

To test integration before JitPack:

```kotlin
repositories {
    mavenLocal()
    google()
    mavenCentral()
}

dependencies {
    implementation("com.casl:casl-android:1.0.0")
}
```

## Troubleshooting

### Build Fails on JitPack

**Check the build log** at `https://jitpack.io/com/github/michaelsiddi/casl-android/v1.0.0/build.log`

Common issues:

1. **Missing Gradle wrapper**
   - Ensure `gradlew` and `gradle/wrapper/` are committed

2. **Wrong JDK version**
   - Verify `jitpack.yml` specifies Java 17

3. **Build errors**
   - Test locally first: `./gradlew :casl:publishToMavenLocal`

4. **Missing Android SDK**
   - JitPack provides Android SDK automatically for Android projects

### Version Not Found

If users get "Could not find com.github.michaelsiddi:casl-android:v1.0.0":

1. Verify release/tag exists: `git tag -l`
2. Check JitPack built successfully
3. Clear Gradle cache: `./gradlew clean --refresh-dependencies`

### Slow First Build

JitPack builds on-demand. First request takes 2-5 minutes. Subsequent requests use cached artifacts.

## Alternative: GitHub Packages

If you prefer GitHub's native package registry:

1. Add GitHub Packages repository to publishing configuration
2. Create a Personal Access Token with `write:packages` scope
3. Configure authentication in `~/.gradle/gradle.properties`

See [GitHub Packages docs](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry) for details.

## Best Practices

1. **Use semantic versioning**: `v1.0.0`, `v1.1.0`, `v2.0.0`
2. **Tag stable releases only**: Don't tag every commit
3. **Test locally first**: `./gradlew publishToMavenLocal`
4. **Document breaking changes**: Update CHANGELOG.md
5. **Keep dependencies minimal**: Currently zero external deps (optimal!)

## Resources

- [JitPack Documentation](https://jitpack.io/docs/)
- [JitPack Building Guide](https://jitpack.io/docs/BUILDING/)
- [Gradle Publishing Plugin](https://docs.gradle.org/current/userguide/publishing_maven.html)

## Status Badge

Add to your README.md:

```markdown
[![](https://jitpack.io/v/michaelsiddi/casl-android.svg)](https://jitpack.io/#michaelsiddi/casl-android)
```

---

**Ready to publish?** Just create a Git tag and push it!

```bash
git tag -a v1.0.0 -m "First stable release"
git push origin v1.0.0
```

Then visit https://jitpack.io/#michaelsiddi/casl-android to trigger the build.
