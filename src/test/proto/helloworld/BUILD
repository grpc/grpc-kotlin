load("//third_party/java/grpc:build_defs.bzl", "java_grpc_library")

licenses(["notice"])

package(default_visibility = ["//third_party/kotlin/grpc_kotlin/src/test:__subpackages__"])

proto_library(
    name = "helloworld_proto",
    srcs = ["helloworld.proto"],
    has_services = 1,
)

java_proto_library(
    name = "helloworld_java_proto",
    deps = [":helloworld_proto"],
)

java_grpc_library(
    name = "helloworld_java_grpc",
    srcs = [":helloworld_proto"],
    deps = [":helloworld_java_proto"],
)
