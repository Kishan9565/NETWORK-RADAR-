plugins {
    id("networkradar.android.library")
}

android {
    namespace = "com.networkradar.feature.history.data"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":feature:history:domain"))
}
