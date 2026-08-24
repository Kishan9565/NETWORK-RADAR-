plugins {
    id("networkradar.android.library")
}

android {
    namespace = "com.networkradar.feature.heatmap.data"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":feature:heatmap:domain"))
}
