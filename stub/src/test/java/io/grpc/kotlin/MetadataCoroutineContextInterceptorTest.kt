package io.grpc.kotlin

import com.google.common.truth.Truth.assertThat
import io.grpc.BindableService
import io.grpc.Channel
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.examples.helloworld.GreeterGrpcKt
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import io.grpc.examples.helloworld.helloReply
import io.grpc.examples.helloworld.helloRequest
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.testing.GrpcCleanupRule
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MetadataCoroutineContextInterceptorTest {
  @Rule @JvmField val grpcCleanup = GrpcCleanupRule()

  @Test
  fun interceptCall_providesMetadataToCoroutineContext() {
    val keyA = Metadata.Key.of("test-header-a", Metadata.ASCII_STRING_MARSHALLER)
    val keyB = Metadata.Key.of("test-header-b", Metadata.ASCII_STRING_MARSHALLER)
    val clientStub =
      GreeterGrpcKt.GreeterCoroutineStub(
        testChannel(
          object : GreeterGrpcKt.GreeterCoroutineImplBase() {
            override suspend fun sayHello(request: HelloRequest): HelloReply {
              val metadata = grpcMetadata()
              return helloReply { message = listOf(metadata.get(keyA).toString(), metadata.get(keyB).toString()).joinToString() }
            }
          }
        )
      )
    val metadata = Metadata()
    metadata.put(keyA, "Test message A")
    metadata.put(keyB, "Test message B")

    val response = runBlocking { clientStub.sayHello(helloRequest {}, metadata) }

    assertThat(response.message).isEqualTo("Test message A, Test message B")
  }

  @Test
  fun grpcMetadata_interceptorNotInjected_throwsStatusExceptionInternal() {
    val keyA = Metadata.Key.of("test-header-a", Metadata.ASCII_STRING_MARSHALLER)
    val keyB = Metadata.Key.of("test-header-b", Metadata.ASCII_STRING_MARSHALLER)
    val clientStub =
      GreeterGrpcKt.GreeterCoroutineStub(
        testChannel(
          object : GreeterGrpcKt.GreeterCoroutineImplBase() {
            override suspend fun sayHello(request: HelloRequest): HelloReply {
              val metadata = grpcMetadata()
              return helloReply { message = listOf(metadata.get(keyA).toString(), metadata.get(keyB).toString()).joinToString() }
            }
          },
          false
        )
      )
    val metadata = Metadata()
    metadata.put(keyA, "Test message A")
    metadata.put(keyB, "Test message B")

    val exception =
      assertFailsWith<StatusException> {
        runBlocking { clientStub.sayHello(helloRequest {}, metadata) }
      }
    assertThat(exception.status.code).isEqualTo(Status.INTERNAL.code)
    assertThat(exception.status.description)
      .isEqualTo(
        "gRPC Metadata not found in coroutineContext. Ensure that MetadataCoroutineContextInterceptor is used in gRPC server."
      )
  }

  private fun testChannel(service: BindableService, attachInterceptor: Boolean = true): Channel {
    val serverName = InProcessServerBuilder.generateName()
    var builder = InProcessServerBuilder.forName(serverName).directExecutor()
    if (attachInterceptor) {
      builder = builder.intercept(MetadataCoroutineContextInterceptor())
    }
    grpcCleanup.register(builder.addService(service).build().start())
    return grpcCleanup.register(
      InProcessChannelBuilder.forName(serverName).directExecutor().build()
    )
  }
}
