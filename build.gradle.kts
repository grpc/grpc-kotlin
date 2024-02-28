import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.protobuf) apply false
    alias(libs.plugins.test.retry)
    alias(libs.plugins.publish.plugin)
    alias(libs.plugins.qoomon.git.versioning)
}

group = "io.grpc"

gitVersioning.apply {
    refs {
        tag("v(?<version>.*)") {
            version = "\${ref.version}"
        }
    }

    rev {
        version = "\${commit}"
    }
}

subprojects {

    apply {
        plugin("java")
        plugin("org.jetbrains.kotlin.jvm")
        plugin("com.google.protobuf")
        plugin("org.gradle.test-retry")
        plugin("maven-publish")
        plugin("signing")
    }

    // gradle-nexus/publish-plugin needs these here too
    group = rootProject.group
    version = rootProject.version

    tasks.withType<JavaCompile> {
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = JavaVersion.VERSION_1_8.toString()
        }
    }

    tasks.withType<Test> {
        testLogging {
            // set options for log level LIFECYCLE
            events = setOf(
                    TestLogEvent.FAILED,
                    TestLogEvent.PASSED,
                    TestLogEvent.SKIPPED,
                    TestLogEvent.STANDARD_OUT
            )

            exceptionFormat = TestExceptionFormat.FULL
            showStandardStreams = true
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

        retry {
            maxRetries = 10
        }

        afterSuite(
            KotlinClosure2({ desc: TestDescriptor, result: TestResult ->
                if (desc.parent == null) { // will match the outermost suite
                    println("Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} successes, ${result.failedTestCount} failures, ${result.skippedTestCount} skipped)")
                }
            })
        )
    }

    extensions.getByType<PublishingExtension>().repositories {
        maven {
            url = uri(rootProject.layout.buildDirectory.dir("maven-repo"))
        }
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

    extensions.getByType<SigningExtension>().sign(extensions.getByType<PublishingExtension>().publications.named("maven").get())
    extensions.getByType<SigningExtension>().useInMemoryPgpKeys(System.getenv("GPG_PRIVATE_KEY"), System.getenv("GPG_PASSPHRASE"))

    tasks.withType<Sign> {
        onlyIf { System.getenv("GPG_PRIVATE_KEY") != null }
    }
}

nexusPublishing.repositories.sonatype {
    username.set(System.getenv("SONATYPE_USERNAME"))
    password.set(System.getenv("SONATYPE_PASSWORD"))
}
