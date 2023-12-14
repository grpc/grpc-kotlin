package io.grpc.kotlin

import org.gradle.internal.impldep.org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File
import java.nio.file.Path

class ExamplesTest {

    // todo: add test to verify jdk8 usage
    @Test
    fun server_client(@TempDir tempDir: Path) {
        val grpcKotlinVersion = System.getProperty("grpc-kotlin-version")
        val examplesDir = System.getProperty("examples-dir")
        val testRepo = System.getProperty("test-repo")

        FileUtils.copyDirectory(File(examplesDir), tempDir.toFile())

        val libsVersionsToml = File(tempDir.toFile(), "gradle/libs.versions.toml")

        val versionRegex = Regex("""version = "(.*)"""")

        val libsVersionsTomlNewLines = libsVersionsToml.readLines().map { line ->
            if (line.contains("grpc-kotlin-stub") || line.contains("protoc-gen-grpc-kotlin")) {
                line.replace(versionRegex, """version = "$grpcKotlinVersion"""")
            }
            else {
                line
            }
        }

        libsVersionsToml.writeText(libsVersionsTomlNewLines.joinToString("\n"))

        val settingsGradle = File(tempDir.toFile(), "settings.gradle.kts")
        val settingsGradleNewLines = settingsGradle.readLines().map { line ->
            if (line.contains("mavenCentral()")) {
                """
                    mavenCentral()
                    maven(uri("$testRepo"))
                """
            }
            else {
                line
            }
        }
        settingsGradle.writeText(settingsGradleNewLines.joinToString("\n"))

        val dependencyResult = GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withArguments(":stub:dependencies")
            .build()

        assertTrue(dependencyResult.output.contains("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion"))

        GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withArguments(":server:jibDockerBuild", "--image=grpc-kotlin-examples-server")
            .build()

        val container = GenericContainer("grpc-kotlin-examples-server")
            .withExposedPorts(50051)
            .waitingFor(Wait.forListeningPort())

        container.start()

        val clientResult = GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withEnvironment(mapOf("PORT" to container.getFirstMappedPort().toString()))
            .withArguments(":client:HelloWorldClient")
            .build()

        assertTrue(clientResult.output.contains("Received: Hello world"))
    }

}