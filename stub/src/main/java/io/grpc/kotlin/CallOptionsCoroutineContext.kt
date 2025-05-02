package io.grpc.kotlin

import io.grpc.CallOptions
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private val COROUTINE_CONTEXT_OPTION: CallOptions.Key<CoroutineContext> =
    CallOptions.Key.createWithDefault("Coroutine context", EmptyCoroutineContext)

/**
 * Sets a coroutine context.
 *
 * @param context coroutine context to put into the call options
 * @return [CallOptions] instance with coroutine context
 */
fun CallOptions.withCoroutineContext(context: CoroutineContext): CallOptions =
    withOption(COROUTINE_CONTEXT_OPTION, context)

/**
 * Gets a coroutine context from the call options.
 *
 * Default: [EmptyCoroutineContext]
 */
val CallOptions.coroutineContext: CoroutineContext
    get() = getOption(COROUTINE_CONTEXT_OPTION)
