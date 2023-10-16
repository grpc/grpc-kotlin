import com.google.protobuf.gradle.*
import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

plugins {
    id("org.jetbrains.dokka") version "1.9.10"
}

repositories {
    google()
    mavenCentral()

    // for Dokka
    maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:${rootProject.ext["coroutinesVersion"]}")

    // Grpc
    api("io.grpc:grpc-stub:${rootProject.ext["grpcVersion"]}")

    // Java
    api("javax.annotation:javax.annotation-api:1.3.2")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:${rootProject.ext["coroutinesVersion"]}")
    testImplementation("com.google.truth.extensions:truth-proto-extension:1.1.5")
    testImplementation("io.grpc:grpc-protobuf:${rootProject.ext["grpcVersion"]}")
    testImplementation("io.grpc:grpc-testing:${rootProject.ext["grpcVersion"]}") // gRCP testing utilities
}

java {
    withSourcesJar()
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
            if (it.name.startsWith("generateTestProto")) {
                it.dependsOn(":compiler:jar")
            }

            it.plugins {
                id("grpc")
                id("grpckt")
            }
        }
    }
}

tasks.create<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    includeEmptyDirs = false
    from(tasks.named("dokkaHtml"))
}

tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets {
        named("main") {
            sourceLink {
                localDirectory.set(file("src/main/java"))
                remoteUrl.set(URL("https://github.com/grpc/grpc-kotlin/blob/master/stub/src/main/java"))
                remoteLineSuffix.set("#L")
            }

            noStdlibLink.set(false)
            noJdkLink.set(false)
            reportUndocumented.set(true)

            externalDocumentationLink(
                url = "https://grpc.github.io/grpc-java/javadoc/",
                packageListUrl = "https://grpc.github.io/grpc-java/javadoc/element-list"
            )

            externalDocumentationLink(
                url = "https://kotlinlang.org/api/kotlinx.coroutines/"
            )

            perPackageOption {
                matchingRegex.set("io.grpc.testing")
                suppress.set(true)
            }

            perPackageOption {
                matchingRegex.set("io.grpc.kotlin.generator")
                suppress.set(true)
            }
        }
    }
}

tasks.named<Jar>("sourcesJar") {
    exclude("**/*.bazel")
}

publishing {
    publications {
        named<MavenPublication>("maven") {
            from(components["java"])

            artifact(tasks.named("javadocJar"))

            pom {
                name.set("gRPC Kotlin Stub")
                artifactId = "grpc-kotlin-stub"
                description.set("Kotlin-based stubs for gRPC services")
            }
        }
    }
}
