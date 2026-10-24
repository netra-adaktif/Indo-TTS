import org.gradle.internal.os.OperatingSystem
import java.util.Properties

// Only US English is included in this build. Other language modules are deliberately excluded.
val ALL_LANGUAGES = listOf("enus")

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.kotlin.compose)
}

val abis = (findProperty("evvdroid.abis") as String? ?: "arm64-v8a")
	.split(",").map { it.trim() }.filter { it.isNotEmpty() }
val rulesForm = findProperty("evvdroid.rules") as String? ?: "c"
val languages = findProperty("evvdroid.langs") as String?
	?: "lang/enus"
val nativeOut = layout.buildDirectory.dir("native/jniLibs")

// Release signing, from a keystore.properties the repo does not carry. Without
// it a clone still builds and the release APK comes out unsigned.
val keystore = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { where ->
	Properties().apply { where.inputStream().use { stream -> load(stream) } }
}

// The engine is built by its own Makefile rather than by CMake, because that
// Makefile writes the language rules with Python before it compiles them and
// nothing here has a reason to reimplement that.
val buildNative by tasks.registering(Exec::class) {
	group = "build"
	description = "Cross-compiles the openevv engine and the JNI bridge for each ABI."
	val engine = rootProject.layout.projectDirectory.dir("native/openevv")
	val script = rootProject.layout.projectDirectory.file("native/build-native.sh")
	inputs.files(rootProject.fileTree("native/patches"))
	inputs.file(rootProject.layout.projectDirectory.file("native/android.mk"))
	inputs.file(rootProject.layout.projectDirectory.file(".gitmodules"))
	inputs.file(script)
	inputs.file(layout.projectDirectory.file("src/main/cpp/evv_jni.c"))
	inputs.property("abis", abis)
	inputs.property("rules", rulesForm)
	inputs.property("langs", languages)
	outputs.dir(nativeOut)
	val shell = if (OperatingSystem.current().isWindows) {
		val git = System.getenv("PROGRAMFILES")?.let { file("$it/Git/bin/bash.exe") }
		if (git != null && git.exists()) git.absolutePath else "bash"
	} else {
		"bash"
	}
	commandLine(shell, script.asFile.absolutePath)
	environment("ABIS", abis.joinToString(" "))
	environment("RULES", rulesForm)
	environment("LANGS", languages)
	environment("OUT", nativeOut.get().asFile.absolutePath)
	val ndkEnv = System.getenv("ANDROID_NDK_HOME") ?: System.getenv("ANDROID_NDK_ROOT") ?: ""
	if (ndkEnv.isNotEmpty()) {
		environment("ANDROID_NDK_HOME", ndkEnv)
	}
	doFirst {
		if (!engine.file("Makefile").asFile.exists()) {
			throw GradleException("native/openevv is empty. Run: git submodule update --init --recursive")
		}
	}
}

android {
	namespace = "id.eloquenceindo"
	compileSdk = 36
	ndkVersion = "27.0.12077973"

	defaultConfig {
		applicationId = "id.eloquenceindo"
		minSdk = 23
		targetSdk = 36
		versionCode = 3
		versionName = "0.3.0"
		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		ndk {
			abiFilters += abis
		}
	}

	sourceSets.getByName("androidTest") {
		kotlin.srcDirs("src/androidTest/kotlin")
	}

	sourceSets.getByName("test") {
		kotlin.srcDirs("src/test/kotlin")
	}

	testOptions {
		unitTests.isReturnDefaultValues = true
	}

	sourceSets.getByName("main") {
		kotlin.srcDirs("src/main/kotlin")
		jniLibs.srcDirs(nativeOut)
	}

	signingConfigs {
		keystore?.let { props ->
			create("release") {
				storeFile = file(props.getProperty("storeFile"))
				storePassword = props.getProperty("storePassword")
				keyAlias = props.getProperty("keyAlias")
				keyPassword = props.getProperty("keyPassword")
				enableV3Signing = true
			}
		}
	}

	buildTypes {
		release {
			signingConfig = signingConfigs.findByName("release")
			isMinifyEnabled = true
			isShrinkResources = true
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
		}
		debug {
			isJniDebuggable = true
		}
	}

	packaging {
		jniLibs {
			useLegacyPackaging = false
		}
	}

	splits {
		abi {
			isEnable = (findProperty("evvdroid.abiSplits") as String?)?.toBoolean() ?: false
			reset()
			include(*abis.toTypedArray())
			isUniversalApk = false
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
		buildConfig = true
		compose = true
	}
}

tasks.named("preBuild") {
	dependsOn(buildNative)
}

dependencies {
	implementation(libs.androidx.core.ktx)
	implementation(libs.material)
	implementation(libs.androidx.activity.compose)
	implementation(platform(libs.compose.bom))
	implementation(libs.compose.ui)
	implementation(libs.compose.material3)

	testImplementation(libs.junit)
	androidTestImplementation(libs.junit)
	androidTestImplementation(libs.androidx.test.junit)
	androidTestImplementation(libs.androidx.test.runner)
	androidTestImplementation(libs.androidx.test.core)
}
