plugins {
    id("com.android.application") version "7.4.2" apply false // Older for IntelliJ Support
    id("com.google.protobuf") version "0.9.3" apply false
    kotlin("jvm") version "1.8.21" apply false
    id("org.jlleitschuh.gradle.ktlint") version "11.3.2"
}

// todo: move to subprojects, but how?
ext["grpcVersion"] = "1.54.1"
ext["grpcKotlinVersion"] = "1.3.0" // CURRENT_GRPC_KOTLIN_VERSION
ext["protobufVersion"] = "3.22.3"
ext["coroutinesVersion"] = "1.6.4"

allprojects {
    repositories {
        mavenCentral()
        google()
        mavenLocal() // For testing new releases of gRPC Kotlin
    }

    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    ktlint {
        filter {
            exclude {
                it.file.path.contains("$buildDir/generated/")
            }
        }
    }
}

tasks.create("assemble").dependsOn(":server:installDist")
