package io.grpc.kotlin

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.extensions.proto.ProtoTruth.assertThat
import com.google.testing.testsize.MediumTest
import com.google.testing.testsize.MediumTestAttribute
import io.grpc.CallOptions
import io.grpc.ClientCall
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import io.grpc.examples.helloworld.GreeterGrpc
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@RunWith(JUnit4::class)
@MediumTest(MediumTestAttribute.THREADS)
class ServerCallsTest : AbstractCallsTest() {
  @Test
  fun simpleUnaryMethod() = runBlocking {
    val channel = makeChannel(
      ServerCalls.unaryServerMethodDefinition(this, sayHelloMethod) { request ->
        helloReply("Hello, ${request.name}")
      }
    )

    val stub = GreeterGrpc.newBlockingStub(channel)
    assertThat(stub.sayHello(helloRequest("Steven"))).isEqualTo(helloReply("Hello, Steven"))
    assertThat(stub.sayHello(helloRequest("Pearl"))).isEqualTo(helloReply("Hello, Pearl"))
  }

  @Test
  fun unaryMethodCancellationPropagatedToServer() = runBlocking {
    val requestReceived = Job()
    val cancelled = Job()
    val channel = makeChannel(
      ServerCalls.unaryServerMethodDefinition(this, sayHelloMethod) {
        requestReceived.complete()
        suspendUntilCancelled { cancelled.complete() }
      }
    )

    val stub = GreeterGrpc.newFutureStub(channel)
    val future = stub.sayHello(helloRequest("Garnet"))
    requestReceived.join()
    future.cancel(true)
    cancelled.join()
  }

  @Test
  fun unaryMethodReceivedTooManyRequests() = runBlocking {
    val channel = makeChannel(
      ServerCalls.unaryServerMethodDefinition(this, sayHelloMethod) {
        helloReply("Hello, ${it.name}")
      }
    )
    val call = channel.newCall(sayHelloMethod, CallOptions.DEFAULT)
    val closeStatus = CompletableDeferred<Status>()

    call.start(object : ClientCall.Listener<HelloReply>() {
      override fun onClose(status: Status, trailers: Metadata) {
        closeStatus.complete(status)
      }
    }, Metadata())
    call.request(1)
    call.sendMessage(helloRequest("Amethyst"))
    call.sendMessage(helloRequest("Pearl"))
    call.halfClose()
    val status = closeStatus.await()
    assertThat(status.code).isEqualTo(Status.Code.INTERNAL)
    assertThat(status.description).contains("received two")
  }

  @Test
  fun unaryMethodReceivedNoRequests() = runBlocking {
    val channel = makeChannel(
      ServerCalls.unaryServerMethodDefinition(this, sayHelloMethod) {
        helloReply("Hello, ${it.name}")
      }
    )
    val call = channel.newCall(sayHelloMethod, CallOptions.DEFAULT)
    val closeStatus = CompletableDeferred<Status>()

    call.start(object : ClientCall.Listener<HelloReply>() {
      override fun onClose(status: Status, trailers: Metadata) {
        closeStatus.complete(status)
      }
    }, Metadata())
    call.request(1)
    call.halfClose()
    val status = closeStatus.await()
    assertThat(status.code).isEqualTo(Status.Code.INTERNAL)
    assertThat(status.description).contains("received none")
  }

  @Test
  fun unaryMethodThrowsStatusException() = runBlocking {
    val channel = makeChannel(
      ServerCalls.unaryServerMethodDefinition(this, sayHelloMethod) {
        throw StatusException(Status.OUT_OF_RANGE)
      }
    )

    val stub = GreeterGrpc.newBlockingStub(channel)
    val ex = assertThrows<StatusRuntimeException> {
      stub.sayHello(helloRequest("Peridot"))
    }
    assertThat(ex.status.code).isEqualTo(Status.Code.OUT_OF_RANGE)
  }

  class MyException : Exception()

  @Test
  fun unaryMethodThrowsException() = runBlocking {
    val channel = makeChannel(
      ServerCalls.unaryServerMethodDefinition(this, sayHelloMethod) {
        throw MyException()
      }
    )

    val stub = GreeterGrpc.newBlockingStub(channel)
    val ex = assertThrows<StatusRuntimeException> {
      stub.sayHello(helloRequest("Lapis Lazuli"))
    }
    assertThat(ex.status.code).isEqualTo(Status.Code.UNKNOWN)
  }

  @Test
  fun simpleServerStreaming() = runBlocking {
    val channel = makeChannel(
      ServerCalls.serverStreamingServerMethodDefinition(this, serverStreamingSayHelloMethod)
        { request, responses ->
          for (name in request.nameList) {
            responses.send(helloReply("Hello, $name"))
          }
        }
    )

    val responses = ClientCalls.serverStreamingRpc(
      this,
      channel,
      serverStreamingSayHelloMethod,
      multiHelloRequest("Garnet", "Amethyst", "Pearl")
    )
    assertThat(responses.toList())
      .containsExactly(
        helloReply("Hello, Garnet"),
        helloReply("Hello, Amethyst"),
        helloReply("Hello, Pearl")
      ).inOrder()
  }

  /**
   * Test that if the implementation launches a worker instead of implementing the worker itself,
   * that it completes correctly.
   */
  @Test
  fun serverStreamingLaunches() {
    val channel = makeChannel(
      ServerCalls.serverStreamingServerMethodDefinition(
        GlobalScope,
        serverStreamingSayHelloMethod
      ) { request, responses -> coroutineScope {
          launch {
            for (name in request.nameList) {
              responses.send(helloReply("Hello, $name"))
            }
          }
        }
      }
    )
    runBlocking {
      val responses = ClientCalls.serverStreamingRpc(
        this,
        channel,
        serverStreamingSayHelloMethod,
        multiHelloRequest("Garnet", "Amethyst", "Pearl")
      )
      assertThat(responses.toList())
        .containsExactly(
          helloReply("Hello, Garnet"),
          helloReply("Hello, Amethyst"),
          helloReply("Hello, Pearl")
        ).inOrder()
    }
  }

  @Test
  fun serverStreamingCancellationPropagatedToServer() = runBlocking {
    val requestReceived = Job()
    val cancelled = Job()
    val channel = makeChannel(
      ServerCalls.serverStreamingServerMethodDefinition(
        this,
        serverStreamingSayHelloMethod
      ) { _, _ ->
        requestReceived.complete()
        suspendUntilCancelled { cancelled.complete() }
      }
    )

    val call = channel.newCall(serverStreamingSayHelloMethod, CallOptions.DEFAULT)
    val closeStatus = CompletableDeferred<Status>()
    call.start(object : ClientCall.Listener<HelloReply>() {
      override fun onClose(status: Status, trailers: Metadata) {
        closeStatus.complete(status)
      }
    }, Metadata())
    call.sendMessage(multiHelloRequest("Steven"))
    requestReceived.join()
    call.cancel("Test cancellation", null)
    cancelled.join()
    assertThat(closeStatus.await().code).isEqualTo(Status.Code.CANCELLED)
  }

  @Test
  fun serverStreamingThrowsStatusException() = runBlocking {
    val channel = makeChannel(
      ServerCalls.serverStreamingServerMethodDefinition(
        this,
        serverStreamingSayHelloMethod
      ) { _, _ -> throw StatusException(Status.OUT_OF_RANGE) }
    )

    val call = channel.newCall(serverStreamingSayHelloMethod, CallOptions.DEFAULT)
    val closeStatus = CompletableDeferred<Status>()
    call.start(object : ClientCall.Listener<HelloReply>() {
      override fun onClose(status: Status, trailers: Metadata) {
        closeStatus.complete(status)
      }
    }, Metadata())
    call.sendMessage(multiHelloRequest("Steven"))
    assertThat(closeStatus.await().code).isEqualTo(Status.Code.OUT_OF_RANGE)
  }

  @Test
  fun serverStreamingThrowsException() = runBlocking {
    val channel = makeChannel(
      ServerCalls.serverStreamingServerMethodDefinition(
        this,
        serverStreamingSayHelloMethod
      ) { _, _ -> throw MyException() }
    )

    val call = channel.newCall(serverStreamingSayHelloMethod, CallOptions.DEFAULT)
    val closeStatus = CompletableDeferred<Status>()
    call.start(object : ClientCall.Listener<HelloReply>() {
      override fun onClose(status: Status, trailers: Metadata) {
        closeStatus.complete(status)
      }
    }, Metadata())
    call.sendMessage(multiHelloRequest("Steven"))
    assertThat(closeStatus.await().code).isEqualTo(Status.Code.UNKNOWN)
  }

  @Test
  fun simpleClientStreaming() = runBlocking {
    val channel = makeChannel(
      ServerCalls.clientStreamingServerMethodDefinition(
        this,
        clientStreamingSayHelloMethod
      ) { requests ->
        helloReply(requests.toList().joinToString(separator = ", ", prefix = "Hello, ") { it.name })
      }
    )

    val requestChannel = produce<HelloRequest> {
      send(helloRequest("Ruby"))
      send(helloRequest("Sapphire"))
    }
    assertThat(
      ClientCalls.clientStreamingRpc(
        this,
        channel,
        clientStreamingSayHelloMethod,
        requestChannel
      )
    ).isEqualTo(helloReply("Hello, Ruby, Sapphire"))
  }

  @Test
  fun clientStreamingDoesntWaitForAllRequests() = runBlocking {
    val channel = makeChannel(
      ServerCalls.clientStreamingServerMethodDefinition(
        this,
        clientStreamingSayHelloMethod
      ) { requests ->
        val req1 = requests.receive().name
        val req2 = requests.receive().name
        helloReply("Hello, $req1 and $req2")
      }
    )

    val requestChannel = produce<HelloRequest> {
      send(helloRequest("Peridot"))
      send(helloRequest("Lapis"))
      send(helloRequest("Jasper"))
      send(helloRequest("Aquamarine"))
    }
    assertThat(
      ClientCalls.clientStreamingRpc(
        this,
        channel,
        clientStreamingSayHelloMethod,
        requestChannel
      )
    ).isEqualTo(helloReply("Hello, Peridot and Lapis"))
  }

  @Test
  fun clientStreamingWhenRequestsCancelledNoBackpressure() = runBlocking {
    val barrier = Job()
    val channel = makeChannel(
      ServerCalls.clientStreamingServerMethodDefinition(
        this,
        clientStreamingSayHelloMethod
      ) { requests ->
        val req1 = requests.receive().name
        val req2 = requests.receive().name
        requests.cancel()
        barrier.join()
        helloReply("Hello, $req1 and $req2")
      }
    )

    val requestChannel = Channel<HelloRequest>()
    val response = async {
      ClientCalls.clientStreamingRpc(
        this,
        channel,
        clientStreamingSayHelloMethod,
        requestChannel
      )
    }
    requestChannel.send(helloRequest("Lapis"))
    requestChannel.send(helloRequest("Peridot"))
    for (i in 1..1000) {
      requestChannel.send(helloRequest("Ruby"))
    }
    barrier.complete()
    assertThat(response.await()).isEqualTo(helloReply("Hello, Lapis and Peridot"))
  }

  @Test
  fun clientStreamingCancellationPropagatedToServer() = runBlocking {
    val requestReceived = Job()
    val cancelled = Job()
    val channel = makeChannel(
      ServerCalls.clientStreamingServerMethodDefinition(
        this,
        clientStreamingSayHelloMethod
      ) {
        it.receive()
        requestReceived.complete()
        suspendUntilCancelled { cancelled.complete() }
      }
    )

    val call = channel.newCall(clientStreamingSayHelloMethod, CallOptions.DEFAULT)
    val closeStatus = CompletableDeferred<Status>()
    call.start(object : ClientCall.Listener<HelloReply>() {
      override fun onClose(status: Status, trailers: Metadata) {
        closeStatus.complete(status)
      }
    }, Metadata())
    call.sendMessage(helloRequest("Steven"))
    requestReceived.join()
    call.cancel("Test cancellation", null)
    cancelled.join()
    assertThat(closeStatus.await().code).isEqualTo(Status.Code.CANCELLED)
  }

  @Test
  fun clientStreamingThrowsStatusException() = runBlocking {
    val channel = makeChannel(
      ServerCalls.clientStreamingServerMethodDefinition(
        this,
        clientStreamingSayHelloMethod
      ) { throw StatusException(Status.INVALID_ARGUMENT) }
    )

    val call = channel.newCall(clientStreamingSayHelloMethod, CallOptions.DEFAULT)
    val closeStatus = CompletableDeferred<Status>()
    call.start(object : ClientCall.Listener<HelloReply>() {
      override fun onClose(status: Status, trailers: Metadata) {
        closeStatus.complete(status)
      }
    }, Metadata())
    call.sendMessage(helloRequest("Steven"))
    assertThat(closeStatus.await().code).isEqualTo(Status.Code.INVALID_ARGUMENT)
  }

  @Test
  fun clientStreamingThrowsException() = runBlocking {
    val channel = makeChannel(
      ServerCalls.clientStreamingServerMethodDefinition(
        this,
        clientStreamingSayHelloMethod
      ) { throw MyException() }
    )

    val call = channel.newCall(clientStreamingSayHelloMethod, CallOptions.DEFAULT)
    val closeStatus = CompletableDeferred<Status>()
    call.start(object : ClientCall.Listener<HelloReply>() {
      override fun onClose(status: Status, trailers: Metadata) {
        closeStatus.complete(status)
      }
    }, Metadata())
    call.sendMessage(helloRequest("Steven"))
    assertThat(closeStatus.await().code).isEqualTo(Status.Code.UNKNOWN)
  }

  @Test
  fun simpleBidiStreamingPingPong() = runBlocking {
    val channel = makeChannel(
      ServerCalls.bidiStreamingServerMethodDefinition(this, bidiStreamingSayHelloMethod)
      { requests, responses ->
        for (request in requests) {
          responses.send(helloReply("Hello, ${request.name}"))
        }
        responses.send(helloReply("Goodbye"))
      }
    )

    val requests = Channel<HelloRequest>()
    val responses =
      ClientCalls.bidiStreamingRpc(this, channel, bidiStreamingSayHelloMethod, requests)

    requests.send(helloRequest("Garnet"))
    assertThat(responses.receive()).isEqualTo(helloReply("Hello, Garnet"))
    requests.send(helloRequest("Steven"))
    assertThat(responses.receive()).isEqualTo(helloReply("Hello, Steven"))
    requests.close()
    assertThat(responses.receive()).isEqualTo(helloReply("Goodbye"))
    assertThat(responses.toList()).isEmpty()
  }

  @Test
  fun bidiStreamingCancellationPropagatedToServer() = runBlocking {
    val requestReceived = Job()
    val cancelled = Job()
    val channel = makeChannel(
      ServerCalls.bidiStreamingServerMethodDefinition(
        this,
        bidiStreamingSayHelloMethod
      ) { requests, _ ->
        requests.receive()
        requestReceived.complete()
        suspendUntilCancelled { cancelled.complete() }
      }
    )

    val call = channel.newCall(bidiStreamingSayHelloMethod, CallOptions.DEFAULT)
    val closeStatus = CompletableDeferred<Status>()
    call.start(object : ClientCall.Listener<HelloReply>() {
      override fun onClose(status: Status, trailers: Metadata) {
        closeStatus.complete(status)
      }
    }, Metadata())
    call.sendMessage(helloRequest("Steven"))
    requestReceived.join()
    call.cancel("Test cancellation", null)
    cancelled.join()
    assertThat(closeStatus.await().code).isEqualTo(Status.Code.CANCELLED)
  }

  @Test
  fun bidiStreamingThrowsStatusException() = runBlocking {
    val channel = makeChannel(
      ServerCalls.bidiStreamingServerMethodDefinition(
        this,
        bidiStreamingSayHelloMethod
      ) { _, _ -> throw StatusException(Status.INVALID_ARGUMENT) }
    )

    val call = channel.newCall(bidiStreamingSayHelloMethod, CallOptions.DEFAULT)
    val closeStatus = CompletableDeferred<Status>()
    call.start(object : ClientCall.Listener<HelloReply>() {
      override fun onClose(status: Status, trailers: Metadata) {
        closeStatus.complete(status)
      }
    }, Metadata())
    call.sendMessage(helloRequest("Steven"))
    assertThat(closeStatus.await().code).isEqualTo(Status.Code.INVALID_ARGUMENT)
  }

  @Test
  fun bidiStreamingThrowsException() = runBlocking {
    val channel = makeChannel(
      ServerCalls.bidiStreamingServerMethodDefinition(
        this,
        bidiStreamingSayHelloMethod
      ) { _, _ -> throw MyException() }
    )

    val call = channel.newCall(bidiStreamingSayHelloMethod, CallOptions.DEFAULT)
    val closeStatus = CompletableDeferred<Status>()
    call.start(object : ClientCall.Listener<HelloReply>() {
      override fun onClose(status: Status, trailers: Metadata) {
        closeStatus.complete(status)
      }
    }, Metadata())
    call.sendMessage(helloRequest("Steven"))
    assertThat(closeStatus.await().code).isEqualTo(Status.Code.UNKNOWN)
  }

  @Test
  fun rejectNonUnaryMethod() = runBlocking {
    assertThrows<IllegalArgumentException> {
      ServerCalls.unaryServerMethodDefinition(this, bidiStreamingSayHelloMethod) { TODO() }
    }
  }

  @Test
  fun rejectNonClientStreamingMethod() = runBlocking {
    assertThrows<IllegalArgumentException> {
      ServerCalls
        .clientStreamingServerMethodDefinition(this, sayHelloMethod) { TODO() }
    }
  }

  @Test
  fun rejectNonServerStreamingMethod()  = runBlocking {
    assertThrows<IllegalArgumentException> {
      ServerCalls
        .serverStreamingServerMethodDefinition(this, sayHelloMethod) { _, _ -> TODO() }
    }
  }

  @Test
  fun rejectNonBidiStreamingMethod()  = runBlocking {
    assertThrows<IllegalArgumentException> {
      ServerCalls
        .bidiStreamingServerMethodDefinition(this, sayHelloMethod) { _, _ -> TODO() }
    }
  }

  @Test
  fun unaryContextPropagated() = runBlocking {
    val differentThreadContext: CoroutineContext =
      Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    val contextKey = Context.key<String>("testKey")
    val contextToInject = Context.ROOT.withValue(contextKey, "testValue")

    val interceptor = object: ServerInterceptor {
      override fun <RequestT, ResponseT> interceptCall(
        call: ServerCall<RequestT, ResponseT>,
        headers: Metadata,
        next: ServerCallHandler<RequestT, ResponseT>
      ): ServerCall.Listener<RequestT> {
        return Contexts.interceptCall(
          contextToInject,
          call,
          headers,
          next
        )
      }
    }

    val channel = makeChannel(
      ServerCalls.unaryServerMethodDefinition(this, sayHelloMethod) {
        withContext(differentThreadContext) {
          // Run this in a definitely different thread, just to verify context propagation
          // is WAI.
          assertThat(contextKey.get(Context.current())).isEqualTo("testValue")
          helloReply("Hello, ${it.name}")
        }
      },
      interceptor
    )

    val stub = GreeterGrpc.newBlockingStub(channel)
    assertThat(stub.sayHello(helloRequest("Peridot"))).isEqualTo(helloReply("Hello, Peridot"))
  }

  @Test
  fun serverScopeCancelledDuringRpc() = runBlocking {
    val serverScope = CoroutineScope(EmptyCoroutineContext)
    val serverReceived = Job()
    val channel = makeChannel(
      ServerCalls.unaryServerMethodDefinition(serverScope, sayHelloMethod) {
        serverReceived.complete()
        suspendForever()
      }
    )

    val test = launch {
      val ex = assertThrows<StatusException> {
        ClientCalls.unaryRpc(this, channel, sayHelloMethod, helloRequest("Greg"))
      }
      assertThat(ex.status.code).isEqualTo(Status.Code.CANCELLED)
    }
    serverReceived.join()
    serverScope.cancel()
    test.join()
  }

  @Test
  fun serverScopeCancelledBeforeRpc() = runBlocking {
    val serverScope = CoroutineScope(EmptyCoroutineContext)
    val channel = makeChannel(
      ServerCalls.unaryServerMethodDefinition(serverScope, sayHelloMethod) {
        suspendForever()
      }
    )

    serverScope.cancel()
    val test = launch {
      val ex = assertThrows<StatusException> {
        ClientCalls.unaryRpc(this, channel, sayHelloMethod, helloRequest("Greg"))
      }
      assertThat(ex.status.code).isEqualTo(Status.Code.CANCELLED)
    }
    test.join()
  }

  @Test
  fun serverStreamingFlowControl() = runBlocking {
    val receiveFirstMessage = Job()
    val receivedFirstMessage = Job()
    val channel = makeChannel(
      ServerCalls.serverStreamingServerMethodDefinition(
        this,
        serverStreamingSayHelloMethod
      ) { _, responses ->
        coroutineScope {
          responses.send(helloReply("1st"))
          val secondSend = launch {
            responses.send(helloReply("2nd"))
          }
          delay(200)
          assertThat(secondSend.isCompleted).isFalse()
          receiveFirstMessage.complete()
          receivedFirstMessage.join()
          secondSend.join()
        }
      }
    )

    val responses = ClientCalls.serverStreamingRpc(
      this,
      channel,
      serverStreamingSayHelloMethod,
      multiHelloRequest()
    )
    receiveFirstMessage.join()
    assertThat(responses.receive()).isEqualTo(helloReply("1st"))
    receivedFirstMessage.complete()
    assertThat(responses.toList()).containsExactly(helloReply("2nd"))
  }
}