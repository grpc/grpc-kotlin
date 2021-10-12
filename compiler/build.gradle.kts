import com.google.protobuf.gradle.*

plugins {
    application
}

application {
    mainClass.set("io.grpc.kotlin.generator.GeneratorRunner")
}

dependencies {
    // Kotlin and Java
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${rootProject.ext["coroutinesVersion"]}")

    // Grpc and Protobuf
    implementation(project(":stub"))
    implementation("io.grpc:grpc-protobuf:${rootProject.ext["grpcVersion"]}")

    // Misc
    implementation(kotlin("reflect"))
    implementation("com.squareup:kotlinpoet:1.5.0")
    implementation("com.google.truth:truth:1.0.1")

    // Testing
    testImplementation("junit:junit:4.12")
    testImplementation("com.google.guava:guava:28.2-jre")
    testImplementation("com.google.jimfs:jimfs:1.1")
    testImplementation("com.google.protobuf:protobuf-gradle-plugin:0.8.13")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.5.2")
    testImplementation("org.mockito:mockito-core:2.28.2")
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
                classifier = "jdk7"
            }
        }
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
