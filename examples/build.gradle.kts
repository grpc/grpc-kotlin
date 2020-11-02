plugins {
    id("com.android.application") version "4.0.0" apply false
    id("com.google.protobuf") version "0.8.13" apply false
    kotlin("jvm") version "1.3.72" apply false
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
}

// todo: move to subprojects, but how?
ext["grpcVersion"] = "1.32.1"
ext["grpcKotlinVersion"] = "0.2.1" // CURRENT_GRPC_KOTLIN_VERSION
ext["protobufVersion"] = "3.13.0"

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        google()
    }

    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}

tasks.create("assemble").dependsOn(":server:installDist")
