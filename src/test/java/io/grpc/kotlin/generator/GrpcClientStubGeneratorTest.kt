package io.grpc.kotlin.generator

import com.google.common.io.Resources
import com.google.protobuf.Descriptors.MethodDescriptor
import com.google.protobuf.Descriptors.ServiceDescriptor
import com.google.protobuf.kotlin.protoc.GeneratorConfig
import com.google.protobuf.kotlin.protoc.JavaPackagePolicy
import com.google.protobuf.kotlin.protoc.testing.assertThat
import io.grpc.examples.helloworld.HelloWorldProto
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GrpcClientStubGeneratorTest {
  companion object {
    private val generator =
      GrpcClientStubGenerator(GeneratorConfig(JavaPackagePolicy.GOOGLE_INTERNAL, false))
    private val greeterServiceDescriptor: ServiceDescriptor =
      HelloWorldProto.getDescriptor().findServiceByName("Greeter")
    private val unaryMethodDescriptor: MethodDescriptor =
      greeterServiceDescriptor.findMethodByName("SayHello")
    private val bidiStreamingMethodDescriptor: MethodDescriptor =
      greeterServiceDescriptor.findMethodByName("BidiStreamSayHello")
  }

  @Test
  fun generateUnaryRpcStub() {
    assertThat(generator.generateRpcStub(unaryMethodDescriptor)).generates(
      """
      /**
       * Issues a unary helloworld.Greeter.SayHello RPC on this stub's [io.grpc.Channel].
       *
       * The implementation may launch further coroutines, which are run as if by
       * [`withContext(coroutineContext)`][kotlinx.coroutines.withContext].  (Some work may also be done in the
       * [java.util.concurrent.Executor] associated with the `io.grpc.Channel`.)
       *
       * @param request The single argument to the RPC, sent to the server.
       *
       * @return The single response from the server.  This coroutine suspends until the server
       * returns it and closes with [io.grpc.Status.OK], at which point this coroutine resumes with the
       * result. If the RPC fails (closes with another [io.grpc.Status]), this will fail with a
       * corresponding [io.grpc.StatusException].
       */
      suspend fun sayHello(request: io.grpc.examples.helloworld.HelloRequest, headers: io.grpc.Metadata = io.grpc.Metadata()): io.grpc.examples.helloworld.HelloReply = kotlinx.coroutines.withContext(coroutineContext) {
        io.grpc.kotlin.ClientCalls.unaryRpc(
          this,
          channel,
          io.grpc.examples.helloworld.GreeterGrpc.getSayHelloMethod(),
          request,
          callOptions,
          headers
        )
      }
      """.trimIndent()
    )
  }

  @Test
  fun generateBidiStreamingRpcStub() {
    assertThat(generator.generateRpcStub(bidiStreamingMethodDescriptor)).generates(
      """
      /**
       * Issues a bidirectional streaming helloworld.Greeter.BidiStreamSayHello RPC on this stub's [io.grpc.Channel].
       *
       * The implementation may launch further coroutines, which are run as if by
       * [`withContext(coroutineContext)`][kotlinx.coroutines.withContext].  (Some work may also be done in the
       * [java.util.concurrent.Executor] associated with the `io.grpc.Channel`.)
       *
       * @param requests A [kotlinx.coroutines.channels.ReceiveChannel] of requests to be sent to the server; 
       * expected to be provided, populated, and closed by the client.  When requests is closed,
       * the RPC stream will be closed; if it is closed with a nonnull cause, the RPC is cancelled
       * and the cause is sent to the server as the reason for cancellation.
       *
       * @return A [`kotlinx.coroutines.channels.ReceiveChannel<io.grpc.examples.helloworld.HelloReply>`][kotlinx.coroutines.channels.ReceiveChannel] for responses from the server.  If
       * cancelled, the RPC is shut down and a cancellation with that cause is sent to the server.  
       * Alternately, if the RPC fails (closes with a [io.grpc.Status] other than `io.grpc.Status.OK`), the
       * returned channel is [closed][kotlinx.coroutines.channels.SendChannel.close] with a corresponding [io.grpc.StatusException].
       */
      fun bidiStreamSayHello(requests: kotlinx.coroutines.channels.ReceiveChannel<io.grpc.examples.helloworld.HelloRequest>, headers: io.grpc.Metadata = io.grpc.Metadata()): kotlinx.coroutines.channels.ReceiveChannel<io.grpc.examples.helloworld.HelloReply> = io.grpc.kotlin.ClientCalls.bidiStreamingRpc(
        kotlinx.coroutines.CoroutineScope(coroutineContext),
        channel,
        io.grpc.examples.helloworld.GreeterGrpc.getBidiStreamSayHelloMethod(),
        requests,
        callOptions,
        headers
      )
      """.trimIndent()
    )
  }

  @Test
  fun generateServiceStub() {
    assertThat(generator.generate(greeterServiceDescriptor)).generatesEnclosed(
      Resources.toString(
        Resources.getResource("io/grpc/kotlin/generator/GreeterCoroutineStub.expected"),
        Charsets.UTF_8
      )
    )
  }
}
