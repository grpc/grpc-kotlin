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

package io.grpc.examples.helloworld

import io.grpc.StatusException
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.examples.helloworld.GreeterGrpcKt.GreeterCoroutineStub
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking

class HelloWorldClient constructor(
    private val channel: ManagedChannel
) : Closeable {
  private val stub: GreeterCoroutineStub = GreeterCoroutineStub(channel)

  fun greet(name: String) = runBlocking {
    val request = HelloRequest.newBuilder().setName(name).build()
    try {
      val response = stub.sayHello(request)
      println("Greeter client received: ${response.message}")
    } catch (e: StatusException) {
      println("RPC failed: ${e.status}")
    }
  }

  override fun close() {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
  }
}


/**
 * Greeter, uses first argument as name to greet if present;
 * greets "world" otherwise.
 */
fun main(args: Array<String>) {
  val port = 50051

  Executors.newFixedThreadPool(10).asCoroutineDispatcher().use { dispatcher ->
    HelloWorldClient(
        ManagedChannelBuilder.forAddress("localhost", port)
            .usePlaintext()
            .executor(dispatcher.asExecutor()).build()
    ).use {
      val user = args.singleOrNull() ?: "world"
      it.greet(user)
    }
  }
}
