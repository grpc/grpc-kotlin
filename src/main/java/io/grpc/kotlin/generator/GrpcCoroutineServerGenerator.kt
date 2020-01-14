package io.grpc.kotlin.generator

import com.google.common.annotations.VisibleForTesting
import com.google.protobuf.Descriptors.MethodDescriptor
import com.google.protobuf.Descriptors.ServiceDescriptor
import com.google.protobuf.kotlin.protoc.ClassSimpleName
import com.google.protobuf.kotlin.protoc.Declarations
import com.google.protobuf.kotlin.protoc.GeneratorConfig
import com.google.protobuf.kotlin.protoc.MemberSimpleName
import com.google.protobuf.kotlin.protoc.builder
import com.google.protobuf.kotlin.protoc.classBuilder
import com.google.protobuf.kotlin.protoc.declarations
import com.google.protobuf.kotlin.protoc.methodName
import com.google.protobuf.kotlin.protoc.of
import com.google.protobuf.kotlin.protoc.serviceName
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import io.grpc.ServerCall
import io.grpc.ServerServiceDefinition
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.kotlin.AbstractCoroutineServerImpl
import io.grpc.kotlin.ServerCalls
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Generator for abstract classes of the form `MyServiceCoroutineImplBase`.
 */
class GrpcCoroutineServerGenerator(config: GeneratorConfig): ServiceCodeGenerator(config) {
  companion object {
    private const val IMPL_BASE_SUFFIX = "CoroutineImplBase"

    private val RECEIVE_CHANNEL: ClassName = ReceiveChannel::class.asClassName()
    private val SEND_CHANNEL: ClassName = SendChannel::class.asClassName()
    private val UNARY_REQUEST_NAME: MemberSimpleName = MemberSimpleName("request")
    private val STREAMING_REQUEST_NAME: MemberSimpleName = MemberSimpleName("requests")
    private val STREAMING_RESPONSE_NAME: MemberSimpleName = MemberSimpleName("responses")

    private val coroutineContextParameter: ParameterSpec =
      ParameterSpec
        .builder("coroutineContext", CoroutineContext::class)
        .defaultValue("%T", EmptyCoroutineContext::class)
        .build()

    private val SERVER_SERVICE_DEFINITION_BUILDER_FACTORY: MemberName =
      ServerServiceDefinition::class.member("builder")

    private val UNARY_SMD: MemberName = ServerCalls::class.member("unaryServerMethodDefinition")
    private val CLIENT_STREAMING_SMD: MemberName =
      ServerCalls::class.member("clientStreamingServerMethodDefinition")
    private val SERVER_STREAMING_SMD: MemberName =
      ServerCalls::class.member("serverStreamingServerMethodDefinition")
    private val BIDI_STREAMING_SMD: MemberName =
      ServerCalls::class.member("bidiStreamingServerMethodDefinition")

    private val UNIMPLEMENTED_STATUS: MemberName =
      Status::class.member("UNIMPLEMENTED")
  }

  override fun generate(service: ServiceDescriptor): Declarations = declarations {
    addType(implClass(service))
  }

  fun implClass(service: ServiceDescriptor): TypeSpec {

    val serviceImplClassName = service.serviceName.toClassSimpleName().withSuffix(IMPL_BASE_SUFFIX)

    val stubs: List<MethodImplStub> = service.methods.map { serviceMethodStub(it, serviceImplClassName) }
    val implBuilder = TypeSpec
      .classBuilder(serviceImplClassName)
      .addModifiers(KModifier.ABSTRACT)
      .addKdoc(
        """
        Skeletal implementation of the %L service based on Kotlin coroutines. 
        """.trimIndent(),
        service.fullName
      )
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addParameter(coroutineContextParameter)
          .build()
      )
      .superclass(AbstractCoroutineServerImpl::class)
      .addSuperclassConstructorParameter("%N", coroutineContextParameter)

    var serverServiceDefinitionBuilder =
      CodeBlock.of("%M(%M())", SERVER_SERVICE_DEFINITION_BUILDER_FACTORY, service.grpcDescriptor)

    for (stub in stubs) {
      implBuilder.addFunction(stub.methodSpec)
      serverServiceDefinitionBuilder = CodeBlock.of(
        """
          %L
            .addMethod(%L)
        """.trimIndent(),
        serverServiceDefinitionBuilder,
        stub.serverMethodDef
      )
    }

    implBuilder.addFunction(
      FunSpec.builder("bindService")
        .addModifiers(KModifier.OVERRIDE, KModifier.FINAL)
        .returns(ServerServiceDefinition::class)
        .addStatement("return %L.build()", serverServiceDefinitionBuilder)
        .build()
    )

    return implBuilder.build()
  }

  @VisibleForTesting
  data class MethodImplStub(
    val methodSpec: FunSpec,
    /**
     * A [CodeBlock] that computes a [ServerMethodDefinition] based on an implementation of
     * the function described in [methodSpec].
     */
    val serverMethodDef: CodeBlock
  )

  @VisibleForTesting
  fun serviceMethodStub(
    method: MethodDescriptor,
    serviceImplClassName: ClassSimpleName
  ): MethodImplStub = with(config) {
    val requestType = method.inputType.messageClass()
    val requestParam = if (method.isClientStreaming) {
      ParameterSpec.of(STREAMING_REQUEST_NAME, RECEIVE_CHANNEL.parameterizedBy(requestType))
    } else {
      ParameterSpec.of(UNARY_REQUEST_NAME, requestType)
    }

    val methodSpecBuilder = FunSpec.builder(method.methodName.toMemberSimpleName())
      .addModifiers(KModifier.SUSPEND, KModifier.OPEN)
      .addParameter(requestParam)
      .addStatement(
        "throw %T(%M.withDescription(%S))",
        StatusException::class,
        UNIMPLEMENTED_STATUS,
        "Method ${method.fullName} is unimplemented"
      )

    val responseType = method.outputType.messageClass()
    if (method.isServerStreaming) {
      val responsesParam =
        ParameterSpec.of(STREAMING_RESPONSE_NAME, SEND_CHANNEL.parameterizedBy(responseType))
      methodSpecBuilder.addParameter(responsesParam)
      methodSpecBuilder.returns(Unit::class)
    } else {
      methodSpecBuilder.returns(responseType)
    }

    methodSpecBuilder.addKdoc(stubKDoc(method, requestParam, serviceImplClassName))

    val methodSpec = methodSpecBuilder.build()

    val smdFactory = if (method.isServerStreaming) {
      if (method.isClientStreaming) BIDI_STREAMING_SMD else SERVER_STREAMING_SMD
    } else {
      if (method.isClientStreaming) CLIENT_STREAMING_SMD else UNARY_SMD
    }

    val serverMethodDef =
      CodeBlock.of(
        """
          %M(
            scope = this,
            descriptor = %L,
            implementation = ::%N
          )
        """.trimIndent(),
        smdFactory,
        method.descriptorCode,
        methodSpec
      )

    MethodImplStub(methodSpec, serverMethodDef)
  }

  private fun stubKDoc(
    method: MethodDescriptor,
    requestParam: ParameterSpec,
    serviceImplClassName: ClassSimpleName
  ): CodeBlock {
    val kDocBindings = mapOf(
      "requestParam" to requestParam,
      "methodName" to method.fullName,
      "receiveChannel" to ReceiveChannel::class,
      "sendChannel" to SendChannel::class,
      "status" to Status::class,
      "statusFromException" to Status::class.member("fromThrowable"),
      "responsesParam" to STREAMING_RESPONSE_NAME,
      "coroutineContext" to CoroutineContext::class,
      "serviceImplClassName" to serviceImplClassName,
      "closedSendChannelEx" to ClosedSendChannelException::class,
      "coroutineScopeBuilder" to MemberName("kotlinx.coroutines", "coroutineScope"),
      "join" to Job::class.member("join"),
      "serverCall" to ServerCall::class,
      "sendChannelSend" to SendChannel::class.member("send")
    )
    val kDocSections = mutableListOf(
      "Implements %methodName:L as a coroutine.",
      """
        When gRPC receives a %methodName:L RPC, gRPC will invoke this method within the
        [%coroutineContext:T] used to create this `%serviceImplClassName:L`.
      """.trimIndent()
    )

    if (method.isServerStreaming) {
      kDocSections.add(
        """
          If this method completes without throwing an exception, gRPC will close the 
          `%responsesParam:L` channel (if it is not already closed), finish sending any messages 
          remaining in it, and close the RPC with [%status:T.OK].  Note that if other coroutines
          are still writing to `%responsesParam:L`, they may get a [%closedSendChannelEx:T].  Make
          sure all responses are sent before this method completes, e.g. by wrapping your
          implementation in [%coroutineScopeBuilder:M] or explicitly [joining][%join:M] the jobs
          that are sending responses.
        """.trimIndent()
      )
      kDocSections.add(
        """
          If this method throws an exception, the server will abort sending responses and close the
          RPC with a [%status:T] [inferred][%statusFromException:M] from the thrown exception. Other
          RPCs will not be affected.
        """.trimIndent()
      )
    } else {
      kDocSections.add(
        """
          If this method returns a response successfully, gRPC will send the response to the client
           and close the RPC with [%status:T.OK].
        """.trimIndent()
      )
      kDocSections.add(
        """
          If this method throws an exception, the server will not send any responses and close the
          RPC with a [%status:T] [inferred][%statusFromException:M] from the thrown exception.  Other
          RPCs will not be affected.
        """.trimIndent()
      )
    }

    if (method.isClientStreaming) {
      kDocSections.add(
        """
          @param %requestParam:N A [%receiveChannel:T] where client requests can be read.
                [Cancelling][%receiveChannel:T.cancel] this channel can be used to indicate that
                further client requests should be discarded.
        """.trimIndent())
    } else {
      kDocSections.add(
        """
          @param %requestParam:N The request sent by the client.
        """.trimIndent()
      )
    }

    if (method.isServerStreaming) {
      kDocSections.add(
        """
          @param %responsesParam:L A [%sendChannel:T] to send responses to.  Explicitly closing this
          channel when the RPC is done is optional.  If this channel is closed with a nonnull cause,
          the RPC will be closed with a [%status:T] [inferred][%status:T.fromException] from that
          cause.  [Sending][%sendChannelSend:M] to this channel may suspend if additional responses
          cannot be sent without excess buffering; see [%serverCall:T.Listener.onReady] for details.
        """.trimIndent()
      )
    } else {
      kDocSections.add(
        """
          @return The response to send to the client.
        """.trimIndent()
      )
    }

    return CodeBlock
      .builder()
      .addNamed(kDocSections.joinToString("\n\n"), kDocBindings)
      .build()
  }
}