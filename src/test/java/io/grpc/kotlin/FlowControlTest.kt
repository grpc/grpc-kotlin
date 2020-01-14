package io.grpc.kotlin

import com.google.common.truth.Truth.assertThat
import com.google.testing.testsize.MediumTest
import com.google.testing.testsize.MediumTestAttribute
import io.grpc.examples.helloworld.HelloRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Tests for the flow control of the Kotlin gRPC APIs. */
@RunWith(JUnit4::class)
@MediumTest(MediumTestAttribute.THREADS)
class FlowControlTest : AbstractCallsTest() {
  @Test
  fun bidiPingPongFlowControl() = runBlocking {
    val channel = makeChannel(ServerCalls.bidiStreamingServerMethodDefinition(
      bufferCapacity = 1,
      scope = this,
      descriptor = bidiStreamingSayHelloMethod,
      implementation = { requests, responses ->
        for (request in requests) {
          responses.send(helloReply("Hello, ${request.name}"))
        }
      }
    ))
    val requests = Channel<HelloRequest>()
    val responses = ClientCalls.bidiStreamingRpc(
      scope = this,
      channel = channel,
      requests = requests,
      method = bidiStreamingSayHelloMethod
    )
    requests.send(helloRequest("Garnet"))
    requests.send(helloRequest("Amethyst"))
    requests.send(helloRequest("Pearl"))
    val fourth = launch { requests.send(helloRequest("Steven")) }
    delay(200) // wait for everything to work its way through the system
    assertThat(fourth.isCompleted).isFalse()
    /*
     * The current state of the system:
     *   - helloReply("Hello, Garnet") is in the client, which is suspended waiting for it to be
     *     received from the responses channel.
     *   - helloReply("Hello, Amethyst") is in the server implementation body, which is suspended
     *     waiting for it to be sent to its responses channel (a rendezvous queue, which will be
     *     unblocked when the client reports it's ready for another element).
     *   - helloRequest("Pearl") is in the server's requests buffer, which is suspended waiting
     *     for the server implementation to receive from its requests channel.
     *   - the job named fourth is suspended, waiting for the client implementation to receive
     *     from that channel -- which will happen after the server requests another message.
     */
    assertThat(responses.receive()).isEqualTo(helloReply("Hello, Garnet"))
    fourth.join() // pulling one element allows the cycle to advance
    responses.cancel()
  }

  @Test
  fun bidiPingPongFlowControlExpandedServerBuffer() = runBlocking {
    val channel = makeChannel(ServerCalls.bidiStreamingServerMethodDefinition(
      bufferCapacity = 2,
      scope = this,
      descriptor = bidiStreamingSayHelloMethod,
      implementation = { requests, responses ->
        for (request in requests) {
          responses.send(helloReply("Hello, ${request.name}"))
        }
      }
    ))
    val requests = Channel<HelloRequest>()
    val responses = ClientCalls.bidiStreamingRpc(
      scope = this,
      channel = channel,
      requests = requests,
      method = bidiStreamingSayHelloMethod
    )
    requests.send(helloRequest("Garnet"))
    requests.send(helloRequest("Amethyst"))
    requests.send(helloRequest("Pearl"))
    requests.send(helloRequest("Steven"))
    val fifth = launch { requests.send(helloRequest("Connie")) }
    delay(200) // wait for everything to work its way through the system
    assertThat(fifth.isCompleted).isFalse()
    /*
     * The current state of the system:
     *   - helloReply("Hello, Garnet") is in the client, which is suspended waiting for it to be
     *     received from the responses channel.
     *   - helloReply("Hello, Amethyst") is in the server implementation body, which is suspended
     *     waiting for it to be sent to its responses channel (a rendezvous queue, which will be
     *     unblocked when the client reports it's ready for another element).
     *   - helloRequest("Pearl") and helloRequest("Steven") are in the server's requests buffer,
     *     which is suspended waiting for the server implementation to receive from its requests
     *     channel.
     *   - the job named fifth is suspended, waiting for the client implementation to receive
     *     from that channel -- which will happen after the server requests another message.
     */
    assertThat(responses.receive()).isEqualTo(helloReply("Hello, Garnet"))
    fifth.join() // pulling one element allows the cycle to advance
    responses.cancel()
  }

  @Test
  fun bidiPingPongFlowControlExpandedClientBuffer() = runBlocking {
    val channel = makeChannel(ServerCalls.bidiStreamingServerMethodDefinition(
      bufferCapacity = 1,
      scope = this,
      descriptor = bidiStreamingSayHelloMethod,
      implementation = { requests, responses ->
        for (request in requests) {
          responses.send(helloReply("Hello, ${request.name}"))
        }
      }
    ))
    val requests = Channel<HelloRequest>()
    val responses = ClientCalls.bidiStreamingRpc(
      scope = this,
      channel = channel,
      requests = requests,
      method = bidiStreamingSayHelloMethod,
      responseChannelSize = 2
    )
    requests.send(helloRequest("Garnet"))
    requests.send(helloRequest("Amethyst"))
    requests.send(helloRequest("Pearl"))
    requests.send(helloRequest("Steven"))
    val fifth = launch { requests.send(helloRequest("Connie")) }
    delay(200) // wait for everything to work its way through the system
    assertThat(fifth.isCompleted).isFalse()
    /*
     * The current state of the system:
     *   - helloReply("Hello, Garnet") and helloReply("Hello, Amethyst") are in the client, which is
     *     suspended waiting for one to be received from the responses channel.
     *   - helloReply("Hello, Pearl") is in the server implementation body, which is suspended
     *     waiting for it to be sent to its responses channel (a rendezvous queue, which will be
     *     unblocked when the client reports it's ready for another element).
     *   - helloRequest("Steven") is in the server's requests buffer,
     *     which is suspended waiting for the server implementation to receive from its requests
     *     channel.
     *   - the job named fifth is suspended, waiting for the client implementation to receive
     *     from that channel -- which will happen after the server requests another message.
     */
    assertThat(responses.receive()).isEqualTo(helloReply("Hello, Garnet"))
    fifth.join() // pulling one element allows the cycle to advance
    responses.cancel()
  }

  @Test
  fun bidiPingPongFlowControlServerDrawsMultipleRequests() = runBlocking {
    val channel = makeChannel(ServerCalls.bidiStreamingServerMethodDefinition(
      bufferCapacity = 1,
      scope = this,
      descriptor = bidiStreamingSayHelloMethod,
      implementation = { requests, responses ->
        val requestItr = requests.iterator()
        while (requestItr.hasNext()) {
          val a = requestItr.next()
          check(requestItr.hasNext())
          val b = requestItr.next()
          responses.send(helloReply("Hello, ${a.name} and ${b.name}"))
        }
      }
    ))
    val requests = Channel<HelloRequest>()
    val responses = ClientCalls.bidiStreamingRpc(
      scope = this,
      channel = channel,
      requests = requests,
      method = bidiStreamingSayHelloMethod
    )
    requests.send(helloRequest("Garnet"))
    requests.send(helloRequest("Amethyst"))
    requests.send(helloRequest("Pearl"))
    requests.send(helloRequest("Steven"))
    requests.send(helloRequest("Connie"))
    val sixth = launch { requests.send(helloRequest("Onion")) }
    delay(300) // wait for everything to work its way through the system
    assertThat(sixth.isCompleted).isFalse()
    /*
     * The current state of the system:
     *   - helloReply("Hello, Garnet and Amethyst") is in the client, which is suspended waiting for
     *     it to be received from the responses channel.
     *   - helloReply("Hello, Pearl and Steven") is in the server implementation body, which is
     *     suspended waiting for it to be sent to its responses channel (a rendezvous queue, which
     *     will be unblocked when the client reports it's ready for another element).
     *   - helloRequest("Connie") is in the server's requests buffer, which is suspended waiting for
     *     the server implementation to receive from its requests channel.
     *   - the job named sixth is suspended, waiting for the client implementation to receive
     *     from that channel -- which will happen after the server requests another message.
     */
    assertThat(responses.receive()).isEqualTo(helloReply("Hello, Garnet and Amethyst"))
    sixth.join() // pulling one element allows the cycle to advance
    responses.cancel()
  }

  @Test
  fun bidiPingPongFlowControlServerSendsMultipleResponses() = runBlocking {
    val channel = makeChannel(ServerCalls.bidiStreamingServerMethodDefinition(
      scope = this,
      descriptor = bidiStreamingSayHelloMethod,
      implementation = { requests, responses ->
        for (request in requests) {
          responses.send(helloReply("Hello, ${request.name}"))
          responses.send(helloReply("Goodbye, ${request.name}"))
        }
      }
    ))
    val requests = Channel<HelloRequest>()
    val responses = ClientCalls.bidiStreamingRpc(
      scope = this,
      channel = channel,
      requests = requests,
      method = bidiStreamingSayHelloMethod
    )
    requests.send(helloRequest("Garnet"))
    requests.send(helloRequest("Amethyst"))
    val third = launch { requests.send(helloRequest("Pearl")) }
    delay(200) // wait for everything to work its way through the system
    assertThat(third.isCompleted).isFalse()
    /*
     * The current state of the system:
     *   - helloReply("Hello, Garnet") is in the client, which is suspended waiting for
     *     it to be received from the responses channel.
     *   - helloReply("Goodbye, Garnet") is in the server implementation body, which is
     *     suspended waiting for it to be sent to its responses channel (a rendezvous queue, which
     *     will be unblocked when the client reports it's ready for another element).
     *   - helloRequest("Amethyst") is in the server's requests buffer, which is suspended waiting
     *     for the server implementation to receive from its requests channel.
     *   - the job named third is suspended, waiting for the client implementation to receive
     *     from that channel -- which will happen after the server requests another message.
     */
    assertThat(responses.receive()).isEqualTo(helloReply("Hello, Garnet"))
    third.join()
    val fourth = launch { requests.send(helloRequest("Steven")) }
    delay(200)
    /*
     * The current state of the system:
     *   - helloReply("Goodbye, Garnet") is in the client, which is suspended waiting for
     *     it to be received from the responses channel.
     *   - helloReply("Hello, Amethyst") is in the server implementation body, which is
     *     suspended waiting for it to be sent to its responses channel (a rendezvous queue, which
     *     will be unblocked when the client reports it's ready for another element).
     *   - helloRequest("Hello, Pearl") is in the server's request buffer, which is suspended
     *     waiting for the server implementation to receive from its requests channel
     *   - the job named fourth is suspended, waiting for the client implementation to receive
     *     from that channel
     */
    assertThat(fourth.isCompleted).isFalse()
    assertThat(responses.receive()).isEqualTo(helloReply("Goodbye, Garnet"))
    delay(200)
    /*
     * The current state of the system:
     *   - helloReply("Hello, Amethyst") is in the client, which is suspended waiting for
     *     it to be received from the responses channel.
     *   - helloReply("Goodbye, Amethyst") is in the server implementation body, which is
     *     suspended waiting for it to be sent to its responses channel (a rendezvous queue, which
     *     will be unblocked when the client reports it's ready for another element).
     *   - helloRequest("Hello, Pearl") is in the server's request buffer, which is suspended
     *     waiting for the server implementation to receive from its requests channel
     *   - the job named fourth is suspended, waiting for the client implementation to receive
     *     from that channel
     */
    assertThat(fourth.isCompleted).isFalse()
    responses.cancel()
  }
}