plugins {
    id("networkradar.android.library")
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "com.networkradar.core.database"
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.bundles.koin)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.assertk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
