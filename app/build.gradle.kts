import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.poseit.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.poseit.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        // Phones only (keeps the APK small); add "x86_64" to run on an emulator.
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Signed with the debug key so the release APK is installable without extra setup.
            // Replace with your own signingConfig before publishing anywhere.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
