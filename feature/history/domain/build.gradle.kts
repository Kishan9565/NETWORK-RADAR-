plugins {
    id("networkradar.jvm.library")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertk)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
