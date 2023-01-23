load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
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
    ctx.actions.run(
        outputs = [source_jar],
        inputs = [input_dir],
        tools = [ctx.executable._zipper],
        arguments = [args],
        executable = "{zipper}".format(zipper = ctx.executable._zipper.path),
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
            cfg = "exec",
            default = Label("@bazel_tools//tools/zip:zipper"),
            allow_files = True,
        ),
        _generator = attr.label(
            default = Label("//compiler/src/main/java/io/grpc/kotlin/generator:GeneratorRunner"),
            cfg = "exec",
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

def _get_real_short_path(file):
    """Returns the correct short path file name to be used by protoc."""
    short_path = file.short_path
    if short_path.startswith("../"):
        second_slash = short_path.index("/", 3)
        short_path = short_path[second_slash + 1:]

    virtual_imports = "_virtual_imports/"
    if virtual_imports in short_path:
        short_path = short_path.split(virtual_imports)[1].split("/", 1)[1]
    return short_path

def _kt_jvm_proto_library_helper_impl(ctx):
    transitive_set = depset(
        transitive =
            [dep[ProtoInfo].transitive_descriptor_sets for dep in ctx.attr.proto_deps],
    )
    proto_sources = []
    for dep in ctx.attr.proto_deps:
        for file in dep[ProtoInfo].direct_sources:
            proto_sources.append(_get_real_short_path(file))

    gen_src_dir = ctx.actions.declare_directory(ctx.label.name + "/ktproto")

    protoc_args = ctx.actions.args()
    protoc_args.set_param_file_format("multiline")
    protoc_args.use_param_file("@%s")
    protoc_args.add("--kotlin_out=" + gen_src_dir.path)
    protoc_args.add_joined(
        transitive_set,
        join_with = ctx.configuration.host_path_separator,
        format_joined = "--descriptor_set_in=%s",
    )
    protoc_args.add_all(proto_sources)

    ctx.actions.run(
        inputs = depset(transitive = [transitive_set]),
        outputs = [gen_src_dir],
        executable = ctx.executable._protoc,
        arguments = [protoc_args],
        mnemonic = "KtProtoGenerator",
        progress_message = "Generating kotlin proto extensions for " +
                           ", ".join([
                               str(dep.label)
                               for dep in ctx.attr.proto_deps
                           ]),
    )

    # Because protoc outputs an unknown number of files we need to zip them into a srcjar.
    args = ctx.actions.args()
    args.add("c")
    args.add(ctx.outputs.srcjar)
    args.add_all([gen_src_dir])
    ctx.actions.run(
        arguments = [args],
        executable = ctx.executable._zip,
        inputs = [gen_src_dir],
        mnemonic = "KtProtoSrcJar",
        outputs = [ctx.outputs.srcjar],
    )

_kt_jvm_proto_library_helper = rule(
    attrs = dict(
        proto_deps = attr.label_list(
            providers = [ProtoInfo],
        ),
        deps = attr.label_list(
            providers = [JavaInfo],
        ),
        exports = attr.label_list(
            allow_rules = ["java_proto_library"],
        ),
        _protoc = attr.label(
            default = Label("@com_google_protobuf//:protoc"),
            cfg = "exec",
            executable = True,
        ),
        _zip = attr.label(
            default = Label("@bazel_tools//tools/zip:zipper"),
            cfg = "exec",
            executable = True,
        ),
    ),
    implementation = _kt_jvm_proto_library_helper_impl,
    outputs = dict(
        srcjar = "%{name}.srcjar",
    ),
)

def kt_jvm_proto_library(
        name,
        deps = None,
        tags = None,
        testonly = None,
        compatible_with = None,
        restricted_to = None,
        visibility = None,
        flavor = None,
        deprecation = None,
        features = []):
    """
    This rule accepts any number of proto_library targets in "deps", translates them to Kotlin and
    returns the compiled Kotlin.

    See also https://developers.google.com/protocol-buffers/docs/kotlintutorial for how to interact
    with the generated Kotlin representation.

    Note that the rule will also export the java version of the same protos as Kotlin protos depend
    on the java version under the hood.

    For standard attributes, see:
      https://docs.bazel.build/versions/master/be/common-definitions.html#common-attributes

    Args:
      name: A name for the target
      deps: One or more proto_library targets to turn into Kotlin.
      tags: Standard attribute
      testonly: Standard attribute
      compatible_with: Standard attribute
      restricted_to: Standard attribute
      visibility: Standard attribute
      flavor: "normal" (default) for normal proto runtime, or "lite" for the lite runtime
        (for Android usage)
      deprecation: Standard attribute
      features: Standard attribute
    """
    java_proto_target = ":%s_DO_NOT_DEPEND_java_proto" % name
    helper_target = ":%s_DO_NOT_DEPEND_kt_proto" % name

    if flavor == "lite":
        native.java_lite_proto_library(
            name = java_proto_target[1:],
            deps = deps,
            testonly = testonly,
            compatible_with = compatible_with,
            visibility = ["//visibility:private"],
            restricted_to = restricted_to,
            tags = tags,
            deprecation = deprecation,
            features = features,
        )
    else:
        native.java_proto_library(
            name = java_proto_target[1:],
            deps = deps,
            testonly = testonly,
            compatible_with = compatible_with,
            visibility = ["//visibility:private"],
            restricted_to = restricted_to,
            tags = tags,
            deprecation = deprecation,
            features = features,
        )

    _kt_jvm_proto_library_helper(
        name = helper_target[1:],
        proto_deps = deps,
        deps = [
            java_proto_target,
        ],
        testonly = testonly,
        compatible_with = compatible_with,
        visibility = ["//visibility:private"],
        restricted_to = restricted_to,
        tags = tags,
        deprecation = deprecation,
        features = features,
    )

    kt_jvm_library(
        name = name,
        srcs = [helper_target + ".srcjar"],
        deps = [
            "@maven//:com_google_protobuf_protobuf_kotlin",
            java_proto_target,
        ],
        exports = [
            java_proto_target,
        ],
        testonly = testonly,
        compatible_with = compatible_with,
        visibility = visibility,
        restricted_to = restricted_to,
        tags = tags,
        deprecation = deprecation,
        features = features,
    )
