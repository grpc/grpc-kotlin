package io.grpc.kotlin

import kotlinx.coroutines.CoroutineScope
import io.grpc.Context as GrpcContext
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.withContext

/**
 * A [CoroutineContext] that propagates an associated [io.grpc.Context] to coroutines run using
 * that context, regardless of thread.
 */
class GrpcContextElement(private val grpcContext: GrpcContext) : ThreadContextElement<GrpcContext> {
  companion object Key : CoroutineContext.Key<GrpcContextElement> {
    fun current(): GrpcContextElement = GrpcContextElement(GrpcContext.current())
  }

  override val key: CoroutineContext.Key<GrpcContextElement>
    get() = Key

  override fun restoreThreadContext(context: CoroutineContext, oldState: GrpcContext) {
    grpcContext.detach(oldState)
  }

  override fun updateThreadContext(context: CoroutineContext): GrpcContext {
    return grpcContext.attach()
  }
}

/**
 * Runs a coroutine with `this` as the [current][GrpcContext.current] gRPC context, suspends until
 * it completes, and returns the result.
 */
suspend fun <T> GrpcContext.runCoroutine(block: suspend CoroutineScope.() -> T): T =
  withContext(GrpcContextElement(this), block)