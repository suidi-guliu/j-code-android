plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}
android {
    namespace = "dev.suicode.packages"
    compileSdk = 36
    defaultConfig { minSdk = 33 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions { jvmTarget = "21" }
}
dependencies {
    api(libs.kotlinx.serialization.json)
    testImplementation(kotlin("test"))
    testImplementation(libs.coroutines.core)
}

// Reuse the exact production verifier on the host rather than maintaining a second parser.
val validatorRuntime by configurations.creating
dependencies {
    add("validatorRuntime", kotlin("stdlib"))
    add("validatorRuntime", libs.kotlinx.serialization.json)
}
tasks.register<JavaExec>("validatePackage") {
    dependsOn("compileDebugKotlin")
    classpath = files(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) + validatorRuntime
    mainClass.set("dev.suicode.packages.PackageTool")
    doFirst {
        args = listOf(providers.gradleProperty("suiPackage").get()) + providers.gradleProperty("suiInstalled").orNull.orEmpty().split(',').filter(String::isNotEmpty)
    }
}

// Fixture changes must invalidate tests even when Kotlin classes did not change.
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    inputs.dir(rootProject.file("native-library"))
    inputs.dir(rootProject.file("docs/specifications/examples"))
    inputs.property("suiTestJdk",System.getenv("SUI_QUICKRUN_TEST_JAVA_HOME") ?: "unset")
}
