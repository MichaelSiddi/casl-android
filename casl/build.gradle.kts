plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.michaelsiddi.casl"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        targetSdk = 34

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    // Apply explicit API mode only to main source sets, not test code
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        if (!name.contains("Test")) {
            kotlinOptions {
                freeCompilerArgs = listOf("-Xexplicit-api=strict")
            }
        }
    }
}

dependencies {
    // Kotlin standard library and reflection (required for type detection)
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.20")

    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.20")
    testImplementation("org.robolectric:robolectric:4.11.1")
}

// Maven Publishing Configuration for JitPack
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = findProperty("GROUP") as String? ?: "com.casl"
                artifactId = "casl-android"
                version = findProperty("VERSION_NAME") as String? ?: "1.0.0"

                pom {
                    name.set("CASL Android")
                    description.set(findProperty("POM_DESCRIPTION") as String? ?: "CASL Android Authorization Library")
                    url.set(findProperty("POM_URL") as String? ?: "https://github.com/yourorg/casl-android")

                    licenses {
                        license {
                            name.set(findProperty("POM_LICENCE_NAME") as String? ?: "MIT License")
                            url.set(findProperty("POM_LICENCE_URL") as String? ?: "https://opensource.org/licenses/MIT")
                        }
                    }

                    developers {
                        developer {
                            id.set(findProperty("POM_DEVELOPER_ID") as String? ?: "casl")
                            name.set(findProperty("POM_DEVELOPER_NAME") as String? ?: "CASL Team")
                        }
                    }

                    scm {
                        url.set(findProperty("POM_SCM_URL") as String? ?: "https://github.com/yourorg/casl-android")
                        connection.set(findProperty("POM_SCM_CONNECTION") as String? ?: "scm:git:git://github.com/yourorg/casl-android.git")
                        developerConnection.set(findProperty("POM_SCM_DEV_CONNECTION") as String? ?: "scm:git:ssh://git@github.com/yourorg/casl-android.git")
                    }
                }
            }
        }
    }
}
