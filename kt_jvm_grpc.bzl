load("@io_grpc_grpc_java//:java_grpc_library.bzl", "java_grpc_library")
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")
load("@io_bazel_rules_kotlin//kotlin/internal/jvm:impl.bzl", "kt_jvm_library_impl")

def _java_grpc_name(name):
    return ":%s_DO_NOT_DEPEND_java_grpc" % name

def _kt_grpc_name(name):
    return ":%s_DO_NOT_DEPEND_kt_grpc" % name

def _collect_providers(provider, deps):
    """Collects the requested provider from the given list of deps."""
    return [dep[provider] for dep in deps if provider in dep]

def _kt_grpc_extensions_impl(ctx):
    direct_descriptor_set = depset([dep[ProtoInfo].direct_descriptor_set for dep in ctx.attr.proto_deps])
    transitive_descriptor_set = depset(
        transitive = [dep[ProtoInfo].transitive_descriptor_sets for dep in ctx.attr.proto_deps],
    )

    gen_src_dir = ctx.actions.declare_directory(ctx.label.name + "/ktgrpc")

    gensrc_args = ctx.actions.args()
    gensrc_args.add(gen_src_dir.path)  # have to explicitly write .path for a directory
    gensrc_args.add_all(direct_descriptor_set)
    gensrc_args.add("--")
    gensrc_args.add_all(transitive_descriptor_set)

    ctx.actions.run(
        outputs = [gen_src_dir],
        inputs = depset(transitive = [direct_descriptor_set, transitive_descriptor_set]),
        arguments = [gensrc_args],
        progress_message = "Generating Kotlin gRPC extensions for " +
                           ", ".join([str(dep.label) for dep in ctx.attr.proto_deps]),
        executable = ctx.executable._generator,
        mnemonic = "KtGrpcExtensions",
    )

    output_jar = ctx.actions.declare_file("some_jar.jar")
    source_jar = ctx.actions.declare_file("some_jar.srcjar")

    java_info = java_common.compile(
        ctx,
        source_files = [gen_src_dir],
        output = output_jar,
        java_toolchain = ctx.attr._java_toolchain[java_common.JavaToolchainInfo],
        host_javabase = ctx.attr._host_javabase[java_common.JavaRuntimeInfo],
    )

    zipper_args = ctx.actions.args()
    zipper_args.add_all(depset([gen_src_dir]))
    ctx.actions.run_shell(
        outputs = [source_jar],
        inputs = [gen_src_dir],
        tools = [ctx.executable._zipper],
        arguments = [zipper_args],
        command = "{zipper} c {output_jar} $1".format(
            output_jar = source_jar.path,
            zipper = ctx.executable._zipper.path,
        ),
    )

    return [java_info, DefaultInfo(files = depset([source_jar, gen_src_dir]))]

_kt_grpc_library_helper = rule(
    attrs = dict(
        proto_deps = attr.label_list(
            providers = [ProtoInfo],
        ),
        deps = attr.label_list(
            providers = [JavaInfo],
        ),
        exports = attr.label_list(
            allow_rules = ["_java_grpc_library", "_java_lite_grpc_library"],
        ),
        _zipper = attr.label(
            executable = True,
            cfg = "host",
            default = Label("@bazel_tools//tools/zip:zipper"),
            allow_files = True,
        ),
        _java_toolchain = attr.label(
            default = Label("@bazel_tools//tools/jdk:current_java_toolchain"),
        ),
        _host_javabase = attr.label(
            cfg = "host",
            default = Label("@bazel_tools//tools/jdk:current_host_java_runtime"),
        ),
        _generator = attr.label(
            default = Label("//compiler/src/main/java/io/grpc/kotlin/generator:GeneratorRunner"),
            cfg = "host",
            executable = True,
        ),
    ),
    fragments = ["java"],
    provides = [JavaInfo],
    implementation = _kt_grpc_extensions_impl,
)

def kt_jvm_grpc_library(
        name,
        srcs = None,
        deps = None,
        tags = None,
        testonly = None,  # None to not override Blaze's default for //javatests, b/112708042
        compatible_with = None,
        restricted_to = None,
        visibility = None,
        flavor = None,
        deprecation = None,
        features = []):
    """This rule compiles Kotlin APIs for gRPC services from the proto_library targets in deps.

    In particular, it builds the java_grpc_library for the target and then Kotlin extensions
    atop that.  This rule can be depended on from Java and Kotlin targets, without conflicting with
    a java_grpc_library for the same target.

    Args:
      name: a name for the target
      srcs: exactly one proto_library targets, to generate Kotlin APIs for
      deps: exactly one JVM proto_library target for srcs: should be either java_proto_library,
            java_lite_proto_library, kt_jvm_proto_library, or kt_jvm_lite_proto_library
            targets
      tags: A list of string tags passed to generated targets.
      testonly: Whether this target is intended only for tests.
      compatible_with: Standard attribute, see http://go/be-common#common.compatible_with
      restricted_to: Standard attribute, see http://go/be-common#common.restricted_to
      visibility: A list of targets allowed to depend on this rule.
      flavor: "normal" (default) for normal proto runtime, or "lite" for the lite runtime
        (for Android usage)
      deprecation: Standard attribute, see http://go/be-common#common.deprecation
      features: Features enabled.
    """

    srcs = srcs or []
    deps = deps or []

    if len(srcs) != 1:
        fail("Expected exactly one src", "srcs")
    if len(deps) != 1:
        fail("Expected exactly one dep", "deps")

    if flavor == None or flavor == "normal":
        kt_grpc_deps = [
            "@io_grpc_grpc_java//stub",
            "@io_grpc_grpc_java//context",
            "//stub/src/main/java/io/grpc/kotlin:stub",
        ]

    elif flavor == "lite":
        fail("Android support is unimplemented")
    else:
        fail("Flavor must be normal or lite")

    java_grpc_library(
        name = _java_grpc_name(name)[1:],
        srcs = srcs,
        deps = deps,
        compatible_with = compatible_with,
        visibility = ["//visibility:private"],
        flavor = flavor,
        restricted_to = restricted_to,
        testonly = testonly,
        deprecation = deprecation,
        features = features,
    )

    gen_rule_label = _kt_grpc_name(name)
    gen_rule_name = gen_rule_label[1:]
    grpc_deps = [_java_grpc_name(name)] + kt_grpc_deps

    _kt_grpc_library_helper(
        name = gen_rule_name,
        proto_deps = srcs,
        deps = grpc_deps,
        exports = [_java_grpc_name(name)],
        compatible_with = compatible_with,
        restricted_to = restricted_to,
        testonly = testonly,
        visibility = visibility,
        deprecation = deprecation,
        features = features,
    )

    kt_jvm_library(
        name = name,
        srcs = [gen_rule_label],
        deps = grpc_deps,
    )
