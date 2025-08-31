# gRPC-Kotlin/JVM - An RPC library and framework

## Overview

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
- [Maven / Gradle Plugin instructions]
- [Basics tutorial][]
- [API reference][]

How-to pages from this repo:

- [Contributing](CONTRIBUTING.md)
- [Building gRPC-Kotlin](BUILDING.md)

Note that [official releases][] are [published to Maven Central][].

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
[Maven / Gradle Plugin instructions]: compiler/README.md

## Developer

### Bazel 8+

Add the following to your `MODULE.bazel` with a suitable [version](https://registry.bazel.build/modules/grpc_kotlin).

```starlark
bazel_dep(name = "grpc_kotlin", version = "VERSION")
```

### Bazel 7

In addition to `bazel_dep`, create a patch to remove `ignore_directories` from `REPO.bazel` (**check content of the correct version**) and apply it using:

```starlark
bazel_dep(name = "grpc_kotlin", version = "VERSION")

single_version_override(
    module_name = "grpc_kotlin",
    version = "VERSION",
    patches = [":remove_ignore_directories.patch"],
    patch_strip = 1,
)
```

An example of `remove_ignore_directories.patch`:

```patch
--- a/REPO.bazel
+++ b/REPO.bazel
@@ -1 +0,0 @@
-ignore_directories(["bzl-examples", "formatter", "**/bin"])
```

### Legacy WORKSPACE

No longer supported.

## Maintainer

### Test

Please note that the max version of JAVA supported is 22. If you use a newer version, first set your `PATH`:

```bash
$ PATH="$YOUR_JDK_PATH/bin:$PATH"
```

Then run the following command to test your local changes before committing:

```bash
$ bazelisk clean && ./gradlew clean build --parallel && ./gradlew publishToMavenLocal && bazelisk test ... && cd bzl-examples/bzlmod && bazelisk clean && bazelisk test ... && cd -
```

### Release: Sonatype

Make sure that [Release Github Action](/.github/workflows/release.yaml) succeeds and artifacts are uploaded to Maven.

If not, contact @bshaffer.

### Publish to BCR

Publishing to BCR requires manual operation on the PR and hence can't be fully automated in [release.yaml](/.github/workflows/release.yaml):

1. Run the Publish to BCR Github Action: [publish.yaml](/.github/workflows/publish.yaml).
2. Check logs for link to the PR.
3. Manually comment `@bazel-io skip_check unstable_url` on the generated PR.

After the PR is merged, make sure the new version is visible in [BCR](https://registry.bazel.build/modules/grpc_kotlin).
