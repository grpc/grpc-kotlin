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

import io.grpc.Server
import io.grpc.ServerBuilder

class HelloWorldServer private constructor(
  val port: Int,
  val server: Server
) {
  constructor(port: Int) :
  this(
    serverBuilder = ServerBuilder.forPort(port),
    port = port
  )

  constructor(
    serverBuilder: ServerBuilder<*>,
    port: Int
  ) : this(
    port = port,
    server = serverBuilder.addService(HelloWorldService()).build()
  )

  fun start() {
    server.start()
    println("Server started, listening on $port")
    Runtime.getRuntime().addShutdownHook(
      Thread {
        println("*** shutting down gRPC server since JVM is shutting down")
        this@HelloWorldServer.stop()
        println("*** server shut down")
      }
    )
  }

  fun stop() {
    server.shutdown()
  }

  fun blockUntilShutdown() {
    server.awaitTermination()
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      val port = 50051
      val server = HelloWorldServer(port)
      server.start()
      server.blockUntilShutdown()
    }
  }

  class HelloWorldService : GreeterGrpcKt.GreeterCoroutineImplBase() {

    override suspend fun sayHello(request: HelloRequest): HelloReply {
      return HelloReply.newBuilder().setMessage("Hello ${request.name}").build()
    }
  }
}
