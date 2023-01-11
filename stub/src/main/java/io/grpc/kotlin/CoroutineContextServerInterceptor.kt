package io.grpc.kotlin

import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.StatusException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import io.grpc.Context as GrpcContext

/**
 * A [ServerInterceptor] subtype that can install elements in the [CoroutineContext] where server
 * logic is executed.  These elements are applied "after" the
 * [AbstractCoroutineServerImpl.context]; that is, the interceptor overrides the server's context.
 */
abstract class CoroutineContextServerInterceptor : ServerInterceptor {
  companion object {
    // This is deliberately kept visibility-restricted; it's intentional that the only way to affect
    // the CoroutineContext is to extend CoroutineContextServerInterceptor.
    internal val COROUTINE_CONTEXT_KEY : GrpcContext.Key<CoroutineContext> =
      GrpcContext.keyWithDefault("grpc-kotlin-coroutine-context", EmptyCoroutineContext)

    private fun GrpcContext.extendCoroutineContext(coroutineContext: CoroutineContext): GrpcContext {
      val oldCoroutineContext: CoroutineContext = COROUTINE_CONTEXT_KEY[this]
      val newCoroutineContext = oldCoroutineContext + coroutineContext
      return withValue(COROUTINE_CONTEXT_KEY, newCoroutineContext)
    }
  }

  /**
   * Override this function to return a [CoroutineContext] in which to execute [call] and [headers].
   * The returned [CoroutineContext] will override any corresponding context elements in the
   * server object.
   *
   * This function will be called each time a [call] is executed.
   *
   * @throws StatusException if the call should be closed with the [Status][io.grpc.Status] in the
   *     exception and further processing suppressed
   */
  abstract fun coroutineContext(call: ServerCall<*, *>, headers: Metadata): CoroutineContext

  /**
   * Override this function to insert a forwarding server call.
   */
  open fun <ReqT, RespT> forward(call: ServerCall<ReqT, RespT>) :ServerCall<ReqT, RespT> {
    return call
  }

  private inline fun <R> withGrpcContext(context: GrpcContext, action: () -> R): R {
    val oldContext: GrpcContext = context.attach()
    return try {
      action()
    } finally {
      context.detach(oldContext)
    }
  }

  final override fun <ReqT, RespT> interceptCall(
    call: ServerCall<ReqT, RespT>,
    headers: Metadata,
    next: ServerCallHandler<ReqT, RespT>
  ): ServerCall.Listener<ReqT> {
    val coroutineContext = try {
      coroutineContext(call, headers)
    } catch (e: StatusException) {
      call.close(e.status, e.trailers ?: Metadata())
      throw e
    }
    return withGrpcContext(GrpcContext.current().extendCoroutineContext(coroutineContext)) {
      next.startCall(forward(call), headers)
    }
  }
}