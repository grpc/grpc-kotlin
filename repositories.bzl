load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# For use with maven_install's artifacts.
# maven_install(
#     ...
#     artifacts = [
#         # Your own deps
#     ] + IO_GRPC_GRPC_KOTLIN_ARTIFACTS + IO_GRPC_GRPC_JAVA_ARTIFACTS,
# )
IO_GRPC_GRPC_KOTLIN_ARTIFACTS = [
    "com.google.guava:guava:29.0-android",
    "com.squareup:kotlinpoet:1.5.0",
    "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1",
    "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.1",
    "org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.6.1",
]

# For use with maven_install's override_targets.
# maven_install(
#     ...
#     override_targets = dict(
#         IO_GRPC_GRPC_JAVA_OVERRIDE_TARGETS.items() +
#         IO_GRPC_GRPC_KOTLIN_OVERRIDE_TARGETS.items(),
#         "your.target:artifact": "@//third_party/artifact",
#     )
IO_GRPC_GRPC_KOTLIN_OVERRIDE_TARGETS = dict()

# Call this after compat_repositories() to load all dependencies.
def grpc_kt_repositories():
    """Imports dependencies for kt_jvm_grpc.bzl"""
    if not native.existing_rule("io_bazel_rules_kotlin"):
        io_bazel_rules_kotlin()
    if not native.existing_rule("com_google_protobuf"):
        com_google_protobuf()
    if not native.existing_rule("io_grpc_grpc_java"):
        io_grpc_grpc_java()

def io_bazel_rules_kotlin():
    rules_kotlin_version = "1.6.0-RC-1"
    rules_kotlin_sha = "f1a4053eae0ea381147f5056bb51e396c5c494c7f8d50d0dee4cc2f9d5c701b0"
    http_archive(
        name = "io_bazel_rules_kotlin",
        urls = ["https://github.com/bazelbuild/rules_kotlin/releases/download/%s/rules_kotlin_release.tgz" % rules_kotlin_version],
        sha256 = rules_kotlin_sha,
    )

def com_google_protobuf():
    protobuf_version = "3.20.1"
    protobuf_sha = "3a400163728db996e8e8d21c7dfb3c239df54d0813270f086c4030addeae2fad"

    http_archive(
        name = "com_google_protobuf",
        sha256 = protobuf_sha,
        strip_prefix = "protobuf-%s" % protobuf_version,
        urls = ["https://github.com/protocolbuffers/protobuf/releases/download/v%s/protobuf-all-%s.tar.gz" % (protobuf_version, protobuf_version)],
    )

def io_grpc_grpc_java():
    http_archive(
        name = "io_grpc_grpc_java",
        sha256 = "2f2ca0701cf23234e512f415318bfeae00036a980f6a83574264f41c0201e5cd",
        strip_prefix = "grpc-java-1.46.0",
        url = "https://github.com/grpc/grpc-java/archive/v1.46.0.zip",
    )
