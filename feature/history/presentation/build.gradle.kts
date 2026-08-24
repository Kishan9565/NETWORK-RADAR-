plugins {
    id("networkradar.android.library")
    id("networkradar.android.compose")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.networkradar.feature.history.presentation"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:presentation"))
    implementation(project(":feature:history:domain"))
    
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.bundles.koin)
}
