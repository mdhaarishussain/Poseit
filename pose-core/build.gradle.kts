plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

// Pure-Kotlin pose engine: model, authoring DSL, built-in pose library, matching & coaching logic.
// No Android dependencies, so everything here is unit-testable on the JVM (and renderable with Java2D).
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}

tasks.test {
    // Preview PNGs are written relative to the module dir: pose-core/build/pose-previews/
    workingDir = projectDir
    systemProperty("java.awt.headless", "true")
}
