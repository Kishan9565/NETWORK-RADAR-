plugins {
    id("networkradar.jvm.library")
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.kotlinx.coroutines.core)
}
