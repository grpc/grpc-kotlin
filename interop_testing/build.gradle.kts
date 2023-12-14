import com.google.protobuf.gradle.*

plugins {
    application
}

dependencies {
    implementation(kotlin("test"))
    implementation(libs.kotlinx.coroutines.core)

    implementation(project(":stub"))

    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.auth)
    implementation(libs.grpc.alts)
    implementation(libs.grpc.netty)
    implementation(libs.grpc.okhttp)
    implementation(libs.grpc.testing)

    implementation(libs.protobuf.java)

    implementation(libs.truth)

    testImplementation(libs.mockito.core)
    testImplementation(libs.okhttp) {
        because("transitive dep for grpc-okhttp")
    }
}

protobuf {
    protoc {
        artifact = libs.protoc.asProvider().get().toString()
    }
    plugins {
        id("grpc") {
            artifact = libs.protoc.gen.grpc.java.get().toString()
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
