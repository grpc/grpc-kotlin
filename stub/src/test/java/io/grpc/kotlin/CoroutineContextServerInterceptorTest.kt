package io.grpc.kotlin

import com.google.common.truth.Truth.assertThat
import io.grpc.Metadata as GrpcMetadata
import io.grpc.ServerCall
import io.grpc.ServerInterceptors
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.examples.helloworld.GreeterGrpcKt.GreeterCoroutineImplBase
import io.grpc.examples.helloworld.GreeterGrpcKt.GreeterCoroutineStub
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Tests for [CoroutineContextServerInterceptor]. */
@RunWith(JUnit4::class)
class CoroutineContextServerInterceptorTest : AbstractCallsTest() {
  class ArbitraryContextElement(val message: String = "") : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<ArbitraryContextElement>

    override val key: CoroutineContext.Key<*>
      get() = Key
  }

  class HelloReplyWithContextMessage(message: String? = null) :
    GreeterCoroutineImplBase(
      message?.let { ArbitraryContextElement(it) } ?: EmptyCoroutineContext
    ) {
    override suspend fun sayHello(request: HelloRequest): HelloReply =
      helloReply(coroutineContext[ArbitraryContextElement]!!.message)
  }

  @Test
  fun injectContext() {
    val interceptor =
      object : CoroutineContextServerInterceptor() {
        override fun coroutineContext(
          call: ServerCall<*, *>,
          headers: GrpcMetadata
        ): CoroutineContext = ArbitraryContextElement("success")
      }

    val channel = makeChannel(HelloReplyWithContextMessage(), interceptor)
    val client = GreeterCoroutineStub(channel)

    runBlocking { assertThat(client.sayHello(helloRequest("")).message).isEqualTo("success") }
  }

  @Test
  fun conflictingInterceptorsInnermostWins() {
    val interceptor1 =
      object : CoroutineContextServerInterceptor() {
        override fun coroutineContext(
          call: ServerCall<*, *>,
          headers: GrpcMetadata
        ): CoroutineContext = ArbitraryContextElement("first")
      }
    val interceptor2 =
      object : CoroutineContextServerInterceptor() {
        override fun coroutineContext(
          call: ServerCall<*, *>,
          headers: GrpcMetadata
        ): CoroutineContext = ArbitraryContextElement("second")
      }

    val channel =
      makeChannel(
        ServerInterceptors.intercept(
          ServerInterceptors.intercept(HelloReplyWithContextMessage(), interceptor2),
          interceptor1
        )
      )
    val client = GreeterCoroutineStub(channel)

    runBlocking { assertThat(client.sayHello(helloRequest("")).message).isEqualTo("second") }
  }

  @Test
  fun interceptorContextTakesPriority() {
    val interceptor =
      object : CoroutineContextServerInterceptor() {
        override fun coroutineContext(
          call: ServerCall<*, *>,
          headers: GrpcMetadata
        ): CoroutineContext = ArbitraryContextElement("interceptor")
      }

    val channel = makeChannel(HelloReplyWithContextMessage("server"), interceptor)
    val client = GreeterCoroutineStub(channel)

    runBlocking { assertThat(client.sayHello(helloRequest("")).message).isEqualTo("interceptor") }
  }

  @Test
  fun statusExceptionThrownFromCoroutineContextClosesCall() {
    val interceptor =
      object : CoroutineContextServerInterceptor() {
        override fun coroutineContext(
          call: ServerCall<*, *>,
          headers: GrpcMetadata
        ): CoroutineContext {
          throw StatusException(Status.INTERNAL.withDescription("An error"))
        }
      }

    val channel = makeChannel(HelloReplyWithContextMessage("server"), interceptor)
    val client = GreeterCoroutineStub(channel)

    runBlocking { assertThrows<StatusException> { client.sayHello(helloRequest("")) } }
  }

  @Test
  fun retainsTrailersFromStatusExceptionThrownFromCoroutineContext() {
    val aMetadataKey = GrpcMetadata.Key.of("a-metadata-key", GrpcMetadata.ASCII_STRING_MARSHALLER)
    val interceptor =
      object : CoroutineContextServerInterceptor() {
        override fun coroutineContext(
          call: ServerCall<*, *>,
          headers: GrpcMetadata
        ): CoroutineContext {
          val trailers = GrpcMetadata().apply { put(aMetadataKey, "A value") }
          throw StatusException(Status.INTERNAL, trailers)
        }
      }

    val channel = makeChannel(HelloReplyWithContextMessage("server"), interceptor)
    val client = GreeterCoroutineStub(channel)

    runBlocking {
      val thrown = assertThrows<StatusException> { client.sayHello(helloRequest("")) }

      assertThat(thrown.trailers.get(aMetadataKey)).isEqualTo("A value")
    }
  }
}
