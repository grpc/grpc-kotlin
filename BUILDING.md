# Building gRPC-Kotlin

Building should only be necessary if you are making changes to gRPC-Kotlin, or if
you'd like to use or test a version of gRPC-Kotlin that hasn't been released
yet.

Use the following gradle command to build gRPC-Kotlin:

```console
$ ./gradlew build
```

Install the built artifacts into your local Maven repository so that you can use
them in your projects:

```console
$ ./gradlew publishToMavenLocal
```

Ensure that you configure your project build to use the local version of
gRPC-Kotlin.


## Releasing

1. [Generate a changelog](https://github.com/grpc/grpc-kotlin/releases/new) and prepend it to [CHANGELOG.md](CHANGELOG.md)
2. Create a Pull Request with updated versions in: [build.gradle.kts](build.gradle.kts) and [examples/build.gradle.kts](examples/build.gradle.kts)
3. Once merged, tag the release with `vX.Y.Z` and push the tag.  This will kick off a GitHub Action that does the actual release.