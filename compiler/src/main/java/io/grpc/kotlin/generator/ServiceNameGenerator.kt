package io.grpc.kotlin.generator

import com.google.protobuf.Descriptors
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import io.grpc.kotlin.generator.protoc.Declarations
import io.grpc.kotlin.generator.protoc.GeneratorConfig
import io.grpc.kotlin.generator.protoc.declarations

class ServiceNameGenerator(config: GeneratorConfig) : ServiceCodeGenerator(config) {
  override fun generate(service: Descriptors.ServiceDescriptor): Declarations {
    return declarations {
      addProperty(
        PropertySpec.builder("SERVICE_NAME", String::class, KModifier.CONST)
          .initializer("%T.SERVICE_NAME", service.grpcClass)
          .build()
      )
    }
  }
}