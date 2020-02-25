# Open source Stubby. World class RPC for the world.

load("//third_party/java/grpc:build_defs.bzl", "java_grpc_library")
load("//java/com/google/testing/builddefs:GenTestRules.bzl", "GenTestRules")
load("//tools/build_defs/kotlin:rules.bzl", "kt_android_library", "kt_jvm_grpc_library", "kt_jvm_library")

package(
    default_visibility = [
        "//third_party/java/grpc:__pkg__",
        "//third_party/java_src/grpc:__subpackages__",
    ],
)

licenses(["notice"])  # Apache 2.0

proto_library(
    name = "interop_testing_proto",
    testonly = 1,
    srcs = glob(["src/main/proto/grpc/testing/*.proto"]),
    has_services = 1,
    compatible_with = ["//buildenv/target:appengine"],
    deps = ["//google/protobuf:wrappers"],
)

java_proto_library(
    name = "interop_testing_java_proto",
    testonly = 1,
    compatible_with = ["//buildenv/target:appengine"],
    deps = [":interop_testing_proto"],
)

java_lite_proto_library(
    name = "interop_testing_java_proto_lite",
    testonly = 1,
    deps = [":interop_testing_proto"],
)

java_grpc_library(
    name = "interop_testing_proto_grpc",
    testonly = 1,
    srcs = [":interop_testing_proto"],
    compatible_with = ["//buildenv/target:appengine"],
    deps = [
        ":interop_testing_java_proto",
        "//google/protobuf:wrappers_java_proto",
    ],
)

kt_jvm_grpc_library(
    name = "interop_testing_kt_grpc",
    testonly = 1,
    srcs = [":interop_testing_proto"],
    compatible_with = ["//buildenv/target:appengine"],
)

java_grpc_library(
    name = "interop_testing_java_proto_lite_grpc",
    testonly = 1,
    srcs = [":interop_testing_proto"],
    constraints = ["android"],
    flavor = "lite",
    deps = [
        ":interop_testing_java_proto_lite",
    ],
)

kt_jvm_grpc_library(
    name = "interop_testing_kt_lite_grpc",
    testonly = 1,
    srcs = [":interop_testing_proto"],
    compatible_with = ["//buildenv/target:appengine"],
    flavor = "lite",
)

java_library(
    name = "resources",
    resources = glob(["src/main/resources/io/grpc/testing/integration/testdata/*"]),
)

kt_jvm_library(
    name = "interop-testing",
    testonly = 1,
    srcs = glob(
        [
            "src/main/java/io/grpc/testing/integration/*.java",
            "src/main/java/io/grpc/testing/integration/*.kt",
        ],
    ),
    compatible_with = ["//buildenv/target:appengine"],
    runtime_deps = [":resources"],
    deps = [
        ":interop_testing_java_proto",
        ":interop_testing_kt_grpc",
        "//google/protobuf:wrappers_java_proto",
        "//java/com/google/common/annotations",
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//java/com/google/common/io",
        "//java/com/google/common/net",
        "//java/com/google/common/primitives",
        "//java/com/google/common/util/concurrent",
        "//java/com/google/protobuf:protobuf_lite",
        "//third_party/java/auth_kit",
        "//third_party/java/grpc:alts",
        "//third_party/java/grpc:auth",
        "//third_party/java/grpc:context",
        "//third_party/java/grpc:core",
        "//third_party/java/grpc:netty",
        "//third_party/java/grpc:okhttp",
        "//third_party/java/grpc:protobuf_lite",
        "//third_party/java/grpc:stub",
        "//third_party/java/grpc:testing",
        "//third_party/java/grpc:util",
        "//third_party/java/jsr305_annotations",
        "//third_party/java/junit",
        "//third_party/java/netty4:handler",
        "//third_party/java/truth",
        "//third_party/kotlin/kotlin:kotlin_test",
        "//third_party/kotlin/kotlinx_coroutines",
    ],
)

kt_android_library(
    name = "interop_testing_android",
    testonly = 1,
    srcs = [
        "src/main/java/io/grpc/testing/integration/AbstractInteropTest.kt",
        "src/main/java/io/grpc/testing/integration/TestServiceImpl.kt",
        "src/main/java/io/grpc/testing/integration/Util.java",
    ],
    resource_files = glob(["src/main/resources/io/grpc/testing/integration/testdata/*"]),
    deps = [
        ":interop_testing_java_proto_lite",
        ":interop_testing_kt_lite_grpc",
        "//google/protobuf:wrappers_java_proto_lite",
        "//java/com/google/protobuf:protobuf_lite",
        "//third_party/java/android_libs/guava_jdk5",
        "//third_party/java/auth_kit:android",
        "//third_party/java/grpc:auth_android",
        "//third_party/java/grpc:context_android",
        "//third_party/java/grpc:core_android",
        "//third_party/java/grpc:protobuf_lite_android",
        "//third_party/java/grpc:stub_android",
        "//third_party/java/grpc:testing_android",
        "//third_party/java/jsr305_annotations",
        "//third_party/java/junit:junit-android",
        "//third_party/java/mockito:mockito-android",
        "//third_party/java/truth:truth-android",
        "//third_party/kotlin/kotlin:kotlin_test",
        "//third_party/kotlin/kotlinx_coroutines",
    ],
)

kt_jvm_library(
    name = "test_lib",
    testonly = 1,
    srcs = glob(
        [
            "src/test/java/**/*.java",
            "src/test/java/**/*.kt",
        ],
    ),
    deps = [
        ":interop-testing",
        ":interop_testing_java_proto",
        ":interop_testing_kt_grpc",
        "//google/protobuf:wrappers_java_proto",
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//java/com/google/common/io",
        "//java/com/google/common/reflect",
        "//java/com/google/common/util/concurrent",
        "//java/com/google/protobuf:protobuf_lite",
        "//third_party/java/grpc:context",
        "//third_party/java/grpc:core",
        "//third_party/java/grpc:inprocess",
        "//third_party/java/grpc:netty",
        "//third_party/java/grpc:okhttp",
        "//third_party/java/grpc:stub",
        "//third_party/java/grpc:testing",
        "//third_party/java/grpc:util",
        "//third_party/java/junit",
        "//third_party/java/mockito",
        "//third_party/java/netty4:common",
        "//third_party/java/netty4:handler",
        "//third_party/java/netty4:transport",
        "//third_party/java/okhttp",
        "//third_party/java/truth",
        "//third_party/kotlin/kotlin:kotlin_test",
    ],
)

GenTestRules(
    name = "GeneratedTestRules",
    test_files = glob(["src/test/java/**/*Test.java"]),
    deps = [
        ":test_lib",
        "//third_party/java/conscrypt",
        "//third_party/java/grpc:testing",
        "//third_party/java/netty_tcnative",
    ],
)

test_suite(name = "AllTests")
