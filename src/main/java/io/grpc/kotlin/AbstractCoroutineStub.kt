package io.grpc.kotlin

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.stub.AbstractStub
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * A skeleton implementation of a coroutine-based client stub, suitable for extension by generated
 * client stubs.
 */
abstract class AbstractCoroutineStub<S: AbstractCoroutineStub<S>>(
  channel: Channel,
  val coroutineContext: CoroutineContext = EmptyCoroutineContext,
  callOptions: CallOptions = CallOptions.DEFAULT
): AbstractStub<S>(channel, callOptions) {
  final override fun build(channel: Channel, callOptions: CallOptions): S =
    build(channel, this.coroutineContext, callOptions)

  abstract fun build(
    channel: Channel,
    coroutineContext: CoroutineContext,
    callOptions: CallOptions
  ): S

  fun addCoroutineContext(context: CoroutineContext): S =
    build(channel, this.coroutineContext + context, callOptions)
}
