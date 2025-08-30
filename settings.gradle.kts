rootProject.name = "grpc-kotlin"

include("stub", "compiler", "interop_testing", "integration_testing")

dependencyResolutionManagement {
  @Suppress("UnstableApiUsage")
  repositories {
    mavenCentral()
    maven("https://repo.gradle.org/gradle/libs-releases")
  }
}
