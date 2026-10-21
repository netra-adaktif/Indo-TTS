plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    `maven-publish`
}

android {
    namespace = "org.evvdroid"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.evvdroid"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    // Pustaka Material Design untuk menyediakan Theme.Material3
    implementation("com.google.android.material:material:1.11.0")
    
    // Pustaka dasar AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.netra-adaktif"
                artifactId = "indo-tts"
                version = "1.0.0"
            }
        }
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/netra-adaktif/Indo-TTS")
                credentials {
                    username = System.getenv("GITHUB_ACTOR") ?: "netra-adaktif"
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}
