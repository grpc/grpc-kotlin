package io.grpc.kotlin

import io.grpc.MethodDescriptor
import io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING
import io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING
import io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING
import io.grpc.MethodDescriptor.MethodType.UNARY
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerMethodDefinition
import io.grpc.Status
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import io.grpc.Metadata as GrpcMetadata

/**
 * Helpers for implementing a gRPC server based on a Kotlin coroutine implementation.
 */
object ServerCalls {
  /**
   * Creates a [ServerMethodDefinition] that implements the specified unary RPC method by running
   * the specified implementation and associated implementation details within the specified
   * [CoroutineScope] (and/or a subscope).
   */
  fun <RequestT, ResponseT> unaryServerMethodDefinition(
    scope: CoroutineScope,
    descriptor: MethodDescriptor<RequestT, ResponseT>,
    bufferCapacity: Int = 1,
    implementation: suspend (RequestT) -> ResponseT
  ): ServerMethodDefinition<RequestT, ResponseT> {
    require(descriptor.type == UNARY) {
      "Expected a unary method descriptor but got $descriptor"
    }
    return serverMethodDefinition(scope, descriptor, bufferCapacity) { requests, responses ->
      unaryRequestImpl(descriptor, requests) { responses.send(implementation(it)) }
    }
  }

  /**
   * Creates a [ServerMethodDefinition] that implements the specified client-streaming RPC method by
   * running the specified implementation and associated implementation details within the specified
   * [CoroutineScope] (and/or a subscope).
   */
  fun <RequestT, ResponseT> clientStreamingServerMethodDefinition(
    scope: CoroutineScope,
    descriptor: MethodDescriptor<RequestT, ResponseT>,
    bufferCapacity: Int = 1,
    implementation: suspend (ReceiveChannel<RequestT>) -> ResponseT
  ): ServerMethodDefinition<RequestT, ResponseT> {
    require(descriptor.type == CLIENT_STREAMING) {
      "Expected a client streaming method descriptor but got $descriptor"
    }
    return serverMethodDefinition(scope, descriptor, bufferCapacity) { requests, responses ->
      responses.send(implementation(requests))
      Status.OK
    }
  }

  /**
   * Creates a [ServerMethodDefinition] that implements the specified server-streaming RPC method by
   * running the specified implementation and associated implementation details within the specified
   * [CoroutineScope] (and/or a subscope).
   */
  fun <RequestT, ResponseT> serverStreamingServerMethodDefinition(
    scope: CoroutineScope,
    descriptor: MethodDescriptor<RequestT, ResponseT>,
    bufferCapacity: Int = 1,
    implementation: suspend (RequestT, SendChannel<ResponseT>) -> Unit
  ): ServerMethodDefinition<RequestT, ResponseT> {
    require(descriptor.type == SERVER_STREAMING) {
      "Expected a server streaming method descriptor but got $descriptor"
    }
    return serverMethodDefinition(scope, descriptor, bufferCapacity) { requests, responses ->
      unaryRequestImpl(descriptor, requests) { implementation(it, responses) }
    }
  }

  /**
   * Creates a [ServerMethodDefinition] that implements the specified bidirectional-streaming RPC
   * method by running the specified implementation and associated implementation details within the
   * specified [CoroutineScope] (and/or a subscope).
   */
  fun <RequestT, ResponseT> bidiStreamingServerMethodDefinition(
    scope: CoroutineScope,
    descriptor: MethodDescriptor<RequestT, ResponseT>,
    bufferCapacity: Int = 1,
    implementation: suspend (ReceiveChannel<RequestT>, SendChannel<ResponseT>) -> Unit
  ): ServerMethodDefinition<RequestT, ResponseT> {
    require(descriptor.type == BIDI_STREAMING) {
      "Expected a bidi streaming method descriptor but got $descriptor"
    }
    return serverMethodDefinition(scope, descriptor, bufferCapacity) { requests, responses ->
      implementation(requests, responses)
      Status.OK
    }
  }

  /**
   * Builds a [ServerMethodDefinition] that implements the specified RPC method by running the
   * specified channel-based implementation within the specified [CoroutineScope] (and/or a
   * subscope).
   */
  private fun <RequestT, ResponseT> serverMethodDefinition(
    scope: CoroutineScope,
    descriptor: MethodDescriptor<RequestT, ResponseT>,
    bufferCapacity: Int,
    implementation:
      suspend (ReceiveChannel<RequestT>, SendChannel<ResponseT>) -> Status
  ): ServerMethodDefinition<RequestT, ResponseT> =
    ServerMethodDefinition.create(
      descriptor,
      serverCallHandler(scope, descriptor.fullMethodName, bufferCapacity, implementation)
    )

  /**
   * Returns a [ServerCallHandler] that implements an RPC method by running the specified
   * channel-based implementation within the specified [CoroutineScope] (and/or a subscope).
   */
  private fun <RequestT, ResponseT> serverCallHandler(
    scope: CoroutineScope,
    rpcName: String,
    bufferCapacity: Int,
    implementation:
      suspend (ReceiveChannel<RequestT>, SendChannel<ResponseT>) -> Status
  ): ServerCallHandler<RequestT, ResponseT> =
    ServerCallHandler {
      call, _ ->
      serverCallListener(
        scope + GrpcContextElement.current(),
        call,
        rpcName,
        bufferCapacity,
        implementation
      )
    }

  private fun Status.withMaybeCause(cause: Throwable?): Status =
    if (cause == null) this else this.withCause(cause)

  private fun <RequestT, ResponseT> serverCallListener(
    scope: CoroutineScope,
    call: ServerCall<RequestT, ResponseT>,
    rpcName: String,
    bufferCapacity: Int,
    implementation:
      suspend (ReceiveChannel<RequestT>, SendChannel<ResponseT>) -> Status
  ): ServerCall.Listener<RequestT> = with (scope) {

    /*
     * Let
     *
     * K = bufferCapacity
     * R = (sum of arguments passed to call.request) - (# of times onMessage has been called)
     * S = 1 if the buffer coroutine is suspended waiting on requestOutput.send, 0 otherwise
     * N = number of elements in requestInput
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
     * None of the methods of ServerCall.Listener block.  They may end this loop:
     * - onCancel and onHalfClose guarantee that onMessage will never be called again
     * - onReady does not interact with this loop, instead just potentially informing the
     *   *output* coroutine that it should proceed to send
     *
     * (If requestOutput is cancelled, R goes up to ~infinity, but that happens-after the channels
     * are cancelled.)
     */

    val (requestInput, requestOutput) = bufferedChannel<RequestT>(
      bufferCapacity = bufferCapacity,
      requestMore = { call.request(1) },
      onCancel = {
        // we'll never read any more requests, but the client shouldn't experience backpressure
        // from attempting to send them
        call.request(Int.MAX_VALUE)
      }
    )

    val responseChannel = Channel<ResponseT>()

    val completionStatus = CompletableDeferred<Status>(coroutineContext[Job])

    // NB: the Java API defers the sending of the headers to just before the first response message.
    // The docs specify that either implementation is valid.  Still, something to keep an eye on.
    call.sendHeaders(GrpcMetadata())
    call.request(bufferCapacity)
    val readiness = Readiness()

    val workers = launch {
      launch(CoroutineName("Implementation job for $rpcName server")) {
        completionStatus.complete(
          try {
            implementation(requestOutput, responseChannel)
          } catch (c: CancellationException) {
            Status.CANCELLED.withCause(c)
          } catch (e: Throwable) {
            Status.fromThrowable(e)
          } finally {
            requestOutput.cancel()
            responseChannel.close()
          }
        )
      }

      launch(CoroutineName("SendResponses job for $rpcName server")) {
        var keepGoing = true
        val responsesItr = responseChannel.iterator()
        while (keepGoing) {
          readiness.suspendUntilReady()
          if (completionStatus.isCompleted) {
            keepGoing = false
          } else {
            while (keepGoing && call.isReady) {
              keepGoing = try {
                responsesItr.hasNext()
                // suspends until we have a response to send, or responses is closed
                // (either successfully, or with a failure throwable)
              } catch (e: Exception) {
                completionStatus.complete(Status.fromThrowable(e))
                false
              }
              if (keepGoing) {
                call.sendMessage(responsesItr.next())
              }
            }
          }
        }
      }
    }

    completionStatus.invokeOnCompletion {
      val closeStatus: Status = if (completionStatus.isCancelled) {
        Status.CANCELLED.withMaybeCause(it)
      } else {
        completionStatus.doneValue
      }
      if (!closeStatus.isOk) {
        workers.cancel()
      }
      requestOutput.cancel()
      workers.invokeOnCompletion {
        call.close(closeStatus, GrpcMetadata())
      }
    }

    object: ServerCall.Listener<RequestT>() {
      var isReceiving = true

      override fun onCancel() {
        completionStatus.complete(Status.CANCELLED)
      }

      override fun onMessage(message: RequestT) {
        if (!isReceiving) {
          return
        }
        try {
          if (!requestInput.offer(message)) {
            throw AssertionError(
              "onMessage should never be called when requestInput is unready"
            )
          }
        } catch (e: CancellationException) {
          // we don't want any more client input; swallow it
          isReceiving = false
        }
      }

      override fun onHalfClose() {
        requestInput.close()
      }

      override fun onReady() {
        readiness.onReady()
      }
    }
  }

  /** Helper that extracts one request and verifies that there is only one request. */
  private suspend fun <RequestT, ResponseT> unaryRequestImpl(
    descriptor: MethodDescriptor<RequestT, ResponseT>,
    requests: ReceiveChannel<RequestT>,
    implementation: suspend (RequestT) -> Unit
  ): Status {
    val requestItr = requests.iterator()
    if (!requestItr.hasNext()) {
      return Status.INTERNAL
        .withDescription(
          "Expected one request for $descriptor but received none"
        )
    }
    val request = requestItr.next()
    implementation(request)
    return if (requestItr.hasNext()) {
      Status.INTERNAL
        .withDescription(
          "Expected one request for method $descriptor but received two"
        )
    } else {
      Status.OK
    }
  }
}
