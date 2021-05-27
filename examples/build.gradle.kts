plugins {
    id("com.android.application") version "4.1.1" apply false
    id("com.google.protobuf") version "0.8.16" apply false
    kotlin("jvm") version "1.4.32" apply false
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
}

// todo: move to subprojects, but how?
ext["grpcVersion"] = "1.38.0"
ext["grpcKotlinVersion"] = "1.1.0" // CURRENT_GRPC_KOTLIN_VERSION
ext["protobufVersion"] = "3.17.1"

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }

    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}

tasks.create("assemble").dependsOn(":server:installDist")
