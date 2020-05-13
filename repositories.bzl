load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

IO_GRPC_GRPC_KOTLIN_ARTIFACTS = [
    "com.google.guava:guava:29.0-jre",
    "com.squareup:kotlinpoet:1.5.0",
    "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.5",
]

IO_GRPC_GRPC_KOTLIN_OVERRIDE_TARGETS = dict()

def grpc_kt_repositories():
    if not native.existing_rule("bazel_build_rules_android"):
        _bazel_build_rules_android()
    if not native.existing_rule("io_bazel_rules_kotlin"):
        _io_bazel_rules_kotlin()
    if not native.existing_rule("com_google_protobuf"):
        _com_google_protobuf()

def _bazel_build_rules_android():
    http_archive(
        name = "build_bazel_rules_android",
        urls = ["https://github.com/bazelbuild/rules_android/archive/v0.1.1.zip"],
        sha256 = "cd06d15dd8bb59926e4d65f9003bfc20f9da4b2519985c27e190cddc8b7a7806",
        strip_prefix = "rules_android-0.1.1",
    )

def _io_bazel_rules_kotlin():
    rules_kotlin_version = "b40d920c5a5e044c541513f0d5e9260d0a4579c0"
    http_archive(
        name = "io_bazel_rules_kotlin",
        urls = ["https://github.com/bazelbuild/rules_kotlin/archive/%s.zip" % rules_kotlin_version],
        sha256 = "3dadd0ad7272be6b1ed1274f62cadd4a1293c89990bcd7b4af32637a70ada63e",
        type = "zip",
        strip_prefix = "rules_kotlin-%s" % rules_kotlin_version,
    )

def _com_google_protobuf():
    http_archive(
        name = "com_google_protobuf",
        sha256 = "60d2012e3922e429294d3a4ac31f336016514a91e5a63fd33f35743ccfe1bd7d",
        strip_prefix = "protobuf-3.11.0",
        urls = ["https://github.com/protocolbuffers/protobuf/archive/v3.11.0.zip"],
    )
