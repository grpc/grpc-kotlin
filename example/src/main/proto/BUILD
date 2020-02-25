load("//tools/build_defs/kotlin:rules.bzl", "kt_jvm_grpc_library")

package(
    default_visibility = ["//third_party/kotlin/grpc_kotlin:__subpackages__"],
)

licenses(["notice"])

proto_library(
    name = "route_guide_proto",
    srcs = ["route_guide.proto"],
    has_services = 1,
    deps = ["//google/protobuf:duration"],
)

java_proto_library(
    name = "route_guide_proto_java",
    deps = [":route_guide_proto"],
)

kt_jvm_grpc_library(
    name = "route_guide_grpc_kotlin",
    srcs = [":route_guide_proto"],
)
