load("//tools/build_defs/kotlin:rules.bzl", "kt_jvm_library")

licenses(["notice"])

package(
    default_visibility = ["//third_party/kotlin/grpc_kotlin:__subpackages__"],
)

kt_jvm_library(
    name = "common",
    srcs = [
        "Helpers.kt",
        "Readiness.kt",
    ],
    deps = [
        "//third_party/java/grpc:core",
        "//third_party/kotlin/kotlinx_coroutines",
    ],
)

kt_jvm_library(
    name = "common_android",
    srcs = [
        "Helpers.kt",
        "Readiness.kt",
    ],
    constraints = ["android"],
    deps = [
        "//third_party/java/grpc:core_android",
        "//third_party/kotlin/kotlinx_coroutines",
    ],
)

kt_jvm_library(
    name = "client",
    srcs = [
        "AbstractCoroutineStub.kt",
        "ClientCalls.kt",
        "StubFor.kt",
    ],
    deps = [
        ":common",
        "//third_party/java/grpc:core",
        "//third_party/java/grpc:stub",
        "//third_party/kotlin/kotlinx_coroutines",
    ],
)

kt_jvm_library(
    name = "client_android",
    srcs = [
        "AbstractCoroutineStub.kt",
        "ClientCalls.kt",
        "StubFor.kt",
    ],
    constraints = ["android"],
    deps = [
        ":common_android",
        "//third_party/java/grpc:core_android",
        "//third_party/java/grpc:stub_android",
        "//third_party/kotlin/kotlinx_coroutines",
    ],
)

kt_jvm_library(
    name = "context",
    srcs = [
        "GrpcContextElement.kt",
    ],
    deps = [
        "//third_party/java/grpc:context",
        "//third_party/kotlin/kotlinx_coroutines",
    ],
)

kt_jvm_library(
    name = "context_android",
    srcs = [
        "GrpcContextElement.kt",
    ],
    constraints = ["android"],
    deps = [
        "//third_party/java/grpc:context_android",
        "//third_party/kotlin/kotlinx_coroutines",
    ],
)

kt_jvm_library(
    name = "server",
    srcs = [
        "AbstractCoroutineServerImpl.kt",
        "ServerCalls.kt",
    ],
    deps = [
        ":common",
        ":context",
        "//third_party/java/grpc:core",
        "//third_party/kotlin/kotlinx_coroutines",
    ],
)

kt_jvm_library(
    name = "server_android",
    srcs = [
        "AbstractCoroutineServerImpl.kt",
        "ServerCalls.kt",
    ],
    deps = [
        ":common_android",
        ":context_android",
        "//third_party/java/grpc:core_android",
        "//third_party/kotlin/kotlinx_coroutines",
    ],
)

# Omit a server-android for now; that seems unlikely?
