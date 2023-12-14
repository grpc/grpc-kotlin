import com.google.protobuf.gradle.*
import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

plugins {
    alias(libs.plugins.dokka)
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
    implementation(libs.kotlinx.coroutines.core.jvm)

    // Grpc
    api(libs.grpc.stub)

    // Java
    api(libs.javax.annotation.api)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.kotlinx.coroutines.debug)
    testImplementation(libs.truth.proto.extension)
    testImplementation(libs.grpc.protobuf)
    testImplementation(libs.grpc.testing)
}

java {
    withSourcesJar()
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
