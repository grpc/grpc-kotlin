package io.grpc.kotlin

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.extensions.proto.ProtoTruth
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ClientInterceptors
import io.grpc.MethodDescriptor
import io.grpc.examples.helloworld.GreeterGrpcKt
import io.grpc.examples.helloworld.HelloRequest
import io.grpc.examples.helloworld.MultiHelloRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.UUID
import kotlin.coroutines.CoroutineContext

@RunWith(JUnit4::class)
class ClientCallOptionsCoroutineContextPropagationTest : AbstractCallsTest() {

  @Test
  fun `should capture coroutine context with unary call`() {
    val server = object : GreeterGrpcKt.GreeterCoroutineImplBase() {
      override suspend fun sayHello(request: HelloRequest) = helloReply("Hello, ${request.name}!")
    }
    val interceptor = CoroutineContextCapturingInterceptor()
    val contextElement = DummyCoroutineContextElement()
    val channel = ClientInterceptors.intercept(makeChannel(server), interceptor)
    val stub = GreeterGrpcKt.GreeterCoroutineStub(channel)

    runBlocking {
      withContext(contextElement) {
        ProtoTruth.assertThat(stub.sayHello(helloRequest("Steven")))
          .isEqualTo(helloReply("Hello, Steven!"))
      }
    }
    assertThat(interceptor.coroutineContext).isNotNull()
    assertThat(interceptor.coroutineContext!![DummyCoroutineContextElement]).isEqualTo(contextElement)
  }

  @Test
  fun `should capture coroutine context with client streaming`() {
    val server = object : GreeterGrpcKt.GreeterCoroutineImplBase() {
      override suspend fun clientStreamSayHello(requests: Flow<HelloRequest>) = requests.map { request ->
        helloReply("Hello, ${request.name}!")
      }.first()
    }
    val interceptor = CoroutineContextCapturingInterceptor()
    val contextElement = DummyCoroutineContextElement()
    val channel = ClientInterceptors.intercept(makeChannel(server), interceptor)
    val stub = GreeterGrpcKt.GreeterCoroutineStub(channel)

    runBlocking {
      withContext(contextElement) {
        ProtoTruth.assertThat(stub.clientStreamSayHello(flowOf(helloRequest("Steven"))))
          .isEqualTo(helloReply("Hello, Steven!"))
      }
    }
    assertThat(interceptor.coroutineContext).isNotNull()
    assertThat(interceptor.coroutineContext!![DummyCoroutineContextElement]).isEqualTo(contextElement)
  }

  @Test
  fun `should capture coroutine context with server streaming`() {
    val server = object : GreeterGrpcKt.GreeterCoroutineImplBase() {
      override fun serverStreamSayHello(request: MultiHelloRequest) = flowOf(
        helloReply("Hello, ${request.nameList.joinToString()}!")
      )
    }
    val interceptor = CoroutineContextCapturingInterceptor()
    val contextElement = DummyCoroutineContextElement()
    val channel = ClientInterceptors.intercept(makeChannel(server), interceptor)
    val stub = GreeterGrpcKt.GreeterCoroutineStub(channel)

    runBlocking {
      withContext(contextElement) {
        ProtoTruth.assertThat(stub.serverStreamSayHello(multiHelloRequest("Steven", "Andrew")).first())
          .isEqualTo(helloReply("Hello, Steven, Andrew!"))
      }
    }
    assertThat(interceptor.coroutineContext).isNotNull()
    assertThat(interceptor.coroutineContext!![DummyCoroutineContextElement]).isEqualTo(contextElement)
  }

  @Test
  fun `should capture coroutine context with bidi streaming`() {
    val server = object : GreeterGrpcKt.GreeterCoroutineImplBase() {
      override fun bidiStreamSayHello(requests: Flow<HelloRequest>) = requests.map { request ->
        helloReply("Hello, ${request.name}!")
      }
    }
    val interceptor = CoroutineContextCapturingInterceptor()
    val contextElement = DummyCoroutineContextElement()
    val channel = ClientInterceptors.intercept(makeChannel(server), interceptor)
    val stub = GreeterGrpcKt.GreeterCoroutineStub(channel)

    runBlocking {
      withContext(contextElement) {
        ProtoTruth.assertThat(stub.bidiStreamSayHello(flowOf(helloRequest("Steven"))).first())
          .isEqualTo(helloReply("Hello, Steven!"))
      }
    }
    assertThat(interceptor.coroutineContext).isNotNull()
    assertThat(interceptor.coroutineContext!![DummyCoroutineContextElement]).isEqualTo(contextElement)
  }
}

private data class DummyCoroutineContextElement(val value: UUID = UUID.randomUUID()) : CoroutineContext.Element {
  override val key: CoroutineContext.Key<*> = Key

  companion object Key : CoroutineContext.Key<DummyCoroutineContextElement>
}

private class CoroutineContextCapturingInterceptor : ClientInterceptor {

  var coroutineContext: CoroutineContext? = null

  override fun <ReqT : Any?, RespT : Any?> interceptCall(
    method: MethodDescriptor<ReqT, RespT>,
    callOptions: CallOptions,
    next: Channel,
  ): ClientCall<ReqT, RespT> {
    coroutineContext = callOptions.coroutineContext

    return next.newCall(method, callOptions)
  }
}
