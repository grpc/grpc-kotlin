load("//tools/build_defs/kotlin:rules.bzl", "kt_jvm_library")
load("//testing/build_defs:junit_test_suites.bzl", "junit_test_suites")

licenses(["notice"])

package(default_testonly = 1)

# kt_jvm_library doesn't currently support resources, so we just dump them in a java_library.
java_library(
    name = "resources",
    resources = [
        "GreeterCoroutineImplBase.expected",
        "GreeterCoroutineStub.expected",
        "HelloWorldProtoGrpcKt.expected",
    ],
)

kt_jvm_library(
    name = "tests",
    srcs = glob(["*.kt"]),
    deps = [
        ":resources",
        "//java/com/google/common/io",
        "//java/com/google/protobuf",
        "//java/com/google/testing/testsize:annotations",
        "//net/proto2/compiler/proto:plugin_java_proto",
        "//third_party/java/jimfs",
        "//third_party/java/junit",
        "//third_party/java/truth",
        "//third_party/kotlin/grpc_kotlin/src/main/java/io/grpc/kotlin/generator",
        "//third_party/kotlin/grpc_kotlin/src/test/proto/helloworld:helloworld_java_grpc",
        "//third_party/kotlin/grpc_kotlin/src/test/proto/helloworld:helloworld_java_proto",
        "//third_party/kotlin/kotlinpoet",
        "//third_party/kotlin/mockito_kotlin",
        "//third_party/kotlin/protoc_plugin_common",
        "//third_party/kotlin/protoc_plugin_common:testing",
    ],
)

junit_test_suites(
    name = "junit",
    sizes = ["small"],
    deps = [":tests"],
)
