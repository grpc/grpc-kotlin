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
