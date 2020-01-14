/*
 * Copyright 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.kotlin

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.extensions.proto.ProtoTruth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import com.google.testing.testsize.MediumTest
import com.google.testing.testsize.MediumTestAttribute
import io.grpc.CallOptions
import io.grpc.ClientCall
import io.grpc.Context
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.examples.helloworld.Greeter
import io.grpc.examples.helloworld.GreeterGrpc
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import io.grpc.examples.helloworld.MultiHelloRequest
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.TimeUnit

/** Tests for [ClientCalls]. */
@RunWith(JUnit4::class)
@MediumTest(MediumTestAttribute.THREADS)
class ClientCallsTest: AbstractCallsTest() {

  /**
   * Verifies that a simple unary RPC successfully returns results to a suspend function.
   */
  @Test
  fun simpleUnary() = runBlocking {
    val serverImpl = object : GreeterGrpc.GreeterImplBase() {
      override fun sayHello(request: HelloRequest, responseObserver: StreamObserver<HelloReply>) {
        responseObserver.onNext(helloReply("Hello, ${request.name}"))
        responseObserver.onCompleted()
      }
    }

    channel = makeChannel(serverImpl)

    assertThat(
      ClientCalls.unaryRpc(
        scope = this,
        channel = channel,
        callOptions = CallOptions.DEFAULT,
        method = sayHelloMethod,
        request = helloRequest("Cindy")
      )
    ).isEqualTo(helloReply("Hello, Cindy"))

    assertThat(
      ClientCalls.unaryRpc(
        scope = this,
        channel = channel,
        callOptions = CallOptions.DEFAULT,
        method = sayHelloMethod,
        request = helloRequest("Jeff")
      )
    ).isEqualTo(helloReply("Hello, Jeff"))
  }

  /**
   * Verify that a unary RPC that does not respond within a timeout specified by [CallOptions]
   * fails on the client with a DEADLINE_EXCEEDED and is cancelled on the server.
   */
  @Test
  fun unaryServerDoesNotRespondGrpcTimeout() = runBlocking {
    val serverCancelled = Job()

    val serverImpl = object : GreeterGrpc.GreeterImplBase() {
      override fun sayHello(request: HelloRequest, responseObserver: StreamObserver<HelloReply>) {
        whenContextIsCancelled { serverCancelled.complete() }
      }
    }

    channel = makeChannel(serverImpl)

    val ex = assertThrows<StatusException> {
      ClientCalls.unaryRpc(
        scope = this,
        channel = channel,
        callOptions = CallOptions.DEFAULT.withDeadlineAfter(200, TimeUnit.MILLISECONDS),
        method = sayHelloMethod,
        request = helloRequest("Jeff")
      )
    }
    assertThat(ex.status.code).isEqualTo(Status.Code.DEADLINE_EXCEEDED)
    serverCancelled.join()
  }

  /** Verify that a server that sends two responses to a unary RPC causes an exception. */
  @Test
  fun unaryTooManyResponses() = runBlocking {
    val serverImpl = object : GreeterGrpc.GreeterImplBase() {
      override fun sayHello(request: HelloRequest, responseObserver: StreamObserver<HelloReply>) {
        responseObserver.onNext(helloReply("Hello, ${request.name}"))
        responseObserver.onNext(helloReply("It's nice to meet you, ${request.name}"))
        responseObserver.onCompleted()
      }
    }

    channel = makeChannel(serverImpl)

    // Apparently this fails with a server cancellation.
    assertThrows<Exception> {
      ClientCalls.unaryRpc(
        scope = this,
        channel = channel,
        callOptions = CallOptions.DEFAULT,
        method = sayHelloMethod,
        request = helloRequest("Cindy")
      )
    }
    Unit
  }

  /** Verify that a server that sends zero responses to a unary RPC causes an exception. */
  @Test
  fun unaryNoResponses() = runBlocking {
    val serverImpl = object : GreeterGrpc.GreeterImplBase() {
      override fun sayHello(request: HelloRequest, responseObserver: StreamObserver<HelloReply>) {
        responseObserver.onCompleted()
      }
    }

    channel = makeChannel(serverImpl)

    // Apparently this fails with a server cancellation.
    assertThrows<Exception> {
      ClientCalls.unaryRpc(
        scope = this,
        channel = channel,
        callOptions = CallOptions.DEFAULT,
        method = sayHelloMethod,
        request = helloRequest("Cindy")
      )
    }
    Unit
  }

  /**
   * Verify that cancelling a coroutine job that includes the RPC as a subtask propagates the
   * cancellation to the server.
   */
  @Test
  fun unaryCancelCoroutinePropagatesToServer() = runBlocking {
    // Completes if and only if the server processes cancellation.
    val serverReceived = Job()
    val serverCancelled = Job()

    val serverImpl = object : GreeterGrpc.GreeterImplBase() {
      override fun sayHello(request: HelloRequest, responseObserver: StreamObserver<HelloReply>) {
        serverReceived.complete()
        whenContextIsCancelled { serverCancelled.complete() }
      }
    }

    channel = makeChannel(serverImpl)

    val job = async {
      ClientCalls.unaryRpc(
        scope = this,
        channel = channel,
        callOptions = CallOptions.DEFAULT,
        method = sayHelloMethod,
        request = helloRequest("Jeff")
      )
    }
    serverReceived.join()
    job.cancel()
    serverCancelled.join()
  }

  @Test
  fun unaryServerExceptionPropagated() = runBlocking {
    val serverImpl = object : GreeterGrpc.GreeterImplBase() {
      override fun sayHello(request: HelloRequest, responseObserver: StreamObserver<HelloReply>) {
        throw IllegalArgumentException("No hello for you!")
      }
    }

    channel = makeChannel(serverImpl)

    val ex = assertThrows<StatusException> {
      ClientCalls.unaryRpc(
        scope = this,
        channel = channel,
        callOptions = CallOptions.DEFAULT,
        method = sayHelloMethod,
        request = helloRequest("Cindy")
      )
    }
    assertThat(ex.status.code).isEqualTo(Status.Code.UNKNOWN)
  }

  @Test
  fun unaryRejectsNonUnaryMethod() = runBlocking {
    val serverImpl = object : GreeterGrpc.GreeterImplBase() {}

    channel = makeChannel(serverImpl)

    assertThrows<IllegalArgumentException> {
      ClientCalls.unaryRpc(
        scope = this,
        channel = channel,
        callOptions = CallOptions.DEFAULT,
        method = clientStreamingSayHelloMethod,
        request = helloRequest("Cindy")
      )
    }
    Unit
  }

  @Test
  fun serverStreamingRejectsNonServerStreamingMethod() = runBlocking {
    val serverImpl = object : GreeterGrpc.GreeterImplBase() {}

    channel = makeChannel(serverImpl)

    assertThrows<IllegalArgumentException> {
      ClientCalls.serverStreamingRpc(
        scope = this,
        channel = channel,
        method = sayHelloMethod,
        request = helloRequest("Cindy"),
        callOptions = CallOptions.DEFAULT
      )
    }
    Unit
  }

  @Test
  fun simpleServerStreamingRpc() = runBlocking {
    val serverImpl = object : GreeterGrpc.GreeterImplBase() {
      override fun serverStreamSayHello(
        request: MultiHelloRequest,
        responseObserver: StreamObserver<HelloReply>
      ) {
        for (name in request.nameList) {
          responseObserver.onNext(helloReply("Hello, $name"))
        }
        responseObserver.onCompleted()
      }
    }

    channel = makeChannel(serverImpl)

    val rpc = ClientCalls.serverStreamingRpc(
      scope = this,
      channel = channel,
      method = serverStreamingSayHelloMethod,
      request = multiHelloRequest("Cindy", "Jeff", "Aki")
    )

    assertThat(rpc.toList()).containsExactly(
      helloReply("Hello, Cindy"), helloReply("Hello, Jeff"), helloReply("Hello, Aki")
    ).inOrder()
  }

  @Test
  fun serverStreamingRpcCancellation() = runBlocking {
    val serverCancelled = Job()
    val serverReceived = Job()
    val serverImpl = object : GreeterGrpc.GreeterImplBase() {
      override fun serverStreamSayHello(
        request: MultiHelloRequest,
        responseObserver: StreamObserver<HelloReply>
      ) {
        whenContextIsCancelled { serverCancelled.complete() }
        serverReceived.complete()
        for (name in request.nameList) {
          responseObserver.onNext(helloReply("Hello, $name"))
        }
        responseObserver.onCompleted()
      }
    }

    channel = makeChannel(serverImpl)

    val rpc = ClientCalls.serverStreamingRpc(
      scope = this,
      channel = channel,
      method = serverStreamingSayHelloMethod,
      request = multiHelloRequest("Tim", "Jim", "Pym")
    )
    serverReceived.join()
    rpc.cancel(CancellationException("no longer needed"))
    serverCancelled.join()
  }

  @Test
  fun simpleClientStreamingRpc() = runBlocking {
    val serverImpl = object : GreeterGrpc.GreeterImplBase() {
      override fun clientStreamSayHello(
        responseObserver: StreamObserver<HelloReply>
      ): StreamObserver<HelloRequest> {
        return object : StreamObserver<HelloRequest> {
          private val names = mutableListOf<String>()

          override fun onNext(value: HelloRequest) {
            names += value.name
          }

          override fun onError(t: Throwable) = throw t

          override fun onCompleted() {
            responseObserver.onNext(
              helloReply(names.joinToString(prefix = "Hello, ", separator = ", "))
            )
            responseObserver.onCompleted()
          }
        }
      }
    }

    channel = makeChannel(serverImpl)

    val requests = produce<HelloRequest> {
      send(helloRequest("Tim"))
      send(helloRequest("Jim"))
    }
    assertThat(
      ClientCalls.clientStreamingRpc(
        scope = this,
        channel = channel,
        method = clientStreamingSayHelloMethod,
        requests = requests
      )
    ).isEqualTo(helloReply("Hello, Tim, Jim"))
  }

  @Test
  fun clientStreamingRpcReturnsEarly() = runBlocking {
    val serverImpl = object : GreeterGrpc.GreeterImplBase() {
      override fun clientStreamSayHello(
        responseObserver: StreamObserver<HelloReply>
      ): StreamObserver<HelloRequest> {
        return object : StreamObserver<HelloRequest> {
          private val names = mutableListOf<String>()
          private var isComplete = false

          override fun onNext(value: HelloRequest) {
            names += value.name
            if (names.size >= 2 && !isComplete) {
              onCompleted()
            }
          }

          override fun onError(t: Throwable) = throw t

          override fun onCompleted() {
            if (!isComplete) {
              responseObserver.onNext(
                helloReply(names.joinToString(prefix = "Hello, ", separator = ", "))
              )
              responseObserver.onCompleted()
              isComplete = true
            }
          }
        }
      }
    }

    channel = makeChannel(serverImpl)

    val requests = Channel<HelloRequest>()
    val response = async {
      ClientCalls.clientStreamingRpc(
        scope = this,
        channel = channel,
        method = clientStreamingSayHelloMethod,
        requests = requests
      )
    }
    requests.send(helloRequest("Tim"))
    requests.send(helloRequest("Jim"))
    assertThat(response.await()).isEqualTo(helloReply("Hello, Tim, Jim"))
    try {
      requests.send(helloRequest("John"))
    } catch (allowed: CancellationException) {
      // Either this should successfully send, or the channel should be cancelled; either is
      // acceptable.  The one unacceptable outcome would be for these operations to suspend
      // indefinitely, waiting for them to be sent.
    }
  }

  @Test
  fun clientStreamingRpcCancellation() = runBlocking {
    val serverCancelled = Job()
    val serverReceived = Job()
    val serverImpl = object : GreeterGrpc.GreeterImplBase() {
      override fun clientStreamSayHello(
        responseObserver: StreamObserver<HelloReply>
      ): StreamObserver<HelloRequest> {
        return object : StreamObserver<HelloRequest> {
          private val names = mutableListOf<String>()

          override fun onNext(value: HelloRequest) {
            whenContextIsCancelled { serverCancelled.complete() }
            Context.current().withCancellation().addListener(
              Context.CancellationListener {
                serverCancelled.complete()
              },
              directExecutor()
            )
            serverReceived.complete()
            names += value.name
          }

          override fun onError(t: Throwable) = throw t

          override fun onCompleted() {
            responseObserver.onNext(
              helloReply(names.joinToString(prefix = "Hello, ", separator = ", "))
            )
            responseObserver.onCompleted()
          }
        }
      }
    }

    channel = makeChannel(serverImpl)

    val requests = Channel<HelloRequest>()
    val rpc = async {
      ClientCalls.clientStreamingRpc(
        scope = this,
        channel = channel,
        method = clientStreamingSayHelloMethod,
        requests = requests
      )
    }
    requests.send(helloRequest("Tim"))
    serverReceived.join()
    rpc.cancel(CancellationException("no longer needed"))
    serverCancelled.join()
  }

  @Test
  fun clientStreamingRpcCancelled() = runBlocking {
    val serverImpl = object : GreeterGrpc.GreeterImplBase() {
      override fun clientStreamSayHello(
        responseObserver: StreamObserver<HelloReply>
      ): StreamObserver<HelloRequest> {
        return object : StreamObserver<HelloRequest> {
          private val names = mutableListOf<String>()

          override fun onNext(value: HelloRequest) {
            names += value.name
          }

          override fun onError(t: Throwable) = throw t

          override fun onCompleted() {
            responseObserver.onNext(
              helloReply(names.joinToString(prefix = "Hello, ", separator = ", "))
            )
            responseObserver.onCompleted()
          }
        }
      }
    }

    channel = makeChannel(serverImpl)

    val requests = Channel<HelloRequest>()
    val response = async {
      ClientCalls.clientStreamingRpc(
        scope = this,
        channel = channel,
        method = clientStreamingSayHelloMethod,
        requests = requests
      )
    }
    requests.send(helloRequest("Tim"))
    response.cancel()
    response.join()
    assertThrows<CancellationException> {
      requests.send(helloRequest("John"))
    }
  }

  @Test
  fun simpleBidiStreamingRpc() = runBlocking {
    val serverImpl = object : GreeterGrpc.GreeterImplBase() {
      override fun bidiStreamSayHello(
        responseObserver: StreamObserver<HelloReply>
      ): StreamObserver<HelloRequest> {
        return object : StreamObserver<HelloRequest> {
          override fun onNext(value: HelloRequest) {
            responseObserver.onNext(helloReply("Hello, ${value.name}"))
          }

          override fun onError(t: Throwable) = throw t

          override fun onCompleted() {
            responseObserver.onCompleted()
          }
        }
      }
    }

    channel = makeChannel(serverImpl)

    val requests = Channel<HelloRequest>()
    val rpc = ClientCalls.bidiStreamingRpc(
      scope = this,
      channel = channel,
      method = bidiStreamingSayHelloMethod,
      requests = requests
    )
    requests.send(helloRequest("Tim"))
    assertThat(rpc.receive()).isEqualTo(helloReply("Hello, Tim"))
    requests.send(helloRequest("Jim"))
    assertThat(rpc.receive()).isEqualTo(helloReply("Hello, Jim"))
    requests.close()
    assertThat(rpc.iterator().hasNext()).isFalse()
  }

  @Test
  fun bidiStreamingRpcReturnsEarly() = runBlocking {
    val serverImpl = object : GreeterGrpc.GreeterImplBase() {
      override fun bidiStreamSayHello(
        responseObserver: StreamObserver<HelloReply>
      ): StreamObserver<HelloRequest> {
        return object : StreamObserver<HelloRequest> {
          private var responseCount = 0

          override fun onNext(value: HelloRequest) {
            responseCount++
            responseObserver.onNext(helloReply("Hello, ${value.name}"))
            if (responseCount >= 2) {
              onCompleted()
            }
          }

          override fun onError(t: Throwable) = throw t

          override fun onCompleted() {
            responseObserver.onCompleted()
          }
        }
      }
    }

    channel = makeChannel(serverImpl)

    val requests = Channel<HelloRequest>()
    val rpc = ClientCalls.bidiStreamingRpc(
      scope = this,
      channel = channel,
      method = bidiStreamingSayHelloMethod,
      requests = requests
    )
    requests.send(helloRequest("Tim"))
    assertThat(rpc.receive()).isEqualTo(helloReply("Hello, Tim"))
    requests.send(helloRequest("Jim"))
    assertThat(rpc.receive()).isEqualTo(helloReply("Hello, Jim"))
    assertThat(rpc.toList()).isEmpty() // rpc closes responses
    try {
      requests.send(helloRequest("John"))
    } catch (allowed: CancellationException) {
      // Either this should successfully send, or the channel should be cancelled; either is
      // acceptable.  The one unacceptable outcome would be for these operations to suspend
      // indefinitely, waiting for them to be sent.
    }
  }
}
