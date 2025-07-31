package io.grpc.kotlin

import io.grpc.Metadata
import io.grpc.ServerCall
import kotlin.coroutines.CoroutineContext

/**
 * Propagates gRPC Metadata (HTTP Headers) to coroutineContext.
 * Attach the interceptor to gRPC Server and then access the Metadata using grpcMetadata() function.
 *
 * Example usage:
 *
 * ServerBuilder.forPort(8060)
 *   .addService(GreeterImpl())
 *   .intercept(MetadataCoroutineContextInterceptor())
 *
 * grpcMetadata()
 */
class MetadataCoroutineContextInterceptor : CoroutineContextServerInterceptor() {
    final override fun coroutineContext(call: ServerCall<*, *>, headers: Metadata): CoroutineContext {
        return MetadataElement(value = headers)
    }
}

/**
 * Used for accessing the gRPC Metadata from coroutineContext.
 * Example usage:
 *   coroutineContext[MetadataElement]?.value
 */
internal data class MetadataElement(val value: Metadata) : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<MetadataElement>

    override val key: CoroutineContext.Key<MetadataElement> get() = Key
}
