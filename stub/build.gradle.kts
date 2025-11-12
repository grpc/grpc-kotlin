import com.google.protobuf.gradle.*

plugins { alias(libs.plugins.dokka) }

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
  api(libs.protobuf.java)

  // Testing
  testImplementation(libs.junit)
  testImplementation(libs.junit.jupiter.engine)
  testImplementation(libs.truth.proto.extension)
  testImplementation(libs.grpc.protobuf)
  testImplementation(libs.grpc.testing)
  testImplementation(libs.grpc.inprocess)
}

java {
  withSourcesJar()
  toolchain { languageVersion = JavaLanguageVersion.of(17) }
}

protobuf {
  protoc { artifact = libs.protoc.asProvider().get().toString() }
  plugins {
    id("grpc") { artifact = libs.protoc.gen.grpc.java.get().toString() }
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

dokka {
  dokkaSourceSets.main {
    reportUndocumented = true

    sourceLink {
      localDirectory.set(file("src/main/java"))
      remoteUrl("https://github.com/grpc/grpc-kotlin/blob/master/stub/src/main/java")
      remoteLineSuffix.set("#L")
    }

    externalDocumentationLinks.register("grpc-java-docs") {
      url("https://grpc.github.io/grpc-java/javadoc/")
      packageListUrl("https://grpc.github.io/grpc-java/javadoc/element-list")
    }
    externalDocumentationLinks.register("kotlinx.coroutines-docs") {
      url("https://kotlinlang.org/api/kotlinx.coroutines/")
    }

    perPackageOption {
      matchingRegex.set("io.grpc.testing.*")
      suppress.set(true)
    }

    perPackageOption {
      matchingRegex.set("io.grpc.kotlin.generator.*")
      suppress.set(true)
    }
  }
}

val javadocJar by
  tasks.registering(Jar::class) {
    dependsOn(tasks.dokkaGenerate)
    archiveClassifier.set("javadoc")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    includeEmptyDirs = false
    from(layout.buildDirectory.dir("dokka/html"))
  }

tasks.named<Jar>("sourcesJar") { exclude("**/*.bazel") }

publishing {
  publications {
    named<MavenPublication>("maven") {
      from(components["java"])

      artifact(javadocJar)

      pom {
        name.set("gRPC Kotlin Stub")
        artifactId = "grpc-kotlin-stub"
        description.set("Kotlin-based stubs for gRPC services")
      }
    }
  }
}
