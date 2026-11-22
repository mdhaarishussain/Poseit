import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Mixkit stills in release builds (docs/POSE_PHOTOS.md, "Mixkit in release builds"): on since 1.2.0, through
// poseit.releaseMixkitPhotos=true in gradle.properties. The owner asked for the release with all 34 photos after the
// two open questions recorded there (scripted downloads under the Mixkit User Terms, and the unread Envato policies)
// were explained to them; the questions are accepted, not settled. Set the property to false (or pass
// -Pposeit.releaseMixkitPhotos=false) and rebuild to leave the stills out of a release again. Debug builds and the
// screenshot tests always include them.
val releaseMixkitPhotos = providers.gradleProperty("poseit.releaseMixkitPhotos").orNull?.toBoolean() ?: false

android {
    namespace = "com.poseit.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.poseit.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "1.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        // Phones only (keeps the APK small); add "x86_64" to run on an emulator.
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }

    signingConfigs {
        // A PUBLIC, throwaway key committed to the repo so that every build (local, CI, GitHub releases) has the same
        // signature and installs over the previous one. It is not a secret: anyone can sign with it. For builds you
        // distribute to other people, provide your own key through the POSEIT_KEYSTORE* environment variables below
        // (the release workflow reads them from repository secrets).
        create("sideload") {
            storeFile = file("signing/poseit-sideload.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        val ownKeystore = System.getenv("POSEIT_KEYSTORE")
        if (!ownKeystore.isNullOrBlank() && file(ownKeystore).exists()) {
            create("own") {
                storeFile = file(ownKeystore)
                storePassword = System.getenv("POSEIT_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("POSEIT_KEY_ALIAS")
                keyPassword = System.getenv("POSEIT_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("sideload")
            buildConfigField("boolean", "MIXKIT_PHOTOS", "true")
        }
        release {
            buildConfigField("boolean", "MIXKIT_PHOTOS", releaseMixkitPhotos.toString())
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("own") ?: signingConfigs.getByName("sideload")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // MediaPipe Tasks calls java.time (API 26+) whenever a task is created; without desugaring every
        // PoseLandmarker / ImageSegmenter creation fails on Android 7.x (minSdk 24).
        isCoreLibraryDesugaringEnabled = true
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources {
        // MediaPipe memory-maps the model files; they must stay uncompressed in the APK.
        noCompress += listOf("task", "tflite")
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        jniLibs.useLegacyPackaging = false
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
        // Robolectric + Roborazzi screenshot tests render Compose screens on the JVM.
        unitTests.isIncludeAndroidResources = true
        unitTests.all { test ->
            test.systemProperty("roborazzi.test.record", "true")
            // Screenshot tests keep large native-graphics bitmaps alive (the component gallery is a 1300x3200dp
            // canvas); Gradle's default 512 MB test heap ran out on GitHub's runners.
            test.maxHeapSize = "2g"
            // Optional: -ProbolectricRepoUrl=<maven mirror> if Maven Central rate-limits Robolectric's SDK download.
            (project.findProperty("robolectricRepoUrl") as String?)?.let {
                test.systemProperty("robolectric.dependency.repo.url", it)
                test.systemProperty("robolectric.dependency.repo.id", "mirror")
            }
        }
    }
}

// ---------------------------------------------------------------------------
// With poseit.releaseMixkitPhotos unset or false, the release APK leaves the Mixkit stills out entirely (not just
// unshown): their files are removed from the merged assets. Which files they are comes from pose_photos/credits.json.
// ---------------------------------------------------------------------------
if (!releaseMixkitPhotos) {
    val creditsFile = layout.projectDirectory.file("src/main/assets/pose_photos/credits.json").asFile
    tasks.matching { it.name == "mergeReleaseAssets" }.configureEach {
        doLast {
            @Suppress("UNCHECKED_CAST")
            val credits = groovy.json.JsonSlurper().parse(creditsFile) as Map<String, Map<String, Any?>>
            val mixkit = credits.values.filter { it["source"] == "Mixkit" }.map { "pose_photos/${it["photo"]}" }
            outputs.files.files.forEach { dir -> mixkit.forEach { File(dir, it).delete() } }
            logger.lifecycle("Release assets: ${mixkit.size} Mixkit stills left out (poseit.releaseMixkitPhotos is not true)")
        }
    }
}

// ---------------------------------------------------------------------------
// MediaPipe models (Apache-2.0: pose landmarker + DeepLab v3 segmenter) are downloaded at build time
// instead of being committed to git.
// ---------------------------------------------------------------------------
val poseModels = mapOf(
    "pose_landmarker_lite.task" to
        "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/latest/pose_landmarker_lite.task",
    "pose_landmarker_full.task" to
        "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_full/float16/latest/pose_landmarker_full.task",
    // Person segmentation for turning reference photos into outline / silhouette guides.
    "deeplab_v3.tflite" to
        "https://storage.googleapis.com/mediapipe-models/image_segmenter/deeplab_v3/float32/latest/deeplab_v3.tflite",
)
val modelDir = layout.projectDirectory.dir("src/main/assets/models")

val downloadPoseModels by tasks.registering {
    description = "Downloads MediaPipe models into assets/models"
    outputs.dir(modelDir)
    doLast {
        val dir = modelDir.asFile.apply { mkdirs() }
        poseModels.forEach { (name, url) ->
            val target = File(dir, name)
            if (target.exists() && target.length() > 1_000_000) return@forEach
            logger.lifecycle("Downloading $name ...")
            val tmp = File(dir, "$name.part")
            URI(url).toURL().openStream().use { input -> tmp.outputStream().use { input.copyTo(it) } }
            check(tmp.length() > 1_000_000) { "Download of $name looks truncated (${tmp.length()} bytes)" }
            tmp.renameTo(target)
        }
    }
}
tasks.named("preBuild") { dependsOn(downloadPoseModels) }

dependencies {
    implementation(project(":pose-core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.exifinterface)
    implementation(libs.mediapipe.tasks.vision)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi.core)
    testImplementation(libs.roborazzi.compose)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
}
