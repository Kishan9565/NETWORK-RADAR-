plugins {
    id("networkradar.android.library")
}

android {
    namespace = "com.networkradar.feature.comparison.data"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":feature:comparison:domain"))
}
