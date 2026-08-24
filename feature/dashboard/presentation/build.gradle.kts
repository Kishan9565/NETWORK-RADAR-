plugins {
    id("networkradar.android.library")
    id("networkradar.android.compose")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.networkradar.feature.dashboard.presentation"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:presentation"))
    implementation(project(":core:design-system"))
    implementation(project(":feature:dashboard:domain"))
    
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.bundles.koin)
    
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.assertk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
