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
import io.grpc.StatusRuntimeException
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.examples.helloworld.GreeterGrpcKt.GreeterCoroutineStub // Unnecessary?
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class HelloWorldClient private constructor(
  val channel: ManagedChannel,
  val stub: GreeterCoroutineStub,
  val printer: Printer
) : Closeable {

  constructor(
    channelBuilder: ManagedChannelBuilder<*>,
    dispatcher: CoroutineDispatcher,
    printer: Printer
  ) : this(channelBuilder.executor(dispatcher.asExecutor()).build(), printer)

  constructor(
    channel: ManagedChannel,
    printer: Printer
  ) : this(channel, GreeterCoroutineStub(channel), printer)

  /** Say hello to server.  */
  fun greet(name: String) = runBlocking {
      printer.println("Will try to greet $name")

      val request = HelloRequest.newBuilder().setName(name).build()
      try {
        val response = stub.sayHello(request)
        printer.println("Greeter client received: ${response.message}")
      } catch (e: StatusException) {
        printer.println("RPC failed: ${e.status}")
      }
  }

  companion object {
    /**
      * Greeter. If provided, the first element of `args` is the name to use in the
      * greeting.
      */
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
      Executors.newFixedThreadPool(10).asCoroutineDispatcher().use { dispatcher ->
        HelloWorldClient(
          ManagedChannelBuilder.forAddress("localhost", 50051)
            .usePlaintext(),
          dispatcher,
          Printer.stdout
        ).use {
          val user = if (args.size > 0) "${args[0]}" else "world"
          it.greet(user)
        }
      }
    }
  }

  override fun close() {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
  }

  interface Printer {
    companion object {
      val stdout = object : Printer {
        override fun println(str: String) {
          System.out.println(str)
        }
      }
    }
    fun println(str: String)
  }

}
