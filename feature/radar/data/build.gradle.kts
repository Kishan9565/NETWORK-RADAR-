plugins {
    id("networkradar.android.library")
}

android {
    namespace = "com.networkradar.feature.radar.data"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":feature:radar:domain"))
}
