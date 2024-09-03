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

import io.grpc.ManagedChannel   // gRPC 채널을 관리하는 클래스
import io.grpc.ManagedChannelBuilder    // gRPC 채널을 생성하는 데 사용되는 빌더 클래스
import io.grpc.examples.helloworld.GreeterGrpcKt.GreeterCoroutineStub   // gRPC 서비스에 대한 비동기 호출을 지원하는 Kotlin 코루틴 스텁(stub) 클래스 ?
import java.io.Closeable    // 안전한 자원 반납을 위한 인터페이스
import java.util.concurrent.TimeUnit

class HelloWorldClient(private val channel: ManagedChannel) : Closeable {
    private val stub: GreeterCoroutineStub = GreeterCoroutineStub(channel)

    // 비동기로 구현된 함수
    suspend fun greet(name: String) {
        val request = helloRequest { this.name = name }
        val response = stub.sayHello(request)   // 서버의 sayHello 함수 호출 후 응답 받음
        println("Received: ${response.message}, ${response.age}, ${response.height}")
        val againResponse = stub.sayHelloAgain(request)
        println("Received: ${againResponse.message}, ${response.age}, ${response.height}")
    }

    // 비동기로 구현된 함수
    suspend fun greet2(name: String) {
        val request = helloRequest { this.name = name }
        val response = stub.sayHello2(request)   // 서버의 sayHello 함수 호출 후 응답 받음
        println("Received: ${response.message}, ${response.age}, ${response.height}")
        val againResponse = stub.sayHelloAgain2(request)
        println("Received: ${againResponse.message}, ${response.age}, ${response.height}")
    }

    // gRPC 채널을 닫음
    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}

/**
 * Greeter, uses first argument as name to greet if present;
 * greets "world" otherwise.
 */
suspend fun main(args: Array<String>) {
    val port = System.getenv("PORT")?.toInt() ?: 50051

    val channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build()

    val client = HelloWorldClient(channel)

    val user = args.singleOrNull() ?: "world"
    client.greet(user)
    client.greet2(user)
}
