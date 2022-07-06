plugins {
    id("com.android.application") version "7.0.4" apply false // Older for IntelliJ Support
    id("com.google.protobuf") version "0.8.18" apply false
    kotlin("jvm") version "1.7.0" apply false
    id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
}

// todo: move to subprojects, but how?
ext["grpcVersion"] = "1.47.0"
ext["grpcKotlinVersion"] = "1.3.0" // CURRENT_GRPC_KOTLIN_VERSION
ext["protobufVersion"] = "3.21.2"
ext["coroutinesVersion"] = "1.6.2"

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }

    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}

tasks.create("assemble").dependsOn(":server:installDist")
