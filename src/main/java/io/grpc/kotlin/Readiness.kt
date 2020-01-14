package io.grpc.kotlin

import kotlinx.coroutines.channels.Channel

/**
 * A simple helper allowing a notification of "ready" to be broadcast, and waited for.
 */
class Readiness {
  // A CONFLATED channel never suspends to send, and two notifications of readiness are equivalent
  // to one
  private val channel = Channel<Unit>(Channel.CONFLATED)

  fun onReady() {
    if (!channel.offer(Unit)) {
      throw AssertionError(
        "Should be impossible; a CONFLATED channel should never return false on offer"
      )
    }
  }

  suspend fun suspendUntilReady(): Unit = channel.receive()
}