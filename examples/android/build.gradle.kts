plugins {
    id("com.android.application")
    kotlin("android")
}

val composeVersion = "1.4.3"
val composeCompilerVersion = "1.4.7"

dependencies {
    implementation(project(":stub-android"))
    implementation("androidx.activity:activity-compose:1.7.1")
    implementation("androidx.compose.foundation:foundation-layout:$composeVersion")
    implementation("androidx.compose.material:material:$composeVersion")
    implementation("androidx.compose.runtime:runtime:$composeVersion")
    implementation("androidx.compose.ui:ui:$composeVersion")

    runtimeOnly("io.grpc:grpc-okhttp:${rootProject.ext["grpcVersion"]}")
}

kotlin {
    jvmToolchain(8)
}

android {
    compileSdk = 33
    buildToolsVersion = "33.0.0"
    namespace = "io.grpc.examples.helloworld"

    defaultConfig {
        applicationId = "io.grpc.examples.hello"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        val serverUrl: String? by project
        if (serverUrl != null) {
            resValue("string", "server_url", serverUrl!!)
        } else {
            resValue("string", "server_url", "http://10.0.2.2:50051/")
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = composeCompilerVersion
    }
}
