workspace(name = "com_github_grpc_grpc_kotlin")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

http_archive(
    name = "rules_jvm_external",
    sha256 = "62133c125bf4109dfd9d2af64830208356ce4ef8b165a6ef15bbff7460b35c3a",
    strip_prefix = "rules_jvm_external-3.0",
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/3.0.zip",
)

load("@rules_jvm_external//:defs.bzl", "maven_install")
load(
    "//:repositories.bzl",
    "IO_GRPC_GRPC_KOTLIN_ARTIFACTS",
    "IO_GRPC_GRPC_KOTLIN_OVERRIDE_TARGETS",
    "grpc_kt_repositories",
)

http_archive(
    name = "io_grpc_grpc_java",
    sha256 = "e274597cc4de351b4f79e4c290de8175c51a403dc39f83f1dfc50a1d1c9e9a4f",
    strip_prefix = "grpc-java-1.28.0",
    url = "https://github.com/grpc/grpc-java/archive/v1.28.0.zip",
)

load(
    "@io_grpc_grpc_java//:repositories.bzl",
    "IO_GRPC_GRPC_JAVA_ARTIFACTS",
    "IO_GRPC_GRPC_JAVA_OVERRIDE_TARGETS",
    "grpc_java_repositories",
)

maven_install(
    artifacts = IO_GRPC_GRPC_KOTLIN_ARTIFACTS + IO_GRPC_GRPC_JAVA_ARTIFACTS,
    generate_compat_repositories = True,
    override_targets = dict(
        IO_GRPC_GRPC_KOTLIN_OVERRIDE_TARGETS.items() +
        IO_GRPC_GRPC_JAVA_OVERRIDE_TARGETS.items(),
    ),
    repositories = [
        "https://repo.maven.apache.org/maven2/",
    ],
)

load("@maven//:compat.bzl", "compat_repositories")

compat_repositories()

grpc_kt_repositories()

grpc_java_repositories()

load("@com_google_protobuf//:protobuf_deps.bzl", "protobuf_deps")

protobuf_deps()

load(
    "@io_bazel_rules_kotlin//kotlin:kotlin.bzl",
    "kotlin_repositories",
    "kt_register_toolchains",
)

kotlin_repositories()

kt_register_toolchains()
