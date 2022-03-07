plugins {
    id("com.android.application") version "7.0.4" apply false
    id("com.google.protobuf") version "0.8.18" apply false
    kotlin("jvm") version "1.6.10" apply false // Compose Compiler required version
    id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
}

// todo: move to subprojects, but how?
ext["grpcVersion"] = "1.45.0"
ext["grpcKotlinVersion"] = "1.2.1" // CURRENT_GRPC_KOTLIN_VERSION
ext["protobufVersion"] = "3.19.4"
ext["coroutinesVersion"] = "1.6.0"

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }

    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}

tasks.create("assemble").dependsOn(":server:installDist")
