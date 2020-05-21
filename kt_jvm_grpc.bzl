load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")
load("@io_grpc_grpc_java//:java_grpc_library.bzl", "java_grpc_library")

def _invoke_generator(ctx, proto_dep, output_dir):
    direct_descriptor_set = depset([proto_dep[ProtoInfo].direct_descriptor_set])
    transitive_descriptor_set = depset(transitive = [proto_dep[ProtoInfo].transitive_descriptor_sets])

    args = ctx.actions.args()
    args.add(output_dir.path)  # have to explicitly write .path for a directory
    args.add_all(direct_descriptor_set)
    args.add("--")
    args.add_all(transitive_descriptor_set)

    ctx.actions.run(
        outputs = [output_dir],
        inputs = depset(transitive = [direct_descriptor_set, transitive_descriptor_set]),
        arguments = [args],
        executable = ctx.executable._generator,
        mnemonic = "KtGrpcGenerator",
        progress_message = "Generating Kotlin gRPC extensions for %s" % proto_dep.label,
    )

def _build_srcjar(ctx, proto_dep, input_dir, source_jar):
    args = ctx.actions.args()
    args.add("c")
    args.add(source_jar.path)
    args.add_all(depset([input_dir]))
    ctx.actions.run_shell(
        outputs = [source_jar],
        inputs = [input_dir],
        tools = [ctx.executable._zipper],
        arguments = [args],
        command = "{zipper} $1 $2 $3".format(zipper = ctx.executable._zipper.path),
        mnemonic = "KtGrpcSrcJar",
        progress_message = "Generating Kotlin gRPC srcjar for %s" % proto_dep.label,
    )

def _kt_grpc_generate_code_impl(ctx):
    proto_dep = ctx.attr.srcs[0]
    name = ctx.label.name

    gen_src_dir_name = "%s/ktgrpc" % name
    gen_src_dir = ctx.actions.declare_directory(gen_src_dir_name)
    source_jar = ctx.actions.declare_file("%s.srcjar" % name)

    _invoke_generator(ctx, proto_dep, gen_src_dir)
    _build_srcjar(ctx, proto_dep, gen_src_dir, source_jar)

    java_info = ctx.attr.deps[0][JavaInfo]
    default_info = DefaultInfo(files = depset([source_jar, gen_src_dir]))

    return [java_info, default_info]

_kt_grpc_generate_code = rule(
    attrs = dict(
        srcs = attr.label_list(
            providers = [ProtoInfo],
        ),
        deps = attr.label_list(
            providers = [JavaInfo],
        ),
        _zipper = attr.label(
            executable = True,
            cfg = "host",
            default = Label("@bazel_tools//tools/zip:zipper"),
            allow_files = True,
        ),
        _generator = attr.label(
            default = Label("//compiler/src/main/java/io/grpc/kotlin/generator:GeneratorRunner"),
            cfg = "host",
            executable = True,
        ),
    ),
    fragments = ["java"],
    provides = [JavaInfo],
    implementation = _kt_grpc_generate_code_impl,
)

def kt_jvm_grpc_library(
        name,
        srcs = None,
        deps = None,
        tags = None,
        testonly = None,  # None to not override Bazel's default
        compatible_with = None,
        restricted_to = None,
        visibility = None,
        flavor = None,
        deprecation = None,
        features = []):
    """This rule compiles Kotlin APIs for gRPC services from the proto_library in srcs.

    For standard attributes, see:
      https://docs.bazel.build/versions/master/be/common-definitions.html#common-attributes

    Args:
      name: A name for the target
      srcs: Exactly one proto_library target to generate Kotlin APIs for
      deps: Exactly one java_proto_library target for srcs[0]
      tags: A list of string tags passed to generated targets.
      testonly: Whether this target is intended only for tests.
      compatible_with: Standard attribute
      restricted_to: Standard attribute
      visibility: A list of targets allowed to depend on this rule.
      flavor: "normal" (default) for normal proto runtime, or "lite" for the lite runtime
        (for Android usage)
      deprecation: Standard attribute
      features: Features enabled.
    """
    srcs = srcs or []
    deps = deps or []
    kt_deps = []

    if len(srcs) != 1:
        fail("Expected exactly one src", "srcs")
    if len(deps) != 1:
        fail("Expected exactly one dep", "deps")

    kt_deps.extend([
        "@com_github_grpc_grpc_kotlin//stub/src/main/java/io/grpc/kotlin:stub",
        "@com_github_grpc_grpc_kotlin//stub/src/main/java/io/grpc/kotlin:context",
    ])

    kt_grpc_label = ":%s_DO_NOT_DEPEND_kt_grpc" % name
    kt_grpc_name = kt_grpc_label[1:]

    java_grpc_label = ":%s_DO_NOT_DEPEND_java_grpc" % name
    java_grpc_name = java_grpc_label[1:]

    java_grpc_library(
        name = java_grpc_name,
        srcs = srcs,
        deps = deps,
        flavor = flavor,
        compatible_with = compatible_with,
        restricted_to = restricted_to,
        testonly = testonly,
        visibility = visibility,
        deprecation = deprecation,
        features = features,
    )
    kt_deps.append(java_grpc_label)

    _kt_grpc_generate_code(
        name = kt_grpc_name,
        srcs = srcs,
        deps = deps,
    )

    kt_jvm_library(
        name = name,
        srcs = [kt_grpc_label],
        deps = kt_deps,
        exports = kt_deps,
        compatible_with = compatible_with,
        restricted_to = restricted_to,
        testonly = testonly,
        visibility = visibility,
        deprecation = deprecation,
        features = features,
    )
