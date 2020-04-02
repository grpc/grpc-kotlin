gRPC Kotlin Codegen Plugin for Protobuf Compiler
==============================================

This generates the Kotlin interfaces out of the service definition from a
`.proto` file. It works with the Protobuf Compiler (`protoc`).

Normally you don't need to compile the codegen by yourself, since pre-compiled
binaries for common platforms are available on Maven Central:

1. Navigate to https://mvnrepository.com/artifact/io.grpc/protoc-gen-grpc-kotlin
2. Select a package version
3. Click "Files: View All"

However, if the pre-compiled binaries are not compatible with your system,
you may want to build your own codegen.

## System requirement

* Linux, Mac OS X with Clang, or Windows with MSYS2
* Java 7 or up
* [Protobuf](https://github.com/google/protobuf) 3.0.0-beta-3 or up

## Compiling and testing the codegen

Change to the `compiler` directory:

```
$ cd $GRPC_KOTLIN_ROOT/compiler
```

To compile the plugin:

```
$ ../gradlew :protoc-gen-grpc-kotlin:build./gradlew
```

To test the plugin with the compiler:

```
$ ../gradlew test
```

You will see a `PASS` if the test succeeds.

To compile a proto file and generate Kotlin interfaces out of the service definitions:

```
$ protoc --plugin=protoc-gen-grpc-kotlin=build/install/protoc-gen-grpc-kotlin/bin/protoc-gen-grpc-kotlin \
  --grpckt_out="$OUTPUT_FILE" --proto_path="$DIR_OF_PROTO_FILE" "$PROTO_FILE"
```

## Installing the codegen to Maven local repository

This will compile a codegen and put it under your `~/.m2/repository`. This
will make it available to any build tool that pulls codegens from Maven
repositories.

```
$ ../gradlew publishToMavenLocal
```
