# gRPC-Kotlin/JVM - An RPC library and framework

[![Gradle Build Status][]](https://github.com/grpc/grpc-kotlin/actions?query=workflow%3A%22Gradle+Build%22)
[![Bazel Build Status][]](https://github.com/grpc/grpc-kotlin/actions?query=workflow%3A%22Bazel+Build%22)

[![grpc-kotlin-stub][label:stub]][maven:stub]
[![protoc-gen-grpc-kotlin][label:plugin]][maven:plugin]

A Kotlin/JVM implementation of [gRPC](https://grpc.io): A high performance, open
source, general RPC framework that puts mobile and HTTP/2 first.

This repo includes the sources for the following:

- [protoc-gen-grpc-kotlin](compiler): A [protoc][] plugin for generating Kotlin
  gRPC client-stub and server plumbing code.

  > **Note:** The Kotlin protoc plugin uses the [Java protoc plugin][gen-java]
  > behind the scenes to **generate _message types_ as _Java classes_**.
  > Generation of Kotlin sources for proto messages is being discussed in
  > [protocolbuffers/protobuf#3742][].

- [grpc-kotlin-stub](stub): A Kotlin implementation of gRPC, providing runtime
  support for client-stubs and server-side code.

For more information, see the following [Kotlin/JVM pages from grpc.io][]:

- [Quick start][]
- [Basics tutorial][]
- [API reference][]

How-to pages from this repo:

- [Contributing](CONTRIBUTING.md)
- [Building gRPC-Kotlin](BUILDING.md)

Note that [official releases][] are [published to Maven Central][].

## Upgrading

### 1.0.0 -> 1.1.0

The `grpc-kotlin-stub-lite` library no longer exists as `grpc-kotlin-stub` no longer has a dependency on
`grpc-protobuf` or `protobuf-java-util`.  But this means you now need to specify those or the lite dependencies when
you use `grpc-kotlin-stub`.  For examples, see: [examples/stub/build.gradle.kts](examples/stub/build.gradle.kts),
[examples/stub-lite/build.gradle.kts](examples/stub-lite/build.gradle.kts), or
[examples/stub-android/build.gradle.kts](examples/stub-android/build.gradle.kts).

The `javax.annotation:javax.annotation-api` dependency is now transitive so you do not need to specify it manually.


[API Reference]: https://grpc.io/docs/languages/kotlin/api/
[Basics tutorial]: https://grpc.io/docs/languages/kotlin/basics/
[Bazel Build Status]: https://github.com/grpc/grpc-kotlin/workflows/Bazel%20Build/badge.svg
[gen-java]: https://github.com/grpc/grpc-java/tree/master/compiler
[Gradle Build Status]: https://github.com/grpc/grpc-kotlin/workflows/Gradle%20Build/badge.svg
[Kotlin/JVM pages from grpc.io]: https://grpc.io/docs/languages/kotlin/
[label:plugin]: https://img.shields.io/maven-central/v/io.grpc/protoc-gen-grpc-kotlin.svg?label=protoc-gen-grpc-kotlin
[label:stub]: https://img.shields.io/maven-central/v/io.grpc/grpc-kotlin-stub.svg?label=grpc-kotlin-stub
[maven:plugin]: https://search.maven.org/search?q=g:%22io.grpc%22%20AND%20a:%22protoc-gen-grpc-kotlin%22
[maven:stub]: https://search.maven.org/search?q=g:%22io.grpc%22%20AND%20a:%22grpc-kotlin-stub%22
[official releases]: https://github.com/grpc/grpc-kotlin/releases
[protoc]: https://github.com/protocolbuffers/protobuf#protocol-compiler-installation
[protocolbuffers/protobuf#3742]: https://github.com/protocolbuffers/protobuf/issues/3742
[published to Maven Central]: https://search.maven.org/search?q=g:io.grpc%20AND%20grpc-kotlin
[Quick start]: https://grpc.io/docs/languages/kotlin/quickstart/
