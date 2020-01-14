load("//tools/build_defs/kotlin:rules.bzl", "kt_jvm_library")

licenses(["notice"])

kt_jvm_library(
    name = "routeguide",
    srcs = [
        "Database.kt",
        "Points.kt",
        "RouteGuideClient.kt",
        "RouteGuideServer.kt",
    ],
    runtime_deps = [
        "//third_party/java/netty_tcnative",
    ],
    deps = [
        "//google/protobuf:duration_java_proto",
        "//java/com/google/common/base",
        "//java/com/google/common/io",
        "//java/com/google/protobuf/util:javatime",
        "//java/com/google/protobuf/util:json",
        "//third_party/java/grpc:core",
        "//third_party/java/grpc:netty",
        "//third_party/kotlin/grpc_kotlin/example/src/main/proto:route_guide_grpc_kotlin",
        "//third_party/kotlin/grpc_kotlin/example/src/main/proto:route_guide_proto_kotlin",
        "//third_party/kotlin/grpc_kotlin/example/src/main/resources/io/grpc/examples/routeguide:resources",
        "//third_party/kotlin/kotlinx_coroutines",
    ],
)

java_binary(
    name = "client",
    main_class = "io.grpc.examples.routeguide.RouteGuideClient",
    runtime_deps = [":routeguide"],
)

java_binary(
    name = "server",
    main_class = "io.grpc.examples.routeguide.RouteGuideServer",
    runtime_deps = [":routeguide"],
)
