import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    id("com.android.library")
    kotlin("android")
    id("com.google.protobuf")
}

dependencies {
    protobuf(project(":protos"))

    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.8")
    implementation("javax.annotation:javax.annotation-api:1.3.2")

    api("com.google.protobuf:protobuf-javalite:${rootProject.ext["protobufVersion"]}")
    api("io.grpc:grpc-protobuf-lite:${rootProject.ext["grpcVersion"]}")

    // todo: I think we should be able to just include this:
    // api("io.grpc:grpc-kotlin-stub-lite:${rootProject.ext["grpcKotlinVersion"]}")
    // but it doesn't pull transitives correctly

    api("io.grpc:grpc-stub:${rootProject.ext["grpcVersion"]}")
    api("io.grpc:grpc-kotlin-stub:${rootProject.ext["grpcKotlinVersion"]}") {
        exclude("io.grpc", "grpc-protobuf")
    }
}

android {
    compileSdkVersion(30)
    buildToolsVersion = "30.0.2"
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${rootProject.ext["protobufVersion"]}"
    }
    plugins {
        id("java") {
            artifact = "io.grpc:protoc-gen-grpc-java:${rootProject.ext["grpcVersion"]}"
        }
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${rootProject.ext["grpcVersion"]}"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${rootProject.ext["grpcKotlinVersion"]}"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("java") {
                    option("lite")
                }
                id("grpc") {
                    option("lite")
                }
                id("grpckt") {
                    option("lite")
                }
            }
        }
    }
}
