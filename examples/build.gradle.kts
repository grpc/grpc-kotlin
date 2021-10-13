plugins {
    id("com.android.application") version "4.1.1" apply false
    id("com.google.protobuf") version "0.8.17" apply false
    kotlin("jvm") version "1.5.31" apply false
    id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
}

// todo: move to subprojects, but how?
ext["grpcVersion"] = "1.39.0" // need to wait for grpc kotlin to move past this
ext["grpcKotlinVersion"] = "1.2.0" // CURRENT_GRPC_KOTLIN_VERSION
ext["protobufVersion"] = "3.18.1"
ext["coroutinesVersion"] = "1.5.2"

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }

    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}

tasks.create("assemble").dependsOn(":server:installDist")
