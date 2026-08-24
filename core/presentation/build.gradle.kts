plugins {
    id("networkradar.android.library")
    id("networkradar.android.compose")
}

android {
    namespace = "com.networkradar.core.presentation"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:design-system"))
    
    implementation(libs.bundles.koin)
}
