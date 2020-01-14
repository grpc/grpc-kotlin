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

import io.grpc.CallOptions
import io.grpc.ClientCall
import io.grpc.MethodDescriptor
import io.grpc.Status
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import io.grpc.Channel as GrpcChannel
import io.grpc.Metadata as GrpcMetadata

/**
 * Helpers for gRPC clients implemented in Kotlin.  Can be used directly, but intended to be used
 * from generated Kotlin APIs.
 */
object ClientCalls {
  private const val DEFAULT_RESPONSE_CHANNEL_SIZE = 1

  /**
   * Launches a unary RPC on the specified channel, suspending until the result is received.
   */
  suspend fun <RequestT, ResponseT> unaryRpc(
    scope: CoroutineScope,
    channel: GrpcChannel,
    method: MethodDescriptor<RequestT, ResponseT>,
    request: RequestT,
    callOptions: CallOptions = CallOptions.DEFAULT,
    headers: GrpcMetadata = GrpcMetadata()
  ): ResponseT {
    require(method.type == MethodDescriptor.MethodType.UNARY) {
      "Expected a unary RPC method, but got $method"
    }
    return scope.rpcImpl(
      channel = channel,
      method = method,
      callOptions = callOptions,
      headers = headers,
      request = Request.Unary(request)
    ).unaryResponse()
  }

  /**
   * Returns a function object representing a unary RPC.
   *
   * The input headers may be asynchronously formed. [headers] will be called each time the returned
   * RPC is called - the headers are *not* cached.
   */
  fun <RequestT, ResponseT> unaryRpcFunction(
    channel: GrpcChannel,
    method: MethodDescriptor<RequestT, ResponseT>,
    callOptions: CallOptions = CallOptions.DEFAULT,
    headers: suspend () -> GrpcMetadata = { GrpcMetadata() }
  ): suspend (RequestT) -> ResponseT =
    { unaryRpc(CoroutineScope(coroutineContext), channel, method, it, callOptions, headers()) }

  /**
   * Launches a server-streaming RPC on the specified channel, returning a [ReceiveChannel] for the
   * server's responses.
   */
  fun <RequestT, ResponseT> serverStreamingRpc(
    scope: CoroutineScope,
    channel: GrpcChannel,
    method: MethodDescriptor<RequestT, ResponseT>,
    request: RequestT,
    callOptions: CallOptions = CallOptions.DEFAULT,
    headers: GrpcMetadata = GrpcMetadata(),
    responseChannelSize: Int = DEFAULT_RESPONSE_CHANNEL_SIZE
  ): ReceiveChannel<ResponseT> {
    require(method.type == MethodDescriptor.MethodType.SERVER_STREAMING) {
      "Expected a server streaming RPC method, but got $method"
    }
    return scope.rpcImpl(
      channel = channel,
      method = method,
      callOptions = callOptions,
      headers = headers,
      responseChannelSize = responseChannelSize,
      request = Request.Unary(request)
    )
  }

  /**
   * Returns a function object representing a server streaming RPC.
   *
   * The input headers may be asynchronously formed. [headers] will be called each time the returned
   * RPC is called - the headers are *not* cached.
   */
  fun <RequestT, ResponseT> serverStreamingRpcFunction(
    channel: GrpcChannel,
    method: MethodDescriptor<RequestT, ResponseT>,
    callOptions: CallOptions = CallOptions.DEFAULT,
    headers: suspend () -> GrpcMetadata = { GrpcMetadata() },
    responseChannelSize: Int = DEFAULT_RESPONSE_CHANNEL_SIZE
  ): suspend (RequestT) -> ReceiveChannel<ResponseT> =
    {
      serverStreamingRpc(
        CoroutineScope(coroutineContext),
        channel,
        method,
        it,
        callOptions,
        headers(),
        responseChannelSize
      )
    }

  /**
   * Launches a client-streaming RPC on the specified channel, suspending until the server returns
   * the result.  The caller is expected to provide a [ReceiveChannel] of requests, to populate it,
   * and to close it when done.
   *
   * If the server responds before the `requests` channel is closed, the `requests` channel may be
   * [cancelled][ReceiveChannel.cancel].
   */
  suspend fun <RequestT, ResponseT> clientStreamingRpc(
    scope: CoroutineScope,
    channel: GrpcChannel,
    method: MethodDescriptor<RequestT, ResponseT>,
    requests: ReceiveChannel<RequestT>,
    callOptions: CallOptions = CallOptions.DEFAULT,
    headers: GrpcMetadata = GrpcMetadata()
  ): ResponseT {
    require(method.type == MethodDescriptor.MethodType.CLIENT_STREAMING) {
      "Expected a server streaming RPC method, but got $method"
    }
    return scope
      .rpcImpl(
        channel = channel,
        method = method,
        callOptions = callOptions,
        headers = headers,
        request = Request.Streaming(requests)
      ).unaryResponse()
  }

  /**
   * Returns a function object representing a client streaming RPC.
   *
   * The input headers may be asynchronously formed. [headers] will be called each time the returned
   * RPC is called - the headers are *not* cached.
   */
  fun <RequestT, ResponseT> clientStreamingRpcFunction(
    channel: GrpcChannel,
    method: MethodDescriptor<RequestT, ResponseT>,
    callOptions: CallOptions = CallOptions.DEFAULT,
    headers: suspend () -> GrpcMetadata = { GrpcMetadata() }
  ): suspend (ReceiveChannel<RequestT>) -> ResponseT =
    {
      clientStreamingRpc(
        CoroutineScope(coroutineContext),
        channel,
        method,
        it,
        callOptions,
        headers()
      )
    }

  /**
   * Issues a bidirectional streaming RPC on the specified channel, returning a [ReceiveChannel]
   * for the server's responses.  The caller is expected to provide a [ReceiveChannel] of requests,
   * to populate it, and to close it when done.
   *
   * If the server responds before the `requests` channel is closed, the `requests` channel may be
   * [cancelled][ReceiveChannel.cancel].
   */
  fun <RequestT, ResponseT> bidiStreamingRpc(
    scope: CoroutineScope,
    channel: GrpcChannel,
    method: MethodDescriptor<RequestT, ResponseT>,
    requests: ReceiveChannel<RequestT>,
    callOptions: CallOptions = CallOptions.DEFAULT,
    headers: GrpcMetadata = GrpcMetadata(),
    responseChannelSize: Int = DEFAULT_RESPONSE_CHANNEL_SIZE
  ): ReceiveChannel<ResponseT> {
    check(method.type == MethodDescriptor.MethodType.BIDI_STREAMING) {
      "Expected a bidi streaming method, but got $method"
    }
    return scope.rpcImpl(
      channel = channel,
      method = method,
      callOptions = callOptions,
      headers = headers,
      request = Request.Streaming(requests),
      responseChannelSize = responseChannelSize
    )
  }

  /**
   * Returns a function object representing a bidirectional streaming RPC.
   *
   * The input headers may be asynchronously formed. [headers] will be called each time the returned
   * RPC is called - the headers are *not* cached.
   */
  fun <RequestT, ResponseT> bidiStreamingRpcFunction(
    channel: GrpcChannel,
    method: MethodDescriptor<RequestT, ResponseT>,
    callOptions: CallOptions = CallOptions.DEFAULT,
    headers: suspend () -> GrpcMetadata = { GrpcMetadata() },
    responseChannelSize: Int = DEFAULT_RESPONSE_CHANNEL_SIZE
  ): suspend (ReceiveChannel<RequestT>) -> ReceiveChannel<ResponseT> =
    {
      bidiStreamingRpc(
        CoroutineScope(coroutineContext),
        channel,
        method,
        it,
        callOptions,
        headers(),
        responseChannelSize
      )
    }

  /** The client's request(s). */
  private sealed class Request<RequestT> {
    /**
     * Send the request(s) to the ClientCall, with `readiness` indicating calls to `onReady` from
     * the listener.  Returns when sending the requests is done, either because all the requests
     * were sent (in which case `null` is returned) or because the requests channel was closed
     * with an exception (in which case the exception is returned).
     */
    abstract suspend fun sendTo(
      clientCall: ClientCall<RequestT, *>,
      readiness: Readiness
    ): Exception?

    /** Cancels the sending of any further requests. */
    abstract fun cancel(ex: CancellationException?)

    class Unary<RequestT>(private val value: RequestT): Request<RequestT>() {
      override suspend fun sendTo(
        clientCall: ClientCall<RequestT, *>,
        readiness: Readiness
      ): Exception? {
        clientCall.sendMessage(value)
        clientCall.halfClose()
        return null
      }

      override fun cancel(ex: CancellationException?) {
        // do nothing
      }
    }

    class Streaming<RequestT>(private val channel: ReceiveChannel<RequestT>): Request<RequestT>() {
      override suspend fun sendTo(
        clientCall: ClientCall<RequestT, *>,
        readiness: Readiness
      ): Exception? {
        val requestsItr = channel.iterator()
        while (true) {
          // This loop makes sure that we only receive from channel when the server is actually
          // ready for a message without buffering.
          readiness.suspendUntilReady()
          while (clientCall.isReady) {
            val hasNext = try {
              requestsItr.hasNext()
            } catch (ex: Exception) {
              return ex
            }
            if (hasNext) {
              clientCall.sendMessage(requestsItr.next())
            } else {
              clientCall.halfClose()
              return null
            }
          }
        }
      }

      override fun cancel(ex: CancellationException?) {
        channel.cancel(ex)
      }
    }
  }

  /**
   * Retrieves the one and only element of the channel, verifying it has exactly one element.
   * If cancelled, cancels the backing channel.
   */
  private suspend fun <ResponseT> ReceiveChannel<ResponseT>.unaryResponse() : ResponseT = try {
    val iterator = iterator()
    check(iterator.hasNext()) { "Expected exactly one response but got none" }
    val response = iterator.next()
    check(!iterator.hasNext()) {
      "Expected exactly one response but received $response and ${iterator.next()}"
    }
    response
  } catch (e: Throwable) {
    this@unaryResponse.cancel(
      e as? CancellationException ?: CancellationException("Unary response failure", e)
    )
    throw e
  }

  /**
   * Issues an RPC of any type, returning a [ReceiveChannel] for the responses.  This is intended
   * to be the root implementation for all Kotlin coroutine-based RPCs, with non-streaming
   * implementations simply sending or receiving a single message on the appropriate channel.
   */
  private fun <RequestT, ResponseT> CoroutineScope.rpcImpl(
    channel: GrpcChannel,
    method: MethodDescriptor<RequestT, ResponseT>,
    callOptions: CallOptions,
    headers: GrpcMetadata,
    request: Request<RequestT>,
    responseChannelSize: Int = DEFAULT_RESPONSE_CHANNEL_SIZE
  ): ReceiveChannel<ResponseT> {

    val clientCall: ClientCall<RequestT, ResponseT> =
      channel.newCall<RequestT, ResponseT>(method, callOptions)

    require(responseChannelSize >= 0) {
      "Expected responseChannelSize to be positive but was $responseChannelSize"
    }
    require(responseChannelSize != Channel.RENDEZVOUS) {
      "The return buffer must have room for at least one element without suspending or blocking"
    }

    /*
     * Let
     *
     * K = responseChannelSize
     * R = (sum of arguments passed to clientCall.request) - (# of times onMessage has been called)
     * S = 1 if the buffer coroutine is suspended waiting on responsesOutput.send, 0 otherwise
     * N = number of elements in responsesInput
     *
     * The Channel API guarantees that N <= K.  The gRPC API guarantees that R >= 0.
     *
     * Claim: the invariant N + R + S <= K always holds.  Proof by induction.
     *
     * Base case: when this method first returns, R = K, S = 0, N = 0.
     *
     * State transitions:
     * - onMessage is called.  R goes down by 1, and R is never negative, so R must have been >= 1.
     *   Therefore before onMessage was called, N + S <= K - 1.  In particular, N <= K - 1, so
     *   the implementation of onMessage -- which offers an element to requestInput -- succeeds,
     *   and N goes up by one.  Therefore N + R + S stays the same, since R decreased by 1 and N
     *   increased by 1.
     * - The buffering coroutine receives an element from requestInput and suspends until it can be
     *   sent to requestOutput.  This can only happen if N >= 1.  N goes down by 1 and S goes up
     *   from 0 to 1, so N + R + S stays the same.
     * - The buffering coroutine successfully sends an element to requestOutput and resumes. (Since
     *   requestOutput is a rendezvous queue, this happens-after the user implementation receives
     *   the request from requestOutput).  The buffering coroutine then calls the requestMore
     *   lambda, which is a call to call.request(1).  This decreases S from 1 to 0 and increases
     *   R by 1, so N + R + S stays the same.
     *
     * Note that in each case, the decrease happens-before the corresponding increase, so even at
     * intermediate steps N + R + S <= K.
     *
     * None of the methods of ClientCall.Listener block.  They may end this loop:
     * - onCancel and onHalfClose guarantee that onMessage will never be called again
     * - onReady does not interact with this loop, instead just potentially informing the
     *   *output* coroutine that it should proceed to send
     *
     * Notably, for this "pump" to work, K must be at least 1.
     */
    val (responsesInput, responsesOutput) = bufferedChannel<ResponseT>(
      bufferCapacity = responseChannelSize,
      onCancel = { /* do nothing */ },
      requestMore = { clientCall.request(1) }
    )

    val completionException = CompletableDeferred<Exception?>(coroutineContext[Job])

    val readiness = Readiness()

    clientCall.start(
      object : ClientCall.Listener<ResponseT>() {
        override fun onMessage(message: ResponseT) {
          if (!responsesInput.offer(message)) {
            throw AssertionError("onMessage should never be called until responsesInput is ready")
          }
        }

        override fun onClose(status: Status, trailersMetadata: GrpcMetadata) {
          completionException.complete(
            if (status.isOk) null else status.asException(trailersMetadata)
          )
        }

        override fun onReady() {
          readiness.onReady()
        }
      },
      headers
    )
    clientCall.request(responseChannelSize)

    val methodName = method.fullMethodName
    val sendMessageWorker = launch(CoroutineName("SendMessage worker for $methodName")) {
      val ex = request.sendTo(clientCall, readiness)
      if (ex != null && completionException.complete(ex)) {
        clientCall.cancel(null, ex)
      }
    }

    completionException.invokeOnCompletion {
      val ex = it ?: completionException.doneValue
      responsesInput.close(ex)
      val cancelEx = ex?.let { CancellationException("RPC shut down", it) }
      sendMessageWorker.cancel(cancelEx)
      request.cancel(cancelEx)
    }

    return object : ReceiveChannel<ResponseT> by responsesOutput {
      override fun cancel(cause: CancellationException?) {
        responsesOutput.cancel(cause)
        completionException.complete(cause)
        clientCall.cancel(null, cause)
      }
    }
  }
}
