load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# For use with maven_install's artifacts.
# maven_install(
#     ...
#     artifacts = [
#         # Your own deps
#     ] + IO_GRPC_GRPC_KOTLIN_ARTIFACTS + IO_GRPC_GRPC_JAVA_ARTIFACTS,
# )
IO_GRPC_GRPC_KOTLIN_ARTIFACTS = [
    "com.google.guava:guava:29.0-jre",
    "com.squareup:kotlinpoet:1.5.0",
    "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.5",
    "org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.3.5",
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
    rules_kotlin_version = "b40d920c5a5e044c541513f0d5e9260d0a4579c0"
    http_archive(
        name = "io_bazel_rules_kotlin",
        urls = ["https://github.com/bazelbuild/rules_kotlin/archive/%s.zip" % rules_kotlin_version],
        sha256 = "3dadd0ad7272be6b1ed1274f62cadd4a1293c89990bcd7b4af32637a70ada63e",
        type = "zip",
        strip_prefix = "rules_kotlin-%s" % rules_kotlin_version,
    )

def com_google_protobuf():
    http_archive(
        name = "com_google_protobuf",
        sha256 = "b37e96e81842af659605908a421960a5dc809acbc888f6b947bc320f8628e5b1",
        strip_prefix = "protobuf-3.12.0",
        urls = ["https://github.com/protocolbuffers/protobuf/archive/v3.12.0.zip"],
    )

def io_grpc_grpc_java():
    http_archive(
        name = "io_grpc_grpc_java",
        sha256 = "e274597cc4de351b4f79e4c290de8175c51a403dc39f83f1dfc50a1d1c9e9a4f",
        strip_prefix = "grpc-java-1.28.0",
        url = "https://github.com/grpc/grpc-java/archive/v1.28.0.zip",
    )
