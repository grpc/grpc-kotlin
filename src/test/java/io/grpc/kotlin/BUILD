load("//tools/build_defs/kotlin:rules.bzl", "kt_jvm_library", "kt_jvm_proto_library")
load("//testing/build_defs:junit_test_suites.bzl", "junit_test_suites")
load("//third_party/bazel_rules/rules_kotlin/kotlin:rules.bzl", "kt_jvm_grpc_library")

licenses(["notice"])

package(
    default_testonly = 1,
    default_visibility = ["//visibility:private"],
)

kt_jvm_library(
    name = "common",
    srcs = ["AbstractCallsTest.kt"],
    deps = [
        "//java/com/google/common/util/concurrent",
        "//third_party/java/grpc:context",
        "//third_party/java/grpc:core",
        "//third_party/java/grpc:inprocess",
        "//third_party/java/grpc:testing",
        "//third_party/java/junit",
        "//third_party/kotlin/grpc_kotlin/src/test/proto/helloworld:helloworld_java_grpc",
        "//third_party/kotlin/grpc_kotlin/src/test/proto/helloworld:helloworld_java_proto",
        "//third_party/kotlin/kotlin:kotlin_reflect",
        "//third_party/kotlin/kotlinx_coroutines",
        "//third_party/kotlin/kotlinx_coroutines:kotlinx_coroutines_debug",
    ],
)

kt_jvm_library(
    name = "tests",
    srcs = [
        "AbstractCoroutineServerImplTest.kt",
        "ClientCallsTest.kt",
        "FlowControlTest.kt",
        "GrpcContextElementTest.kt",
        "ServerCallsTest.kt",
    ],
    deps = [
        ":common",
        "//java/com/google/common/util/concurrent",
        "//java/com/google/protobuf",
        "//java/com/google/testing/testsize:annotations",
        "//third_party/java/grpc:context",
        "//third_party/java/grpc:core",
        "//third_party/java/grpc:inprocess",
        "//third_party/java/grpc:stub",
        "//third_party/java/junit",
        "//third_party/java/mockito",
        "//third_party/java/truth",
        "//third_party/java/truth/extensions:proto",
        "//third_party/kotlin/grpc_kotlin:client",
        "//third_party/kotlin/grpc_kotlin:context",
        "//third_party/kotlin/grpc_kotlin:server",
        "//third_party/kotlin/grpc_kotlin/src/test/proto/helloworld:helloworld_java_grpc",
        "//third_party/kotlin/grpc_kotlin/src/test/proto/helloworld:helloworld_java_proto",
        "//third_party/kotlin/kotlinx_coroutines",
        "//third_party/kotlin/kotlinx_coroutines:kotlinx_coroutines_debug",
        "//third_party/kotlin/mockito_kotlin",
    ],
)

junit_test_suites(
    name = "junit",
    timeout = {"medium": "short"},
    sizes = [
        "medium",
    ],
    deps = [":tests"],
)

kt_jvm_grpc_library(
    name = "helloworld_kt_grpc",
    srcs = ["//third_party/kotlin/grpc_kotlin/src/test/proto/helloworld:helloworld_proto"],
)

kt_jvm_proto_library(
    name = "helloworld_kt_proto",
    deps = ["//third_party/kotlin/grpc_kotlin/src/test/proto/helloworld:helloworld_proto"],
)

kt_jvm_library(
    name = "generated_code_tests",
    srcs = ["GeneratedCodeTest.kt"],
    deps = [
        ":common",
        ":helloworld_kt_grpc",
        ":helloworld_kt_proto",
        "//java/com/google/testing/testsize:annotations",
        "//third_party/java/grpc:core",
        "//third_party/java/grpc:stub",
        "//third_party/java/grpc:testing",
        "//third_party/java/junit",
        "//third_party/java/truth",
        "//third_party/java/truth/extensions:proto",
        "//third_party/kotlin/kotlinx_coroutines",
        "//third_party/kotlin/kotlinx_coroutines:kotlinx_coroutines_debug",
    ],
)

junit_test_suites(
    name = "generated_code_junit",
    timeout = {"medium": "short"},
    sizes = [
        "medium",
    ],
    suffix = "GeneratedCode",
    deps = [":generated_code_tests"],
)
