package io.grpc.kotlin

import io.grpc.Metadata
import io.grpc.ServerCall
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * A server interceptor which propagates gRPC [Metadata] (HTTP Headers) to coroutineContext. To use
 * it attach the interceptor to gRPC Server and then access the [Metadata] using grpcMetadata()
 * function.
 *
 * Example usage:
 *
 * ServerBuilder.forPort(8060).addService(GreeterImpl())
 * .intercept(MetadataCoroutineContextInterceptor())
 *
 * Then in RPC implementation code call grpcMetadata()
 */
class MetadataCoroutineContextInterceptor : CoroutineContextServerInterceptor() {
  final override fun coroutineContext(call: ServerCall<*, *>, headers: Metadata): CoroutineContext {
    return MetadataElement(value = headers)
  }
}

/**
 * A metadata element for coroutine context. It is used for accessing the gRPC [Metadata] from
 * [coroutineContext].
 *
 * Example usage: coroutineContext[MetadataElement]?.value
 */
internal data class MetadataElement(val value: Metadata) : CoroutineContext.Element {
  companion object Key : CoroutineContext.Key<MetadataElement>

  override val key: CoroutineContext.Key<MetadataElement>
    get() = Key
}
