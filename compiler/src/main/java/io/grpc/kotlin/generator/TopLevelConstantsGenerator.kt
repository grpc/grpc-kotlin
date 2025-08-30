package io.grpc.kotlin.generator

import com.google.protobuf.Descriptors
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asTypeName
import io.grpc.MethodDescriptor
import io.grpc.ServiceDescriptor
import io.grpc.kotlin.generator.protoc.Declarations
import io.grpc.kotlin.generator.protoc.GeneratorConfig
import io.grpc.kotlin.generator.protoc.builder
import io.grpc.kotlin.generator.protoc.declarations
import io.grpc.kotlin.generator.protoc.methodName

/** Generates top-level properties for the service descriptor and method descriptors. */
class TopLevelConstantsGenerator(config: GeneratorConfig) : ServiceCodeGenerator(config) {
  override fun generate(service: Descriptors.ServiceDescriptor): Declarations = declarations {
    addProperty(
      PropertySpec.builder("serviceDescriptor", ServiceDescriptor::class)
        .addAnnotation(JvmStatic::class)
        .getter(
          FunSpec.getterBuilder()
            .addStatement("return %T.getServiceDescriptor()", service.grpcClass)
            .build()
        )
        .build()
    )

    with(config) {
      for (method in service.methods) {
        addProperty(
          PropertySpec.builder(
              method.methodName.toMemberSimpleName().withSuffix("Method"),
              MethodDescriptor::class.asTypeName()
                .parameterizedBy(method.inputType.messageClass(), method.outputType.messageClass())
            )
            .getter(
              FunSpec.getterBuilder()
                .addAnnotation(JvmStatic::class)
                .addStatement(
                  "return %T.%L()",
                  service.grpcClass,
                  method.methodName.toMemberSimpleName().withPrefix("get").withSuffix("Method")
                )
                .build()
            )
            .build()
        )
      }
    }
  }
}
