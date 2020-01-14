workspace(name = "com_github_grpc_grpc_kotlin")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
http_archive(
    name = "build_bazel_rules_android",
    urls = ["https://github.com/bazelbuild/rules_android/archive/v0.1.1.zip"],
    sha256 = "cd06d15dd8bb59926e4d65f9003bfc20f9da4b2519985c27e190cddc8b7a7806",
    strip_prefix = "rules_android-0.1.1",
)

rules_kotlin_version = "b40d920c5a5e044c541513f0d5e9260d0a4579c0"
http_archive(
    name = "io_bazel_rules_kotlin",
    urls = ["https://github.com/bazelbuild/rules_kotlin/archive/%s.zip" % rules_kotlin_version],
    sha256 = "3dadd0ad7272be6b1ed1274f62cadd4a1293c89990bcd7b4af32637a70ada63e",
    type = "zip",
    strip_prefix = "rules_kotlin-%s" % rules_kotlin_version
)

load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kotlin_repositories", "kt_register_toolchains")
kotlin_repositories()
kt_register_toolchains()

http_archive(
    name = "io_grpc_grpc_java",
    sha256 = "11f2930cf31c964406e8a7e530272a263fbc39c5f8d21410b2b927b656f4d9be",
    strip_prefix = "grpc-java-1.26.0",
    url = "https://github.com/grpc/grpc-java/archive/v1.26.0.zip",
)

load("@io_grpc_grpc_java//:repositories.bzl", "grpc_java_repositories")

grpc_java_repositories()

load("@com_google_protobuf//:protobuf_deps.bzl", "protobuf_deps")
protobuf_deps()