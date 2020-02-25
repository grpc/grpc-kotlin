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
  callOptions: CallOptions = CallOptions.DEFAULT
): AbstractStub<S>(channel, callOptions)
