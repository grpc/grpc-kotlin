# Kotlin extensions and augmentations for gRPC APIs.

package(
    default_visibility = [
        ":approved_clients",
        ":internal",
    ],
)

licenses(["notice"])

exports_files(["LICENSE"])

package_group(
    name = "internal",
    packages = [
        "//third_party/bazel_rules/rules_kotlin/...",
        "//third_party/kotlin/grpc_kotlin/...",
        "//tools/build_defs/kotlin/...",
    ],
)

package_group(
    name = "approved_clients",
    packages = [],
)

alias(
    name = "client",
    actual = "//third_party/kotlin/grpc_kotlin/src/main/java/io/grpc/kotlin:client",
)

alias(
    name = "client_android",
    actual = "//third_party/kotlin/grpc_kotlin/src/main/java/io/grpc/kotlin:client_android",
)

alias(
    name = "context",
    actual = "//third_party/kotlin/grpc_kotlin/src/main/java/io/grpc/kotlin:context",
)

alias(
    name = "context_android",
    actual = "//third_party/kotlin/grpc_kotlin/src/main/java/io/grpc/kotlin:context_android",
)

alias(
    name = "server",
    actual = "//third_party/kotlin/grpc_kotlin/src/main/java/io/grpc/kotlin:server",
)

# TODO(lowasser): gRPC always seems to generate the server logic, even for lite?
alias(
    name = "server_android",
    actual = "//third_party/kotlin/grpc_kotlin/src/main/java/io/grpc/kotlin:server_android",
)

alias(
    name = "GeneratorRunner",
    actual = "//third_party/kotlin/grpc_kotlin/src/main/java/io/grpc/kotlin/generator:GeneratorRunner",
)
