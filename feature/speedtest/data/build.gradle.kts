plugins {
    id("networkradar.android.library")
}

android {
    namespace = "com.networkradar.feature.speedtest.data"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":feature:speedtest:domain"))
}
