import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(gradleTestKit())
    testImplementation(libs.testcontainers)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.named<Test>("test") {
    val examplesDir = File(rootDir, "examples")
    inputs.dir(examplesDir)
    dependsOn(":compiler:publishAllPublicationsToMavenRepository")
    dependsOn(":stub:publishAllPublicationsToMavenRepository")

    useJUnitPlatform()

    testLogging {
        showStandardStreams = true
        exceptionFormat = TestExceptionFormat.FULL
        events(TestLogEvent.STARTED, TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }

    retry {
        maxRetries = 1
        maxFailures = 1
    }

    systemProperties["grpc-kotlin-version"] = project.version
    systemProperties["examples-dir"] = examplesDir
    systemProperties["test-repo"] = (publishing.repositories.getByName("maven") as MavenArtifactRepository).url

    /*
    val properties = Properties()
    if (rootProject.file("local.properties").exists()) {
        properties.load(rootProject.file("local.properties").inputStream())
        environment("ANDROID_HOME", properties.getProperty("sdk.dir"))
    }
     */

    // todo: cleanup copyExamples.destinationDir or move copy to tests
}