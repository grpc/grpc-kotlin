plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.protobuf)
}

dependencies {
    protobuf(project(":protos"))

    api(libs.kotlinx.coroutines.core)

    api(libs.grpc.stub)
    api(libs.grpc.protobuf.lite)
    api(libs.grpc.kotlin.stub)
    api(libs.protobuf.kotlin.lite)
}

kotlin {
    jvmToolchain(8)
}

android {
    compileSdk = 34
    buildToolsVersion = "34.0.0"
    namespace = "io.grpc.examples.stublite"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    kotlinOptions {
        freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
    }
}

protobuf {
    protoc {
        artifact = libs.protoc.asProvider().get().toString()
    }
    plugins {
        create("java") {
            artifact = libs.protoc.gen.grpc.java.get().toString()
        }
        create("grpc") {
            artifact = libs.protoc.gen.grpc.java.get().toString()
        }
        create("grpckt") {
            artifact = libs.protoc.gen.grpc.kotlin.get().toString() + ":jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("java") {
                    option("lite")
                }
                create("grpc") {
                    option("lite")
                }
                create("grpckt") {
                    option("lite")
                }
            }
            it.builtins {
                create("kotlin") {
                    option("lite")
                }
            }
        }
    }
}
