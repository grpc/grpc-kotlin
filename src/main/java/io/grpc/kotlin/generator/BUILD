load("//tools/build_defs/kotlin:rules.bzl", "kt_jvm_library")

licenses(["notice"])

package(default_visibility = ["//third_party/kotlin/grpc_kotlin:internal"])

kt_jvm_library(
    name = "generator",
    srcs = glob(["*.kt"]),
    deps = [
        "//java/com/google/common/annotations",
        "//java/com/google/common/base",
        "//java/com/google/protobuf",
        "//net/proto2/compiler/proto:plugin_java_proto",
        "//third_party/java/grpc:core",
        "//third_party/kotlin/grpc_kotlin:client",
        "//third_party/kotlin/grpc_kotlin:context",
        "//third_party/kotlin/grpc_kotlin:server",
        "//third_party/kotlin/kotlinpoet",
        "//third_party/kotlin/kotlinx_coroutines",
        "//third_party/kotlin/protoc_plugin_common",
    ],
)

java_binary(
    name = "GeneratorRunner",
    main_class = "io.grpc.kotlin.generator.GeneratorRunner",
    runtime_deps = [":generator"],
)
