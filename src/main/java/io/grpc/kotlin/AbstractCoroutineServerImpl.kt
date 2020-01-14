package io.grpc.kotlin

import io.grpc.BindableService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Skeleton implementation of a coroutine-based gRPC server implementation.  Intended to be
 * subclassed by generated code.
 */
abstract class AbstractCoroutineServerImpl private constructor(
  private val delegateScope: CoroutineScope
) : CoroutineScope, BindableService {

  constructor(coroutineContext: CoroutineContext = EmptyCoroutineContext) :
    this(CoroutineScope(coroutineContext + SupervisorJob(coroutineContext[Job])))
  // We want a SupervisorJob so one failed RPC does not bring down the entire server.

  final override val coroutineContext: CoroutineContext
    get() = delegateScope.coroutineContext
}