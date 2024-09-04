import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm) apply false  // 서브 프로젝트에서 별도 적용하기 위해 비활성화
    alias(libs.plugins.protobuf) apply false    // Protocol Buffers 설정도 위와 동일
    alias(libs.plugins.test.retry)              // 테스트 실패 시 재시도
    alias(libs.plugins.publish.plugin)          // mvn 배포 설정
    alias(libs.plugins.qoomon.git.versioning)   // git 버전 관리 - 태그와 커밋 기준으로 프로젝트 버전을 관리함
}

group = "io.grpc"

// gitVersioning 설정
// 태그가 v1.0와 같은 형식일 경우 해당 버전을 사용하고, 태그가 없으면 커밋 해시를 사용
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

    // 컴파일 버전 설정
    tasks.withType<JavaCompile> {
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
    }

    // Kotlin 컴파일 버전 설정. Xjsr305=strict 옵션을 추가하여 nullability 검사를 엄격하게 설정
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = JavaVersion.VERSION_1_8.toString()
        }
    }

    // 테스트 로깅 및 재시도 설정
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

        // 테스트를 최대 10번 재시도
        retry {
            maxRetries = 10
        }

        // 테스트 결과 출력. 모든 테스트가 종료되면 테스트 성공유무, 테스트 수, 성공 수, 실패 수, 스킵 수 출력
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
