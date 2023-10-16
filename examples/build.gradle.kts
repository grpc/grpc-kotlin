plugins {
    id("com.android.application") version "8.1.1" apply false
    id("com.google.protobuf") version "0.9.4" apply false
    kotlin("jvm") version "1.9.10" apply false
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1" apply false
}

// todo: move to subprojects, but how?
ext["grpcVersion"] = "1.57.2"
ext["grpcKotlinVersion"] = "1.4.0" // CURRENT_GRPC_KOTLIN_VERSION
ext["protobufVersion"] = "3.24.1"
ext["coroutinesVersion"] = "1.7.3"

subprojects {
    repositories {
        mavenLocal() // For testing new releases of gRPC Kotlin
        mavenCentral()
        google()
    }

    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        filter {
            exclude {
                it.file.path.startsWith(project.layout.buildDirectory.get().dir("generated").toString())
            }
        }
    }
}

tasks.create("assemble").dependsOn(":server:installDist")
