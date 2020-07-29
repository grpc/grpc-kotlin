plugins {
    id("com.android.application")
    kotlin("android")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("androidx.appcompat:appcompat:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.7")
    implementation(project(":stub-lite"))
    runtimeOnly("io.grpc:grpc-okhttp:${rootProject.ext["grpcVersion"]}")
}

android {
    compileSdkVersion(29)
    buildToolsVersion = "29.0.3"

    defaultConfig {
        applicationId = "io.grpc.examples.hello"
        minSdkVersion(23)
        targetSdkVersion(29)
        versionCode = 1
        versionName = "1.0"

        val serverUrl: String? by project
        if (serverUrl != null) {
            resValue("string", "server_url", serverUrl!!)
        } else {
            resValue("string", "server_url", "http://10.0.2.2:50051/")
        }
    }

    sourceSets["main"].java.srcDir("src/main/kotlin")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_7
        targetCompatibility = JavaVersion.VERSION_1_7
    }
}
