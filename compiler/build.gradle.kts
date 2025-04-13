import com.google.protobuf.gradle.*

plugins {
    application
}

application {
    mainClass.set("io.grpc.kotlin.generator.GeneratorRunner")
}

java {
    withSourcesJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    // Kotlin and Java
    implementation(libs.kotlinx.coroutines.core)

    // Grpc and Protobuf
    implementation(project(":stub"))
    implementation(libs.grpc.protobuf)

    // Misc
    implementation(kotlin("reflect"))
    implementation(libs.kotlinpoet)
    implementation(libs.truth)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.guava)
    testImplementation(libs.jimfs)
    testImplementation(libs.protobuf.gradle.plugin)
    testImplementation(libs.protobuf.java)
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.mockito.core)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)

    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })

    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

publishing {
    publications {
        named<MavenPublication>("maven") {
            pom {
                name.set("gRPC Kotlin Compiler")
                artifactId = "protoc-gen-grpc-kotlin"
                description.set("gRPC Kotlin protoc compiler plugin")
            }

            artifact(tasks.jar) {
                classifier = "jdk8"
            }
        }
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
            path = tasks.jar.get().archiveFile.get().asFile.absolutePath
        }
    }
    generateProtoTasks {
        all().forEach {
            if (it.name.startsWith("generateTestProto")) {
                it.dependsOn("jar")
            }

            it.plugins {
                id("grpc")
                id("grpckt")
            }
        }
    }
}
