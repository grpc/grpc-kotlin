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

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.examples.helloworld.GreeterGrpcKt.GreeterCoroutineStub
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.io.Closeable
import java.util.concurrent.TimeUnit

class HelloWorldClient constructor(
    private val channel: ManagedChannel
) : Closeable {
  private val stub: GreeterCoroutineStub = GreeterCoroutineStub(channel)

  suspend fun greet(name: String) = coroutineScope {
    val request = HelloRequest.newBuilder().setName(name).build()
    val response = async { stub.sayHello(request) }
    println("Received: ${response.await().message}")
  }

  override fun close() {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
  }
}


/**
 * Greeter, uses first argument as name to greet if present;
 * greets "world" otherwise.
 */
fun main(args: Array<String>) = runBlocking {
  val port = 50051

  val client = HelloWorldClient(
        ManagedChannelBuilder.forAddress("localhost", port)
            .usePlaintext()
            .executor(Dispatchers.Default.asExecutor())
            .build())

  val user = args.singleOrNull() ?: "world"
  client.greet(user)
}
