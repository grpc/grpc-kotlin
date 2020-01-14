/*
 * Copyright 2020 gRPC authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.kotlin

import com.google.common.truth.Truth.assertThat
import com.google.testing.testsize.SmallTest
import com.nhaarman.mockitokotlin2.mock
import io.grpc.CallOptions
import io.grpc.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@RunWith(JUnit4::class)
@SmallTest
class AbstractCoroutineStubTest {
  val channel = mock<Channel> {  } // We need a Channel, but we're not using it

  object TestContextElement: CoroutineContext.Key<TestContextElement>, CoroutineContext.Element {
    override val key: CoroutineContext.Key<*>
      get() = this
  }

  val testCallOptionsKey: CallOptions.Key<String> =
    CallOptions.Key.create<String>("testCallOptionsKey")

  class CoroutineStub(
    channel: Channel,
    coroutineContext: CoroutineContext,
    callOptions: CallOptions = CallOptions.DEFAULT
  ) : AbstractCoroutineStub<CoroutineStub>(channel, coroutineContext, callOptions) {
    override fun build(
      channel: Channel,
      coroutineContext: CoroutineContext,
      callOptions: CallOptions
    ) = CoroutineStub(channel, coroutineContext, callOptions)

    suspend fun makeAssertions(block: suspend CoroutineStub.() -> Unit) {
      withContext(coroutineContext) { block() }
    }
  }

  @Test
  fun stubCoroutineContextPropagated() = runBlocking {
    CoroutineStub(channel, TestContextElement).makeAssertions {
      assertThat(coroutineContext[TestContextElement] != null)
    }
  }

  @Test
  fun outsideCoroutineContextPropagated() = runBlocking(TestContextElement) {
    CoroutineStub(channel, EmptyCoroutineContext).makeAssertions {
      assertThat(coroutineContext[TestContextElement] != null)
    }
  }

  @Test
  fun callOptionsPropagated() = runBlocking {
    CoroutineStub(
      channel,
      EmptyCoroutineContext,
      CallOptions.DEFAULT.withOption(testCallOptionsKey, "testValue")
    ).makeAssertions {
      assertThat(callOptions.getOption(testCallOptionsKey)).isEqualTo("testValue")
    }
  }

  @Test
  fun withCallOptions() = runBlocking {
    val stub = CoroutineStub(channel, EmptyCoroutineContext)
    stub.withOption(testCallOptionsKey, "testValue").makeAssertions {
      assertThat(callOptions.getOption(testCallOptionsKey)).isEqualTo("testValue")
    }
  }
}
