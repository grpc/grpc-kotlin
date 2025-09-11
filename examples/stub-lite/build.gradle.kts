plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.protobuf)
}

dependencies {
  protobuf(project(":protos"))

  api(libs.kotlinx.coroutines.core)

  api(libs.grpc.stub)
  api(libs.grpc.protobuf.lite)
  api(libs.grpc.kotlin.stub)
  api(libs.protobuf.kotlin.lite)
}

kotlin { jvmToolchain(17) }

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  compilerOptions { freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn") }
}

protobuf {
  protoc { artifact = libs.protoc.asProvider().get().toString() }
  plugins {
    create("grpc") { artifact = libs.protoc.gen.grpc.java.get().toString() }
    create("grpckt") { artifact = libs.protoc.gen.grpc.kotlin.get().toString() + ":jdk8@jar" }
  }
  generateProtoTasks {
    all().forEach {
      it.builtins {
        named("java") { option("lite") }
        create("kotlin") { option("lite") }
      }
      it.plugins {
        create("grpc") { option("lite") }
        create("grpckt") { option("lite") }
      }
    }
  }
}
