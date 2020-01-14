package io.grpc.kotlin.generator

import com.google.common.annotations.VisibleForTesting
import com.google.protobuf.Descriptors.MethodDescriptor
import com.google.protobuf.Descriptors.ServiceDescriptor
import com.google.protobuf.kotlin.protoc.Declarations
import com.google.protobuf.kotlin.protoc.GeneratorConfig
import com.google.protobuf.kotlin.protoc.MemberSimpleName
import com.google.protobuf.kotlin.protoc.UnqualifiedScope
import com.google.protobuf.kotlin.protoc.builder
import com.google.protobuf.kotlin.protoc.classBuilder
import com.google.protobuf.kotlin.protoc.declarations
import com.google.protobuf.kotlin.protoc.member
import com.google.protobuf.kotlin.protoc.methodName
import com.google.protobuf.kotlin.protoc.of
import com.google.protobuf.kotlin.protoc.serviceName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asTypeName
import io.grpc.CallOptions
import io.grpc.Channel as GrpcChannel
import io.grpc.Metadata as GrpcMetadata
import io.grpc.MethodDescriptor.MethodType
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.kotlin.AbstractCoroutineStub
import io.grpc.kotlin.ClientCalls
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.Executor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

/**
 * Logic for generating gRPC stubs for Kotlin.
 */
@VisibleForTesting
class GrpcClientStubGenerator(config: GeneratorConfig) : ServiceCodeGenerator(config) {
  companion object {
    private const val STUB_CLASS_SUFFIX = "CoroutineStub"
    private val UNARY_PARAMETER_NAME = MemberSimpleName("request")
    private val STREAMING_PARAMETER_NAME = MemberSimpleName("requests")
    private val GRPC_CHANNEL_PARAMETER_NAME = MemberSimpleName("channel")
    private val COROUTINE_CONTEXT_PARAMETER_NAME = MemberSimpleName("coroutineContext")
    private val CALL_OPTIONS_PARAMETER_NAME = MemberSimpleName("callOptions")

    private val COROUTINE_CONTEXT_PROPERTY: PropertySpec =
      PropertySpec.of(COROUTINE_CONTEXT_PARAMETER_NAME, CoroutineContext::class)
    private val COROUTINE_CONTEXT_PARAMETER: ParameterSpec = ParameterSpec
      .builder(COROUTINE_CONTEXT_PARAMETER_NAME, CoroutineContext::class)
      .defaultValue("%T", EmptyCoroutineContext::class)
      .build()

    private val HEADERS_PARAMETER: ParameterSpec = ParameterSpec
      .builder("headers", GrpcMetadata::class)
      .defaultValue("%T()", GrpcMetadata::class)
      .build()

    val GRPC_CHANNEL_PARAMETER = ParameterSpec.of(GRPC_CHANNEL_PARAMETER_NAME, GrpcChannel::class)
    val CALL_OPTIONS_PARAMETER = ParameterSpec
      .builder(MemberSimpleName("callOptions"), CallOptions::class)
      .defaultValue("%M", CallOptions::class.member("DEFAULT"))
      .build()

    private val WITH_CONTEXT_MEMBER: MemberName = MemberName("kotlinx.coroutines", "withContext")

    private val RECEIVE_CHANNEL = ReceiveChannel::class.asTypeName()
    private val UNARY_RPC_HELPER = ClientCalls::class.member("unaryRpc")
    private val CLIENT_STREAMING_RPC_HELPER = ClientCalls::class.member("clientStreamingRpc")
    private val SERVER_STREAMING_RPC_HELPER = ClientCalls::class.member("serverStreamingRpc")
    private val BIDI_STREAMING_RPC_HELPER = ClientCalls::class.member("bidiStreamingRpc")

    private val CLOSE_CHANNEL_MEMBER = SendChannel::class.member("close")

    private val MethodDescriptor.type: MethodType
      get() = if (isClientStreaming) {
        if (isServerStreaming) MethodType.BIDI_STREAMING else MethodType.CLIENT_STREAMING
      } else {
        if (isServerStreaming) MethodType.SERVER_STREAMING else MethodType.UNARY
      }
  }

  override fun generate(service: ServiceDescriptor): Declarations = declarations {
    addType(generateStub(service))
  }

  @VisibleForTesting
  fun generateStub(service: ServiceDescriptor): TypeSpec {
    val stubName = service.serviceName.toClassSimpleName().withSuffix(STUB_CLASS_SUFFIX)

    // Not actually a TypeVariableName, but this at least prevents the name from being imported,
    // which we don't want.
    val stubSelfReference: TypeName = TypeVariableName(stubName.toString())

    val builder = TypeSpec
      .classBuilder(stubName)
      .superclass(AbstractCoroutineStub::class.asTypeName().parameterizedBy(stubSelfReference))
      .addKdoc(
        "A stub for issuing RPCs to a(n) %L service as suspending coroutines.",
        service.fullName
      )
      .primaryConstructor(
        FunSpec
          .constructorBuilder()
          .addParameter(GRPC_CHANNEL_PARAMETER)
          .addParameter(COROUTINE_CONTEXT_PARAMETER)
          .addParameter(CALL_OPTIONS_PARAMETER)
          .addAnnotation(JvmOverloads::class)
          .build()
      )
      .addSuperclassConstructorParameter("%N", GRPC_CHANNEL_PARAMETER)
      .addSuperclassConstructorParameter("%N", COROUTINE_CONTEXT_PARAMETER)
      .addSuperclassConstructorParameter("%N", CALL_OPTIONS_PARAMETER)
      .addFunction(buildFun(stubSelfReference))

    for (method in service.methods) {
      builder.addFunction(generateRpcStub(method))
    }
    return builder.build()
  }

  /**
   * Outputs a `FunSpec` of an override of `AbstractCoroutineStub.build` for this particular stub.
   */
  private fun buildFun(stubName: TypeName): FunSpec {
    return FunSpec
      .builder("build")
      .returns(stubName)
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(GRPC_CHANNEL_PARAMETER)
      .addParameter(ParameterSpec.of(COROUTINE_CONTEXT_PARAMETER_NAME, CoroutineContext::class))
      .addParameter(ParameterSpec.of(CALL_OPTIONS_PARAMETER_NAME, CallOptions::class))
      .addStatement(
        "return %T(%N, %N, %N)",
        stubName,
        GRPC_CHANNEL_PARAMETER,
        COROUTINE_CONTEXT_PARAMETER,
        CALL_OPTIONS_PARAMETER
      )
      .build()
  }

  @VisibleForTesting
  fun generateRpcStub(method: MethodDescriptor): FunSpec = with(config) {
    val name = method.methodName.toMemberSimpleName()
    val requestType = method.inputType.messageClass()
    val parameter = if (method.isClientStreaming) {
      ParameterSpec.of(STREAMING_PARAMETER_NAME, RECEIVE_CHANNEL.parameterizedBy(requestType))
    } else {
      ParameterSpec.of(UNARY_PARAMETER_NAME, requestType)
    }

    val responseType = method.outputType.messageClass()

    val returnType =
      if (method.isServerStreaming) RECEIVE_CHANNEL.parameterizedBy(responseType) else responseType

    val helperMethod = when (method.type) {
      MethodType.UNARY -> UNARY_RPC_HELPER
      MethodType.SERVER_STREAMING -> SERVER_STREAMING_RPC_HELPER
      MethodType.CLIENT_STREAMING -> CLIENT_STREAMING_RPC_HELPER
      MethodType.BIDI_STREAMING -> BIDI_STREAMING_RPC_HELPER
      else -> throw IllegalArgumentException()
    }

    val funSpecBuilder =
      funSpecBuilder(name)
        .addParameter(parameter)
        .addParameter(HEADERS_PARAMETER)
        .returns(returnType)
        .addKdoc(rpcStubKDoc(method, parameter, returnType))

    val codeBlockMap = mapOf(
      "withContext" to WITH_CONTEXT_MEMBER,
      "coroutineContextProperty" to COROUTINE_CONTEXT_PROPERTY,
      "helperMethod" to helperMethod,
      "methodDescriptor" to method.descriptorCode,
      "parameter" to parameter,
      "headers" to HEADERS_PARAMETER,
      "coroutineScope" to CoroutineScope::class
    )

    if (!method.isServerStreaming) {
      funSpecBuilder
        .addModifiers(KModifier.SUSPEND)
        .addNamedCode(
          """
          return %withContext:M(%coroutineContextProperty:N) {
            %helperMethod:M(
              this,
              channel,
              %methodDescriptor:L,
              %parameter:N,
              callOptions,
              %headers:N
            )
          }
          """.trimIndent(),
          codeBlockMap
        )
    } else {
      funSpecBuilder
        .addNamedCode(
          """
          return %helperMethod:M(
            %coroutineScope:T(%coroutineContextProperty:N),
            channel,
            %methodDescriptor:L,
            %parameter:N,
            callOptions,
            %headers:N
          )
          """.trimIndent(),
          codeBlockMap
        )
    }
    return funSpecBuilder.build()
  }

  private fun rpcStubKDoc(
    method: MethodDescriptor,
    parameter: ParameterSpec,
    returnType: TypeName
  ): CodeBlock {
    val qualifier = when (method.type) {
      MethodType.BIDI_STREAMING -> "bidirectional streaming"
      MethodType.CLIENT_STREAMING -> "client streaming"
      MethodType.SERVER_STREAMING -> "server streaming"
      MethodType.UNARY -> "unary"
      else -> throw AssertionError("impossible")
    }

    val kDocBindings = mapOf(
      "methodName" to method.fullName,
      "grpcChannel" to GrpcChannel::class,
      "parameter" to parameter,
      "returnType" to returnType,
      "receiveChannel" to ReceiveChannel::class,
      "coroutineContext" to COROUTINE_CONTEXT_PROPERTY,
      "withContext" to WITH_CONTEXT_MEMBER,
      "status" to Status::class,
      "statusException" to StatusException::class,
      "qualifier" to qualifier,
      "closeChannel" to CLOSE_CHANNEL_MEMBER,
      "executor" to Executor::class
    )

    val kDocComponents = mutableListOf<String>()

    kDocComponents.add("Issues a %qualifier:L %methodName:L RPC on this stub's [%grpcChannel:T].")

    kDocComponents.add(
      """
      The implementation may launch further coroutines, which are run as if by
      [`withContext(%coroutineContext:N)`][%withContext:M].  (Some work may also be done in the
      [%executor:T] associated with the `%grpcChannel:T`.)
      """.trimIndent()
    )

    if (method.isClientStreaming) {
      kDocComponents.add(
        """
        @param %parameter:N A [%receiveChannel:T] of requests to be sent to the server; 
        expected to be provided, populated, and closed by the client.  When %parameter:N is closed,
        the RPC stream will be closed; if it is closed with a nonnull cause, the RPC is cancelled
        and the cause is sent to the server as the reason for cancellation.
        """.trimIndent()
      )
    } else {
      kDocComponents.add(
        "@param %parameter:N The single argument to the RPC, sent to the server."
      )
    }

    if (method.isServerStreaming) {
      kDocComponents.add(
        """
        @return A [`%returnType:T`][%receiveChannel:T] for responses from the server.  If
        cancelled, the RPC is shut down and a cancellation with that cause is sent to the server.  
        Alternately, if the RPC fails (closes with a [%status:T] other than `%status:T.OK`), the
        returned channel is [closed][%closeChannel:M] with a corresponding [%statusException:T].
        """.trimIndent()
      )
    } else {
      kDocComponents.add(
        """
        @return The single response from the server.  This coroutine suspends until the server
        returns it and closes with [%status:T.OK], at which point this coroutine resumes with the
        result. If the RPC fails (closes with another [%status:T]), this will fail with a
        corresponding [%statusException:T].
        """.trimIndent()
      )
    }

    return CodeBlock
      .builder()
      .addNamed(kDocComponents.joinToString("\n\n"), kDocBindings)
      .build()
  }
}
