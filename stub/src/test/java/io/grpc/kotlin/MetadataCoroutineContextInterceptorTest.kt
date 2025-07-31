package io.grpc.kotlin

import io.grpc.BindableService
import io.grpc.Channel
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.examples.helloworld.GreeterGrpcKt
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.testing.GrpcCleanupRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MetadataCoroutineContextInterceptorTest {
    @Rule
    @JvmField
    val grpcCleanup = GrpcCleanupRule()

    @Test
    fun `interceptor provides gRPC Metadata to coroutineContext`() {
        val key = Metadata.Key.of("test-header", Metadata.ASCII_STRING_MARSHALLER)
        val clientStub =
            GreeterGrpcKt.GreeterCoroutineStub(testChannel(object : GreeterGrpcKt.GreeterCoroutineImplBase() {
                override suspend fun sayHello(request: HelloRequest): HelloReply {
                    val metadata = grpcMetadata()
                    return HelloReply.newBuilder()
                        .setMessage(metadata.get(key).toString())
                        .build()
                }
            }))
        val metadata = Metadata()
        metadata.put(key, "Test message")

        val response = runBlocking { clientStub.sayHello(HelloRequest.getDefaultInstance(), metadata) }

        Assertions.assertEquals("Test message", response.message)
    }

    @Test
    fun `fails to extract gRPC Metadata if interceptor is not injected`() {
        val key = Metadata.Key.of("test-header", Metadata.ASCII_STRING_MARSHALLER)
        val clientStub =
            GreeterGrpcKt.GreeterCoroutineStub(testChannel(object : GreeterGrpcKt.GreeterCoroutineImplBase() {
                override suspend fun sayHello(request: HelloRequest): HelloReply {
                    val metadata = grpcMetadata()
                    return HelloReply.newBuilder()
                        .setMessage(metadata.get(key).toString())
                        .build()
                }
            }, false))
        val metadata = Metadata()
        metadata.put(key, "Test message")

        val exception = Assertions.assertThrows(StatusException::class.java) {
            runBlocking { clientStub.sayHello(HelloRequest.getDefaultInstance(), metadata) }
        }
        Assertions.assertEquals(Status.INTERNAL.code, exception.status.code)
        Assertions.assertEquals(
            "gRPC Metadata not found in coroutineContext. Ensure that MetadataCoroutineContextInterceptor is used in gRPC server.",
            exception.status.description
        )
    }

    private fun testChannel(service: BindableService, attachInterceptor: Boolean = true): Channel {
        val serverName = InProcessServerBuilder.generateName()
        var builder = InProcessServerBuilder.forName(serverName).directExecutor()
        if (attachInterceptor) {
            builder = builder.intercept(MetadataCoroutineContextInterceptor())
        }
        grpcCleanup.register(builder.addService(service).build().start())
        return grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build())
    }
}
