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

class AnimalsClient(private val channel: ManagedChannel) : Closeable {
    private val dogStub: DogGrpcKt.DogCoroutineStub by lazy { DogGrpcKt.DogCoroutineStub(channel) }
    private val pigStub: PigGrpcKt.PigCoroutineStub by lazy { PigGrpcKt.PigCoroutineStub(channel) }
    private val sheepStub: SheepGrpcKt.SheepCoroutineStub by lazy { SheepGrpcKt.SheepCoroutineStub(channel) }

    suspend fun bark() {
        val request = BarkRequest.getDefaultInstance()
        val response = dogStub.bark(request)
        println("Received: ${response.message}")
    }

    suspend fun oink() {
        val request = OinkRequest.getDefaultInstance()
        val response = pigStub.oink(request)
        println("Received: ${response.message}")
    }

    suspend fun baa() {
        val request = BaaRequest.getDefaultInstance()
        val response = sheepStub.baa(request)
        println("Received: ${response.message}")
    }

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}

/**
 * Talk to the animals. Fluent in dog, pig and sheep.
 */
suspend fun main(args: Array<String>) {
    val usage = "usage: animals_client [{dog|pig|sheep} ...]"

    if (args.isEmpty()) {
        println("No animals specified.")
        println(usage)
    }

    val port = 50051

    val channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build()

    val client = AnimalsClient(channel)

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
