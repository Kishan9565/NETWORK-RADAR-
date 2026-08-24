plugins {
    id("networkradar.android.library")
}

android {
    namespace = "com.networkradar.feature.scan.data"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":feature:scan:domain"))
}
