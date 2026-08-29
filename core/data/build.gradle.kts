plugins {
    id("networkradar.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.networkradar.core.data"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:domain"))
    
    implementation(libs.bundles.ktor)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.bundles.koin)
    implementation(libs.play.services.location)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.assertk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.mockk)
}
