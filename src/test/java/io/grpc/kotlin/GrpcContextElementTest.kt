package io.grpc.kotlin

import com.google.common.truth.Truth.assertThat
import com.google.testing.testsize.MediumTest
import com.google.testing.testsize.MediumTestAttribute
import io.grpc.Context
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.Executors

@RunWith(JUnit4::class)
@MediumTest(MediumTestAttribute.THREADS)
class GrpcContextElementTest {
  val testKey = Context.key<String>("test")

  @Test
  fun testContextPropagation() {
    val testGrpcContext = Context.ROOT.withValue(testKey, "testValue")
    val coroutineContext =
      Executors.newSingleThreadExecutor().asCoroutineDispatcher() + GrpcContextElement(testGrpcContext)
    runBlocking(coroutineContext) {
      val currentTestKey = testKey.get()
      // gets from the implicit current gRPC context
      assertThat(currentTestKey).isEqualTo("testValue")
    }
  }

  @Test
  fun testRun() {
    val testGrpcContext = Context.ROOT.withValue(testKey, "testValue")
    val coroutineContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    runBlocking(coroutineContext) {
      testGrpcContext.runCoroutine {
        assertThat(testKey.get()).isEqualTo("testValue")
      }
    }
  }
}