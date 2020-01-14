package io.grpc.kotlin

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.extensions.proto.ProtoTruth.assertThat
import com.google.testing.testsize.MediumTest
import com.google.testing.testsize.MediumTestAttribute
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.examples.helloworld.GreeterGrpcKt.GreeterCoroutineImplBase
import io.grpc.examples.helloworld.GreeterGrpcKt.GreeterCoroutineStub
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import io.grpc.examples.helloworld.MultiHelloRequest
import io.grpc.examples.helloworld.helloReply
import io.grpc.examples.helloworld.helloRequest
import io.grpc.examples.helloworld.multiHelloRequest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.coroutines.EmptyCoroutineContext

@RunWith(JUnit4::class)
@MediumTest(MediumTestAttribute.THREADS)
class GeneratedCodeTest : AbstractCallsTest() {
  @Test
  fun simpleUnary() {
    val server = object : GreeterCoroutineImplBase() {
      override suspend fun sayHello(request: HelloRequest): HelloReply {
        return helloReply { message = "Hello, ${request.name}!" }
      }
    }
    val channel = makeChannel(server)
    val stub = GreeterCoroutineStub(channel)

    runBlocking {
      assertThat(stub.sayHello(helloRequest { name = "Steven" }))
        .isEqualTo(helloReply { message = "Hello, Steven!" })
    }
  }

  @Test
  fun unaryServerDoesNotRespondGrpcTimeout() = runBlocking {
    val serverCancelled = Job()

    val channel = makeChannel(object : GreeterCoroutineImplBase() {
      override suspend fun sayHello(request: HelloRequest): HelloReply {
        suspendUntilCancelled {
          serverCancelled.complete()
        }
      }
    })

    val stub = GreeterCoroutineStub(channel).withDeadlineAfter(100, TimeUnit.MILLISECONDS)

    val ex = assertThrows<StatusException> {
      stub.sayHello(helloRequest { name = "Topaz" })
    }
    assertThat(ex.status.code).isEqualTo(Status.Code.DEADLINE_EXCEEDED)
    serverCancelled.join()
  }

  @Test
  fun unaryClientCancellation() {
    val helloReceived = Job()
    val helloCancelled = Job()
    val helloChannel = makeChannel(object : GreeterCoroutineImplBase() {
      override suspend fun sayHello(request: HelloRequest): HelloReply {
        helloReceived.complete()
        suspendUntilCancelled {
          helloCancelled.complete()
        }
      }
    })
    val helloStub = GreeterCoroutineStub(helloChannel)

    runBlocking {
      val result = async {
        val request = helloRequest { name = "Steven" }
        helloStub.sayHello(request)
      }
      helloReceived.join()
      result.cancel()
      helloCancelled.join()
    }
  }

  @Test
  fun unaryMethodThrowsStatusException() = runBlocking {
    val channel = makeChannel(
      object : GreeterCoroutineImplBase() {
        override suspend fun sayHello(request: HelloRequest): HelloReply {
          throw StatusException(Status.PERMISSION_DENIED)
        }
      }
    )

    val stub = GreeterCoroutineStub(channel)
    val ex = assertThrows<StatusException> {
      stub.sayHello(helloRequest("Peridot"))
    }
    assertThat(ex.status.code).isEqualTo(Status.Code.PERMISSION_DENIED)
  }

  @Test
  fun unaryMethodThrowsException() = runBlocking {
    val channel = makeChannel(
      object : GreeterCoroutineImplBase() {
        override suspend fun sayHello(request: HelloRequest): HelloReply {
          throw IllegalArgumentException()
        }
      }
    )

    val stub = GreeterCoroutineStub(channel)
    val ex = assertThrows<StatusException> {
      stub.sayHello(helloRequest("Peridot"))
    }
    assertThat(ex.status.code).isEqualTo(Status.Code.UNKNOWN)
  }

  @Test
  fun simpleClientStreamingRpc() = runBlocking {
    val channel = makeChannel(object : GreeterCoroutineImplBase() {
      override suspend fun clientStreamSayHello(
        requests: ReceiveChannel<HelloRequest>
      ): HelloReply {
        return helloReply {
          message = requests.toList().joinToString(prefix = "Hello, ", separator = ", ") { it.name }
        }
      }
    })

    val stub = GreeterCoroutineStub(channel)
    val requests = Channel<HelloRequest>()
    val response = async { stub.clientStreamSayHello(requests) }
    requests.send(helloRequest { name = "Peridot" })
    requests.send(helloRequest { name = "Lapis" })
    requests.close()
    assertThat(response.await()).isEqualTo(helloReply { message = "Hello, Peridot, Lapis" })
  }

  @Test
  fun clientStreamingRpcCancellation() = runBlocking {
    val serverReceived = Job()
    val serverCancelled = Job()
    val channel = makeChannel(object : GreeterCoroutineImplBase() {
      override suspend fun clientStreamSayHello(
        requests: ReceiveChannel<HelloRequest>
      ): HelloReply {
        requests.receive() // TODO(lowasser): discuss whether this is appropriate
        serverReceived.complete()
        suspendUntilCancelled { serverCancelled.complete() }
      }
    })

    val stub = GreeterCoroutineStub(channel)
    val requests = Channel<HelloRequest>()
    val response = async {
      stub.clientStreamSayHello(requests)
    }
    requests.send(helloRequest { name = "Aquamarine" })
    serverReceived.join()
    response.cancel()
    serverCancelled.join()
    assertThrows<CancellationException> {
      requests.send(helloRequest("John"))
    }
  }

  @Test
  fun clientStreamingRpcThrowsStatusException() = runBlocking {
    val channel = makeChannel(object : GreeterCoroutineImplBase() {
      override suspend fun clientStreamSayHello(
        requests: ReceiveChannel<HelloRequest>
      ): HelloReply {
        throw StatusException(Status.PERMISSION_DENIED)
      }
    })
    val stub = GreeterCoroutineStub(channel)
    val requests = Channel<HelloRequest>()

    val ex = assertThrows<StatusException> {
      stub.clientStreamSayHello(requests)
    }
    assertThat(ex.status.code).isEqualTo(Status.Code.PERMISSION_DENIED)
  }

  @Test
  fun clientStreamingRpcReturnsEarly() = runBlocking {
    val channel = makeChannel(object : GreeterCoroutineImplBase() {
      override suspend fun clientStreamSayHello(
        requests: ReceiveChannel<HelloRequest>
      ): HelloReply {
        var count = 0
        val names = mutableListOf<String>()
        for (request in requests) {
          names += request.name
          count++
          if (count >= 2) {
            break
          }
        }
        return helloReply(names.joinToString(prefix = "Hello, ", separator = ", "))
      }
    })

    val stub = GreeterCoroutineStub(channel)
    val requests = Channel<HelloRequest>()
    val response = async { stub.clientStreamSayHello(requests) }
    requests.send(helloRequest { name = "Peridot" })
    requests.send(helloRequest { name = "Lapis" })
    assertThat(response.await()).isEqualTo(helloReply { message = "Hello, Peridot, Lapis" })
    assertThrows<CancellationException> {
      requests.send(helloRequest { name = "Jasper" })
    }
  }

  @Test
  fun simpleServerStreamingRpc() = runBlocking {
    val channel = makeChannel(object : GreeterCoroutineImplBase() {
      override suspend fun serverStreamSayHello(
        request: MultiHelloRequest,
        responses: SendChannel<HelloReply>
      ) {
        for (name in request.nameList) {
          responses.send(helloReply { message = "Hello, $name" })
        }
      }
    })

    val responses = GreeterCoroutineStub(channel).serverStreamSayHello(
      multiHelloRequest {
        name += listOf("Garnet", "Amethyst", "Pearl")
      }
    )

    assertThat(responses.toList())
      .containsExactly(
        helloReply { message = "Hello, Garnet" },
        helloReply { message = "Hello, Amethyst" },
        helloReply { message = "Hello, Pearl" }
      )
      .inOrder()
  }

  @Test
  fun serverStreamingRpcCancellation() = runBlocking {
    val serverCancelled = Job()
    val serverReceived = Job()

    val channel = makeChannel(object : GreeterCoroutineImplBase() {
      override suspend fun serverStreamSayHello(
        request: MultiHelloRequest,
        responses: SendChannel<HelloReply>
      ) {
        serverReceived.complete()
        suspendUntilCancelled {
          serverCancelled.complete()
        }
      }
    })

    val response = GreeterCoroutineStub(channel).serverStreamSayHello(
      multiHelloRequest {
        name += listOf("Topaz", "Aquamarine")
      }
    )
    serverReceived.join()
    response.cancel()
    serverCancelled.join()
  }

  @Test
  fun bidiPingPong() = runBlocking {
    val channel = makeChannel(object : GreeterCoroutineImplBase() {
      override suspend fun bidiStreamSayHello(
        requests: ReceiveChannel<HelloRequest>,
        responses: SendChannel<HelloReply>
      ) {
        for (request in requests) {
          responses.send(helloReply { message = "Hello, ${request.name}" })
        }
      }
    })

    val requests = Channel<HelloRequest>()
    val responses = GreeterCoroutineStub(channel).bidiStreamSayHello(requests)

    requests.send(helloRequest { name = "Steven" })
    assertThat(responses.receive()).isEqualTo(helloReply { message = "Hello, Steven" })
    requests.send(helloRequest { name = "Garnet" })
    assertThat(responses.receive()).isEqualTo(helloReply { message = "Hello, Garnet" })
    requests.close()
    assertThat(responses.toList()).isEmpty()
  }

  @Test
  fun bidiStreamingRpcReturnsEarly() = runBlocking {
    val channel = makeChannel(object : GreeterCoroutineImplBase() {
      override suspend fun bidiStreamSayHello(
        requests: ReceiveChannel<HelloRequest>,
        responses: SendChannel<HelloReply>
      ) {
        var count = 0
        for (request in requests) {
          responses.send(helloReply { message = "Hello, ${request.name}" })
          count++
          if (count >= 2) {
            break
          }
        }
      }
    })

    val stub = GreeterCoroutineStub(channel)
    val requests = Channel<HelloRequest>()
    val responses = stub.bidiStreamSayHello(requests)
    requests.send(helloRequest { name = "Peridot" })
    assertThat(responses.receive()).isEqualTo(helloReply { message = "Hello, Peridot" })
    requests.send(helloRequest { name = "Lapis" })
    assertThat(responses.receive()).isEqualTo(helloReply { message = "Hello, Lapis" })
    assertThat(responses.toList()).isEmpty()
    try {
      requests.send(helloRequest { name = "Jasper" })
    } catch (allowed: CancellationException) {}
  }

  @Test
  fun serverScopeCancelledDuringRpc() = runBlocking {
    val serverJob = Job()
    val serverReceived = Job()
    val channel = makeChannel(
      object : GreeterCoroutineImplBase(serverJob) {
        override suspend fun sayHello(request: HelloRequest): HelloReply {
          serverReceived.complete()
          suspendUntilCancelled { /* do nothing */ }
        }
      }
    )

    val stub = GreeterCoroutineStub(channel)
    val test = launch {
      val ex = assertThrows<StatusException> {
        stub.sayHello(helloRequest { name = "Greg" })
      }
      assertThat(ex.status.code).isEqualTo(Status.Code.CANCELLED)
    }
    serverReceived.join()
    serverJob.cancel()
    test.join()
  }

  @Test
  fun serverScopeCancelledBeforeRpc() = runBlocking {
    val serverJob = Job()
    val channel = makeChannel(
      object : GreeterCoroutineImplBase(serverJob) {
        override suspend fun sayHello(request: HelloRequest): HelloReply {
          suspendUntilCancelled { /* do nothing */ }
        }
      }
    )

    serverJob.cancel()
    val stub = GreeterCoroutineStub(channel)
    val ex = assertThrows<StatusException> {
      stub.sayHello(helloRequest { name = "Greg" })
    }
    assertThat(ex.status.code).isEqualTo(Status.Code.CANCELLED)
  }
}
