plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.blamspot.jcode.core.distro"
    compileSdk = 36

    defaultConfig {
        minSdk = 33
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }
}

dependencies {
    implementation(libs.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.snakeyaml.engine)
    implementation(libs.xz)
    implementation(libs.commons.compress)
    implementation(project(":core:config"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    testImplementation(kotlin("test"))
}

// Fixture changes must invalidate tests even when Kotlin classes did not change.
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    inputs.dir(rootProject.file("native-library/src"))
    inputs.property("suiTestJdk",System.getenv("SUI_QUICKRUN_TEST_JAVA_HOME") ?: "unset")
}
