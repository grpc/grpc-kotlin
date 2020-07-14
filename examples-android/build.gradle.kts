buildscript {
    repositories {
        gradlePluginPortal()
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.6.3")
    }
}

plugins {
    kotlin("jvm") version "1.3.72"
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
}

// todo: move to subprojects, but how?
ext["grpcVersion"] = "1.30.0"
ext["grpcKotlinVersion"] = "0.1.3"
ext["protobufVersion"] = "3.12.2"

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        google()
    }

    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}

tasks.replace("assemble").dependsOn(":server:installDist")
