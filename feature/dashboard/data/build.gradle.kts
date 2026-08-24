plugins {
    id("networkradar.android.library")
}

android {
    namespace = "com.networkradar.feature.dashboard.data"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":feature:dashboard:domain"))
}
