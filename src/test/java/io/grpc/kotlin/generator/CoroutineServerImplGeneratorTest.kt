package io.grpc.kotlin.generator

import com.google.common.io.Resources
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.Descriptors.MethodDescriptor
import com.google.protobuf.kotlin.protoc.ClassSimpleName
import com.google.protobuf.kotlin.protoc.GeneratorConfig
import com.google.protobuf.kotlin.protoc.JavaPackagePolicy
import com.google.protobuf.kotlin.protoc.testing.assertThat
import io.grpc.examples.helloworld.HelloWorldProto
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4


@RunWith(JUnit4::class)
class CoroutineServerImplGeneratorTest {
  private val serviceDescriptor =
    HelloWorldProto.getDescriptor().findServiceByName("Greeter")
  private val config = GeneratorConfig(JavaPackagePolicy.GOOGLE_INTERNAL, false)
  private val generator = GrpcCoroutineServerGenerator(config)
  private val unarySayHelloDescriptor: MethodDescriptor =
    serviceDescriptor.findMethodByName("SayHello")
  private val clientStreamingSayHelloDescriptor: MethodDescriptor =
    serviceDescriptor.findMethodByName("ClientStreamSayHello")
  private val serverStreamingSayHelloDescriptor: MethodDescriptor =
    serviceDescriptor.findMethodByName("ServerStreamSayHello")
  private val bidiStreamingSayHelloDescriptor: MethodDescriptor =
    serviceDescriptor.findMethodByName("BidiStreamSayHello")
  private val stubSimpleName = ClassSimpleName("ThisImpl")

  @Test
  fun unaryImplStub() {
    val stub = generator.serviceMethodStub(unarySayHelloDescriptor, stubSimpleName)
    assertThat(stub.methodSpec).generates("""
      /**
       * Implements helloworld.Greeter.SayHello as a coroutine.
       *
       * When gRPC receives a helloworld.Greeter.SayHello RPC, gRPC will invoke this method within the
       * [kotlin.coroutines.CoroutineContext] used to create this `ThisImpl`.
       *
       * If this method returns a response successfully, gRPC will send the response to the client
       *  and close the RPC with [io.grpc.Status.OK].
       *
       * If this method throws an exception, the server will not send any responses and close the
       * RPC with a [io.grpc.Status] [inferred][io.grpc.Status.fromThrowable] from the thrown exception.  Other
       * RPCs will not be affected.
       *
       * @param request The request sent by the client.
       *
       * @return The response to send to the client.
       */
      open suspend fun sayHello(request: io.grpc.examples.helloworld.HelloRequest): io.grpc.examples.helloworld.HelloReply {
        throw io.grpc.StatusException(io.grpc.Status.UNIMPLEMENTED.withDescription("Method helloworld.Greeter.SayHello is unimplemented"))
      }
    """.trimIndent())
    assertThat(stub.serverMethodDef.toString()).isEqualTo("""
      io.grpc.kotlin.ServerCalls.unaryServerMethodDefinition(
        scope = this,
        descriptor = io.grpc.examples.helloworld.GreeterGrpc.getSayHelloMethod(),
        implementation = ::sayHello
      )
    """.trimIndent())
  }

  @Test
  fun clientStreamingImplStub() {
    val stub = generator.serviceMethodStub(clientStreamingSayHelloDescriptor, stubSimpleName)
    assertThat(stub.methodSpec).generates("""
      /**
       * Implements helloworld.Greeter.ClientStreamSayHello as a coroutine.
       *
       * When gRPC receives a helloworld.Greeter.ClientStreamSayHello RPC, gRPC will invoke this method within the
       * [kotlin.coroutines.CoroutineContext] used to create this `ThisImpl`.
       *
       * If this method returns a response successfully, gRPC will send the response to the client
       *  and close the RPC with [io.grpc.Status.OK].
       *
       * If this method throws an exception, the server will not send any responses and close the
       * RPC with a [io.grpc.Status] [inferred][io.grpc.Status.fromThrowable] from the thrown exception.  Other
       * RPCs will not be affected.
       *
       * @param requests A [kotlinx.coroutines.channels.ReceiveChannel] where client requests can be read.
       *       [Cancelling][kotlinx.coroutines.channels.ReceiveChannel.cancel] this channel can be used to indicate that
       *       further client requests should be discarded.
       *
       * @return The response to send to the client.
       */
      open suspend fun clientStreamSayHello(requests: kotlinx.coroutines.channels.ReceiveChannel<io.grpc.examples.helloworld.HelloRequest>): io.grpc.examples.helloworld.HelloReply {
        throw io.grpc.StatusException(io.grpc.Status.UNIMPLEMENTED.withDescription("Method helloworld.Greeter.ClientStreamSayHello is unimplemented"))
      }
    """.trimIndent())
    assertThat(stub.serverMethodDef.toString()).isEqualTo("""
      io.grpc.kotlin.ServerCalls.clientStreamingServerMethodDefinition(
        scope = this,
        descriptor = io.grpc.examples.helloworld.GreeterGrpc.getClientStreamSayHelloMethod(),
        implementation = ::clientStreamSayHello
      )
    """.trimIndent())
  }

  @Test
  fun serverStreamingImplStub() {
    val stub = generator.serviceMethodStub(serverStreamingSayHelloDescriptor, stubSimpleName)
    assertThat(stub.methodSpec).generates("""
      /**
       * Implements helloworld.Greeter.ServerStreamSayHello as a coroutine.
       *
       * When gRPC receives a helloworld.Greeter.ServerStreamSayHello RPC, gRPC will invoke this method within the
       * [kotlin.coroutines.CoroutineContext] used to create this `ThisImpl`.
       *
       * If this method completes without throwing an exception, gRPC will close the 
       * `responses` channel (if it is not already closed), finish sending any messages 
       * remaining in it, and close the RPC with [io.grpc.Status.OK].  Note that if other coroutines
       * are still writing to `responses`, they may get a [kotlinx.coroutines.channels.ClosedSendChannelException].  Make
       * sure all responses are sent before this method completes, e.g. by wrapping your
       * implementation in [kotlinx.coroutines.coroutineScope] or explicitly [joining][kotlinx.coroutines.Job.join] the jobs
       * that are sending responses.
       *
       * If this method throws an exception, the server will abort sending responses and close the
       * RPC with a [io.grpc.Status] [inferred][io.grpc.Status.fromThrowable] from the thrown exception. Other
       * RPCs will not be affected.
       *
       * @param request The request sent by the client.
       *
       * @param responses A [kotlinx.coroutines.channels.SendChannel] to send responses to.  Explicitly closing this
       * channel when the RPC is done is optional.  If this channel is closed with a nonnull cause,
       * the RPC will be closed with a [io.grpc.Status] [inferred][io.grpc.Status.fromException] from that
       * cause.  [Sending][kotlinx.coroutines.channels.SendChannel.send] to this channel may suspend if additional responses
       * cannot be sent without excess buffering; see [io.grpc.ServerCall.Listener.onReady] for details.
       */
      open suspend fun serverStreamSayHello(request: io.grpc.examples.helloworld.MultiHelloRequest, responses: kotlinx.coroutines.channels.SendChannel<io.grpc.examples.helloworld.HelloReply>) {
        throw io.grpc.StatusException(io.grpc.Status.UNIMPLEMENTED.withDescription("Method helloworld.Greeter.ServerStreamSayHello is unimplemented"))
      }
    """.trimIndent())
    assertThat(stub.serverMethodDef.toString()).isEqualTo("""
      io.grpc.kotlin.ServerCalls.serverStreamingServerMethodDefinition(
        scope = this,
        descriptor = io.grpc.examples.helloworld.GreeterGrpc.getServerStreamSayHelloMethod(),
        implementation = ::serverStreamSayHello
      )
    """.trimIndent())
  }

  @Test
  fun bidiStreamingImplStub() {
    val stub = generator.serviceMethodStub(bidiStreamingSayHelloDescriptor, stubSimpleName)
    assertThat(stub.methodSpec).generates("""
      /**
       * Implements helloworld.Greeter.BidiStreamSayHello as a coroutine.
       *
       * When gRPC receives a helloworld.Greeter.BidiStreamSayHello RPC, gRPC will invoke this method within the
       * [kotlin.coroutines.CoroutineContext] used to create this `ThisImpl`.
       *
       * If this method completes without throwing an exception, gRPC will close the 
       * `responses` channel (if it is not already closed), finish sending any messages 
       * remaining in it, and close the RPC with [io.grpc.Status.OK].  Note that if other coroutines
       * are still writing to `responses`, they may get a [kotlinx.coroutines.channels.ClosedSendChannelException].  Make
       * sure all responses are sent before this method completes, e.g. by wrapping your
       * implementation in [kotlinx.coroutines.coroutineScope] or explicitly [joining][kotlinx.coroutines.Job.join] the jobs
       * that are sending responses.
       *
       * If this method throws an exception, the server will abort sending responses and close the
       * RPC with a [io.grpc.Status] [inferred][io.grpc.Status.fromThrowable] from the thrown exception. Other
       * RPCs will not be affected.
       *
       * @param requests A [kotlinx.coroutines.channels.ReceiveChannel] where client requests can be read.
       *       [Cancelling][kotlinx.coroutines.channels.ReceiveChannel.cancel] this channel can be used to indicate that
       *       further client requests should be discarded.
       *
       * @param responses A [kotlinx.coroutines.channels.SendChannel] to send responses to.  Explicitly closing this
       * channel when the RPC is done is optional.  If this channel is closed with a nonnull cause,
       * the RPC will be closed with a [io.grpc.Status] [inferred][io.grpc.Status.fromException] from that
       * cause.  [Sending][kotlinx.coroutines.channels.SendChannel.send] to this channel may suspend if additional responses
       * cannot be sent without excess buffering; see [io.grpc.ServerCall.Listener.onReady] for details.
       */
      open suspend fun bidiStreamSayHello(requests: kotlinx.coroutines.channels.ReceiveChannel<io.grpc.examples.helloworld.HelloRequest>, responses: kotlinx.coroutines.channels.SendChannel<io.grpc.examples.helloworld.HelloReply>) {
        throw io.grpc.StatusException(io.grpc.Status.UNIMPLEMENTED.withDescription("Method helloworld.Greeter.BidiStreamSayHello is unimplemented"))
      }
    """.trimIndent())
    assertThat(stub.serverMethodDef.toString()).isEqualTo("""
      io.grpc.kotlin.ServerCalls.bidiStreamingServerMethodDefinition(
        scope = this,
        descriptor = io.grpc.examples.helloworld.GreeterGrpc.getBidiStreamSayHelloMethod(),
        implementation = ::bidiStreamSayHello
      )
    """.trimIndent())
  }

  @Test
  fun fullImpl() {
    val type = generator.generate(serviceDescriptor)
    assertThat(type).generatesEnclosed(
      Resources.toString(
        Resources.getResource("io/grpc/kotlin/generator/GreeterCoroutineImplBase.expected"),
        Charsets.UTF_8
      )
    )
  }
}