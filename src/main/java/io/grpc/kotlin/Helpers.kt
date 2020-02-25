package io.grpc.kotlin

import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

/**
 * Extracts the value of a [Deferred] known to be completed, or throws its exception if it was
 * not completed successfully.  (Non-experimental variant of `getDone`.)
 */
val <T> Deferred<T>.doneValue: T
  get() {
    check(isCompleted) { "doneValue should only be called on completed Deferred values" }
    return runBlocking(Dispatchers.Unconfined) {
      await()
    }
  }

/**
 * Cancels a [Job] with a cause and suspends until the job completes/is finished cancelling.
 */
suspend fun Job.cancelAndJoin(message: String, cause: Exception? = null) {
  cancel(message, cause)
  join()
}

suspend fun <T> FlowCollector<T>.emitAll(flow: Flow<T>) {
  flow.collect { emit(it) }
}

/**
 * Extracts the one and only element of this flow, throwing an appropriate [StatusException] if
 * there is not exactly one element.  (Otherwise this is fully equivalent to `Flow.single()`.)
 */
suspend fun <T> Flow<T>.singleOrStatus(
  expected: String,
  descriptor: Any
): T {
  // We could call Flow.single() and catch exceptions, but if the underlying flow throws
  // IllegalStateException or NoSuchElementException for its own reasons, we'd swallow those
  // and give misleading errors.  Instead, we reimplement single() ourselves.
  var result: T? = null
  var found = false
  collect {
    if (!found) {
      found = true
      result = it
    } else {
      throw StatusException(
        Status.INTERNAL.withDescription("Expected one $expected for $descriptor but received two")
      )
    }
  }
  @Suppress("UNCHECKED_CAST")
  if (!found) {
    throw StatusException(
      Status.INTERNAL.withDescription("Expected one $expected for $descriptor but received none")
    )
  } else {
    return result as T
  }
}

/** Runs [block] and returns any exception it throws, or `null` if it does not throw. */
inline fun thrownOrNull(block: () -> Unit): Throwable? = try {
  block()
  null
} catch (thrown: Throwable) {
  thrown
}