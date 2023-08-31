import com.google.protobuf.gradle.*

plugins {
    application
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${rootProject.ext["coroutinesVersion"]}")

    implementation(project(":stub"))

    implementation("io.grpc:grpc-protobuf:${rootProject.ext["grpcVersion"]}")
    implementation("io.grpc:grpc-auth:${rootProject.ext["grpcVersion"]}")
    implementation("io.grpc:grpc-alts:${rootProject.ext["grpcVersion"]}")
    implementation("io.grpc:grpc-netty:${rootProject.ext["grpcVersion"]}")
    implementation("io.grpc:grpc-okhttp:${rootProject.ext["grpcVersion"]}")
    implementation("io.grpc:grpc-testing:${rootProject.ext["grpcVersion"]}")

    implementation("com.google.protobuf:protobuf-java:${rootProject.ext["protobufVersion"]}")

    implementation("com.google.truth:truth:1.1.3")

    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("com.squareup.okhttp:okhttp:2.7.5") {
        because("transitive dep for grpc-okhttp")
    }
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
            path = project(":compiler").tasks.jar.get().archiveFile.get().asFile.absolutePath
        }
    }
    generateProtoTasks {
        all().forEach {
            if (it.name.startsWith("generateTestProto") || it.name.startsWith("generateProto")) {
                it.dependsOn(":compiler:jar")
            }

            it.plugins {
                id("grpc")
                id("grpckt")
            }
        }
    }
}

val testServiceClientStartScripts = tasks.register<CreateStartScripts>("testServiceClientStartScripts") {
    mainClass.set("io.grpc.testing.integration.TestServiceClient")
    applicationName = "test-service-client"
    outputDir = tasks.named<CreateStartScripts>("startScripts").get().outputDir
    classpath = tasks.named<CreateStartScripts>("startScripts").get().classpath
}

val testServiceServerStartScripts = tasks.register<CreateStartScripts>("testServiceServerStartScripts") {
    mainClass.set("io.grpc.testing.integration.TestServiceServer")
    applicationName = "test-service-server"
    outputDir = tasks.named<CreateStartScripts>("startScripts").get().outputDir
    classpath = tasks.named<CreateStartScripts>("startScripts").get().classpath
}

tasks.named("startScripts") {
    dependsOn(testServiceClientStartScripts)
    dependsOn(testServiceServerStartScripts)
}

tasks.withType<AbstractPublishToMaven> {
    enabled = false
}
