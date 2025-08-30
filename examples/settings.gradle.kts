rootProject.name = "grpc-kotlin-examples"

// when running the assemble task, ignore the android & graalvm related subprojects
if (startParameter.taskRequests.find { it.args.contains("assemble") } == null) {
  include(
    "protos",
    "stub",
    "stub-lite",
    "client",
    "native-client",
    "server",
    "stub-android",
    "android"
  )
} else {
  include("protos", "stub", "server")
}

pluginManagement {
  repositories {
    gradlePluginPortal()
    google()
  }
}

dependencyResolutionManagement {
  @Suppress("UnstableApiUsage")
  repositories {
    mavenCentral()
    google()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }
