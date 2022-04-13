/*
 * Copyright 2014 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.grpc.testing.integration

import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.ImmutableList
import io.grpc.*
import io.grpc.internal.testing.TestClientStreamTracer
import io.grpc.internal.testing.TestServerStreamTracer
import io.grpc.testing.TestUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.DisableOnDebug
import org.junit.rules.TestRule
import org.junit.rules.Timeout
import java.io.IOException
import java.net.SocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Abstract base class for all GRPC transport tests.
 *
 *
 *  New tests should avoid using Mockito to support running on AppEngine.
 */
@ExperimentalCoroutinesApi
@FlowPreview
abstract class AbstractCoroutinesInteropTest {
  @get:Rule
  val globalTimeout: TestRule
  private val serverCallCapture = AtomicReference<ServerCall<*, *>>()
  private val requestHeadersCapture = AtomicReference<Metadata?>()
  private val contextCapture = AtomicReference<Context>()
  private var testServiceExecutor: ScheduledExecutorService? = null
  private var server: Server? = null
  private val serverStreamTracers = LinkedBlockingQueue<ServerStreamTracerInfo>()

  private class ServerStreamTracerInfo internal constructor(
    val fullMethodName: String,
    val tracer: InteropServerStreamTracer
  ) {

    class InteropServerStreamTracer : TestServerStreamTracer() {
      @Volatile
      var contextCapture: Context? = null

      override fun filterContext(context: Context): Context {
        contextCapture = context
        return super.filterContext(context)
      }
    }
  }

  private val serverStreamTracerFactory: ServerStreamTracer.Factory =
    object : ServerStreamTracer.Factory() {
      override fun newServerStreamTracer(
        fullMethodName: String,
        headers: Metadata
      ): ServerStreamTracer {
        val tracer = ServerStreamTracerInfo.InteropServerStreamTracer()
        serverStreamTracers.add(ServerStreamTracerInfo(fullMethodName, tracer))
        return tracer
      }
    }

  private fun startServer() {
    val builder = serverBuilder
    if (builder == null) {
      server = null
      return
    }
    val executor = Executors.newScheduledThreadPool(2)
    testServiceExecutor = executor
    val allInterceptors: List<ServerInterceptor> = ImmutableList.builder<ServerInterceptor>()
      .add(recordServerCallInterceptor(serverCallCapture))
      .add(TestUtils.recordRequestHeadersInterceptor(requestHeadersCapture))
      .add(recordContextInterceptor(contextCapture))
      .addAll(TestServiceImpl.interceptors)
      .build()
    builder
      .addService(
        ServerInterceptors.intercept(
          TestServiceImpl(executor),
          allInterceptors
        )
      )
      .addStreamTracerFactory(serverStreamTracerFactory)
    server = try {
      builder.build().start()
    } catch (ex: IOException) {
      throw RuntimeException(ex)
    }
  }

  private fun stopServer() {
    server?.shutdownNow()
    server?.awaitTermination()

    testServiceExecutor?.let {
      it.shutdown()
      while (!it.isTerminated) {
        it.awaitTermination(1, TimeUnit.SECONDS)
      }
    }
  }

  @get:VisibleForTesting
  val listenAddress: SocketAddress
    get() = server!!.listenSockets.first()

  protected lateinit var channel: ManagedChannel

  protected lateinit var stub: TestServiceGrpcKt.TestServiceCoroutineStub

  // to be deleted when subclasses are ready to migrate
  @JvmField
  var blockingStub: TestServiceGrpc.TestServiceBlockingStub? = null

  // to be deleted when subclasses are ready to migrate
  @JvmField
  var asyncStub: TestServiceGrpc.TestServiceStub? = null

  private val clientStreamTracers = LinkedBlockingQueue<TestClientStreamTracer>()
  private val clientStreamTracerFactory: ClientStreamTracer.Factory =
    object : ClientStreamTracer.Factory() {
      override fun newClientStreamTracer(
        info: ClientStreamTracer.StreamInfo,
        headers: Metadata
      ): ClientStreamTracer {
        val tracer = TestClientStreamTracer()
        clientStreamTracers.add(tracer)
        return tracer
      }
    }
  private val tracerSetupInterceptor: ClientInterceptor = object : ClientInterceptor {
    override fun <ReqT, RespT> interceptCall(
      method: MethodDescriptor<ReqT, RespT>,
      callOptions: CallOptions,
      next: Channel
    ): ClientCall<ReqT, RespT> {
      return next.newCall(
        method, callOptions.withStreamTracerFactory(clientStreamTracerFactory)
      )
    }
  }

  /**
   * Must be called by the subclass setup method if overridden.
   */
  @Before
  fun setUp() {
    startServer()
    channel = createChannel()
    stub =
      TestServiceGrpcKt.TestServiceCoroutineStub(channel).withInterceptors(tracerSetupInterceptor)
    blockingStub = TestServiceGrpc.newBlockingStub(channel).withInterceptors(tracerSetupInterceptor)
    asyncStub = TestServiceGrpc.newStub(channel).withInterceptors(tracerSetupInterceptor)
    val additionalInterceptors = additionalInterceptors
    if (additionalInterceptors != null) {
      stub = stub.withInterceptors(*additionalInterceptors)
    }
    requestHeadersCapture.set(null)
  }

  /** Clean up.  */
  @After
  open fun tearDown() {
    channel.shutdownNow()
    try {
      channel.awaitTermination(1, TimeUnit.SECONDS)
    } catch (ie: InterruptedException) {
      logger.log(Level.FINE, "Interrupted while waiting for channel termination", ie)
      // Best effort. If there is an interruption, we want to continue cleaning up, but quickly
      Thread.currentThread().interrupt()
    }
    stopServer()
  }

  protected abstract fun createChannel(): ManagedChannel

  protected val additionalInterceptors: Array<ClientInterceptor>?
    get() = null

  /**
   * Returns the server builder used to create server for each test run.  Return `null` if
   * it shouldn't start a server in the same process.
   */
  protected open val serverBuilder: ServerBuilder<*>?
    get() = null

  companion object {
    private val logger = Logger.getLogger(AbstractCoroutinesInteropTest::class.java.name)
    /** Must be at least [.unaryPayloadLength], plus some to account for encoding overhead.  */
    const val MAX_MESSAGE_SIZE = 16 * 1024 * 1024
    /**
     * Captures the request attributes. Useful for testing ServerCalls.
     * [ServerCall.getAttributes]
     */
    private fun recordServerCallInterceptor(
      serverCallCapture: AtomicReference<ServerCall<*, *>>
    ): ServerInterceptor {
      return object : ServerInterceptor {
        override fun <ReqT, RespT> interceptCall(
          call: ServerCall<ReqT, RespT>,
          requestHeaders: Metadata,
          next: ServerCallHandler<ReqT, RespT>
        ): ServerCall.Listener<ReqT> {
          serverCallCapture.set(call)
          return next.startCall(call, requestHeaders)
        }
      }
    }

    private fun recordContextInterceptor(
      contextCapture: AtomicReference<Context>
    ): ServerInterceptor {
      return object : ServerInterceptor {
        override fun <ReqT, RespT> interceptCall(
          call: ServerCall<ReqT, RespT>,
          requestHeaders: Metadata,
          next: ServerCallHandler<ReqT, RespT>
        ): ServerCall.Listener<ReqT> {
          contextCapture.set(Context.current())
          return next.startCall(call, requestHeaders)
        }
      }
    }
  }

  /**
   * Constructor for tests.
   */
  init {
    var timeout: TestRule = Timeout.seconds(60)
    try {
      timeout = DisableOnDebug(timeout)
    } catch (t: Throwable) { // This can happen on Android, which lacks some standard Java class.
      // Seen at https://github.com/grpc/grpc-java/pull/5832#issuecomment-499698086
      logger.log(Level.FINE, "Debugging not disabled.", t)
    }
    globalTimeout = timeout
  }
}
