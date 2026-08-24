plugins {
    id("networkradar.android.library")
}

android {
    namespace = "com.networkradar.feature.map.data"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":feature:map:domain"))
}
