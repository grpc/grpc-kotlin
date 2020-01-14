package io.grpc.kotlin

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Extracts the value of a [Deferred] known to be completed, or throws its exception if it was
 * not completed successfully.  (`getDone` is equivalent, but experimental.)
 */
val <T> Deferred<T>.doneValue: T
  get() {
    check(isCompleted) { "doneValue should only be called on completed Deferred values" }
    return runBlocking {
      await()
    }
  }

/**
 * Copies data from the input to the output, including closure state.  After an element is
 * successfully sent to the output, calls `onReadyForMore`.
 */
private suspend fun <T> copyFromBuffer(
  input: ReceiveChannel<T>,
  output: SendChannel<T>,
  onReadyForMore: () -> Unit
) {
  val bufferIterator = input.iterator()
  var keepGoing = true
  var exception: Throwable? = null
  while (keepGoing) {
    keepGoing = try {
      bufferIterator.hasNext()
    } catch (t: Throwable) {
      exception = t
      false
    }
    if (keepGoing) {
      output.send(bufferIterator.next())
      onReadyForMore()
    }
  }
  output.close(exception)
}

/**
 * Creates a SendChannel/ReceiveChannel pair such that sending to the SendChannel never suspends
 * if it is used in a strict alternating sequence of send/wait for requestMore/send/wait for
 * requestMore..., and the ReceiveChannel receives elements from the SendChannel.
 */
fun <T> CoroutineScope.bufferedChannel(
  bufferCapacity: Int,
  requestMore: () -> Unit,
  onCancel: () -> Unit
): Pair<SendChannel<T>, ReceiveChannel<T>> {
  require(bufferCapacity > 0) { "bufferCapacity must be positive but was $bufferCapacity" }
  val buffer = Channel<T>(bufferCapacity)
  val output = Channel<T>(Channel.RENDEZVOUS)
  val bufferJob = launch {
    copyFromBuffer(buffer, output, requestMore)
  }
  bufferJob.invokeOnCompletion { t ->
    if (bufferJob.isCancelled) {
      val cause = CancellationException("Job running bufferedChannel was cancelled", t)
      buffer.cancel(cause)
      output.cancel(cause)
      onCancel()
    }
  }
  val receiveChannel = object : ReceiveChannel<T> by output {
    override fun cancel(cause: CancellationException?) {
      bufferJob.cancel(cause)
    }
  }
  return Pair(buffer as SendChannel<T>, receiveChannel)
}
