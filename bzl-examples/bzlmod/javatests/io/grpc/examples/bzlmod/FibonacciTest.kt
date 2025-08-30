package io.grpc.examples.bzlmod

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.extensions.proto.ProtoTruth.assertThat
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.testing.GrpcCleanupRule
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FibonacciTest {
  @get:Rule val grpcCleanup = GrpcCleanupRule()

  private lateinit var stub: FibonacciServiceGrpcKt.FibonacciServiceCoroutineStub

  @Before
  fun setUp() {
    val serverName = InProcessServerBuilder.generateName()

    grpcCleanup.register(
      InProcessServerBuilder.forName(serverName)
        .directExecutor()
        .addService(Fibonacci())
        .build()
        .start()
    )

    stub =
      FibonacciServiceGrpcKt.FibonacciServiceCoroutineStub(
        grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build())
      )
  }

  @Test
  fun query_succeeds() {
    runBlocking {
      val response =
        stub.query(
          queryRequest {
            nth = 20
            mod = 1000000007
          }
        )
      assertThat(response).isEqualTo(queryResponse { nthFibonacci = 6765 })
    }
  }
}
