plugins {
    id("networkradar.android.application")
    id("networkradar.android.compose")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.networkradar"

    defaultConfig {
        applicationId = "com.networkradar"
        versionCode = 1
        versionName = "1.0"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:presentation"))
    implementation(project(":core:design-system"))
    implementation(project(":core:database"))
    
    implementation(project(":feature:dashboard:presentation"))
    implementation(project(":feature:radar:presentation"))
    implementation(project(":feature:speedtest:presentation"))
    implementation(project(":feature:heatmap:presentation"))
    implementation(project(":feature:history:presentation"))
    implementation(project(":feature:comparison:presentation"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.bundles.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.bundles.koin)
    
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertk)
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit5)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
