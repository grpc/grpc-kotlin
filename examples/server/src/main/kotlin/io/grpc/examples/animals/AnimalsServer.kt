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

package io.grpc.examples.animals

import io.grpc.Server
import io.grpc.ServerBuilder

class AnimalsServer constructor(private val port: Int) {
  val server: Server =
    ServerBuilder.forPort(port)
      .addService(DogService())
      .addService(PigService())
      .addService(SheepService())
      .build()

  fun start() {
    server.start()
    println("Server started, listening on $port")
    Runtime.getRuntime()
      .addShutdownHook(
        Thread {
          println("*** shutting down gRPC server since JVM is shutting down")
          this@AnimalsServer.stop()
          println("*** server shut down")
        },
      )
  }

  private fun stop() {
    server.shutdown()
  }

  fun blockUntilShutdown() {
    server.awaitTermination()
  }

  internal class DogService : DogGrpcKt.DogCoroutineImplBase() {
    override suspend fun bark(request: BarkRequest) = barkReply { message = "Bark!" }
  }

  internal class PigService : PigGrpcKt.PigCoroutineImplBase() {
    override suspend fun oink(request: OinkRequest) = oinkReply { message = "Oink!" }
  }

  internal class SheepService : SheepGrpcKt.SheepCoroutineImplBase() {
    override suspend fun baa(request: BaaRequest) = baaReply { message = "Baa!" }
  }
}

fun main() {
  val port = 50051
  val server = AnimalsServer(port)
  server.start()
  server.blockUntilShutdown()
}
