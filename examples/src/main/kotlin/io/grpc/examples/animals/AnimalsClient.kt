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

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.io.Closeable
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

class AnimalsClient constructor(private val channel: ManagedChannel) : Closeable {
    private val dogStub: DogGrpcKt.DogCoroutineStub by lazy { DogGrpcKt.DogCoroutineStub(channel) }
    private val pigStub: PigGrpcKt.PigCoroutineStub by lazy { PigGrpcKt.PigCoroutineStub(channel) }
    private val sheepStub: SheepGrpcKt.SheepCoroutineStub by lazy { SheepGrpcKt.SheepCoroutineStub(channel) }

    suspend fun bark() = coroutineScope {
        val request = BarkRequest.getDefaultInstance()
        val response = async { dogStub.bark(request) }
        println("Received: ${response.await().message}")
    }

    suspend fun oink() = coroutineScope {
        val request = OinkRequest.getDefaultInstance()
        val response = async { pigStub.oink(request) }
        println("Received: ${response.await().message}")
    }

    suspend fun baa() = coroutineScope {
        val request = BaaRequest.getDefaultInstance()
        val response = async { sheepStub.baa(request) }
        println("Received: ${response.await().message}")
    }

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}

/**
 * Talk to the animals. Fluent in dog, pig and sheep.
 */
fun main(args: Array<String>) = runBlocking {
    val usage = "usage: animals_client [{dog|pig|sheep} ...]"

    if (args.isEmpty()) {
        println("No animals specified.")
        println(usage)
    }

    val port = 50051

    val client = AnimalsClient(
            ManagedChannelBuilder.forAddress("localhost", port)
                    .usePlaintext()
                    .executor(Dispatchers.Default.asExecutor())
                    .build())

    args.forEach {
        when (it) {
            "dog" -> client.bark()
            "pig" -> client.oink()
            "sheep" -> client.baa()
            else -> {
                println("Unknown animal type: \"$it\". Try \"dog\", \"pig\" or \"sheep\".")
                println(usage)
            }
        }
    }
}
