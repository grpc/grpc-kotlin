import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

plugins {
    kotlin("jvm") version "1.3.72" apply false
    id("com.google.protobuf") version "0.8.15" apply false
}

ext["grpcVersion"] = "1.36.0" // CURRENT_GRPC_VERSION
ext["protobufVersion"] = "3.14.0"
ext["coroutinesVersion"] = "1.3.3"

subprojects {

    apply {
        plugin("java")
        plugin("org.jetbrains.kotlin.jvm")
        plugin("com.google.protobuf")
        plugin("maven-publish")
        plugin("signing")
    }

    group = "io.grpc"
    version = "1.2.0" // CURRENT_GRPC_KOTLIN_VERSION

    repositories {
        mavenCentral()
    }

    tasks.withType<JavaCompile> {
        sourceCompatibility = JavaVersion.VERSION_1_7.toString()
        targetCompatibility = JavaVersion.VERSION_1_7.toString()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = JavaVersion.VERSION_1_6.toString()
        }
    }

    tasks.withType<Test> {
        testLogging {
            showStandardStreams = true

            // set options for log level LIFECYCLE
            events = setOf(
                    TestLogEvent.FAILED,
                    TestLogEvent.PASSED,
                    TestLogEvent.SKIPPED,
                    TestLogEvent.STANDARD_OUT
            )

            exceptionFormat = TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true

            // set options for log level DEBUG and INFO
            debug {
                events = setOf(
                    TestLogEvent.STARTED,
                    TestLogEvent.FAILED,
                    TestLogEvent.PASSED,
                    TestLogEvent.SKIPPED,
                    TestLogEvent.STANDARD_ERROR,
                    TestLogEvent.STANDARD_OUT
                )

                exceptionFormat = TestExceptionFormat.FULL
            }

            info.events = debug.events
            info.exceptionFormat = debug.exceptionFormat
        }

        afterSuite(
            KotlinClosure2({ desc: TestDescriptor, result: TestResult ->
                if (desc.parent == null) { // will match the outermost suite
                    println("Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} successes, ${result.failedTestCount} failures, ${result.skippedTestCount} skipped)")
                }
            })
        )
    }

    extensions.getByType<PublishingExtension>().publications {
        create<MavenPublication>("maven") {
            pom {
                url.set("https://github.com/grpc/grpc-kotlin")

                scm {
                    connection.set("scm:git:https://github.com/grpc/grpc-kotlin.git")
                    developerConnection.set("scm:git:git@github.com:grpc/grpc-kotlin.git")
                    url.set("https://github.com/grpc/grpc-kotlin")
                }

                licenses {
                    license {
                        name.set("Apache 2.0")
                        url.set("https://opensource.org/licenses/Apache-2.0")
                    }
                }

                developers {
                    developer {
                        id.set("grpc.io")
                        name.set("gRPC Contributors")
                        email.set("grpc-io@googlegroups.com")
                        url.set("https://grpc.io/")
                        organization.set("gRPC Authors")
                        organizationUrl.set("https://www.google.com")
                    }
                }
            }
        }
    }

    extensions.getByType<PublishingExtension>().repositories {
        maven {
            val snapshotUrl = uri("https://oss.sonatype.org/content/repositories/snapshots")
            val releaseUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
            url = if (version.safeAs<String>()?.endsWith("SNAPSHOT") == true) snapshotUrl else releaseUrl
            credentials {
                username = project.findProperty("sonatypeUsername")?.safeAs() ?: ""
                password = project.findProperty("sonatypePassword")?.safeAs() ?: ""
            }
        }
    }

    extensions.getByType<SigningExtension>().sign(extensions.getByType<PublishingExtension>().publications.named("maven").get())

    tasks.withType<Sign> {
        onlyIf { project.hasProperty("signing.keyId") }
    }

}
