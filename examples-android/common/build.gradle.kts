import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.ofSourceSet
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    kotlin("jvm")
    id("com.google.protobuf") version "0.8.12"
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    api("io.grpc:grpc-protobuf-lite:${rootProject.ext["grpcVersion"]}")
    api("io.grpc:grpc-stub:${rootProject.ext["grpcVersion"]}")
    api("io.grpc:grpc-kotlin-stub:${rootProject.ext["grpcKotlinVersion"]}") {
        exclude("io.grpc", "grpc-protobuf")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_7
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${rootProject.ext["protobufVersion"]}"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${rootProject.ext["grpcVersion"]}"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${rootProject.ext["grpcKotlinVersion"]}"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.builtins {
                named("java") {
                    option("lite")
                }
            }
            it.plugins {
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
