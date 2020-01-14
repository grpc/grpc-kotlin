package io.grpc.kotlin.generator

import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.kotlin.protoc.AbstractGeneratorRunner
import com.google.protobuf.kotlin.protoc.GeneratorConfig
import com.google.protobuf.kotlin.protoc.JavaPackagePolicy
import com.squareup.kotlinpoet.FileSpec

/** Main runner for code generation for Kotlin gRPC APIs. */
object GeneratorRunner: AbstractGeneratorRunner() {
  @JvmStatic
  fun main(args: Array<String>) = super.doMain(args)

  private val config = GeneratorConfig(JavaPackagePolicy.GOOGLE_INTERNAL, false)

  val generator = ProtoFileCodeGenerator(
    generators = listOf(::GrpcClientStubGenerator, ::GrpcCoroutineServerGenerator),
    config = config,
    topLevelSuffix = "GrpcKt"
  )

  override fun generateCodeForFile(file: FileDescriptor): List<FileSpec> =
    listOfNotNull(generator.generateCodeForFile(file))
}
