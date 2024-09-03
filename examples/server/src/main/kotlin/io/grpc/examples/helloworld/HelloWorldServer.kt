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

// gRPC 서버를 구현하는 클래스
class HelloWorldServer(private val port: Int) {
    // HelloWorldService 서비스를 서버에 추가
    val server: Server =
        ServerBuilder
            .forPort(port)
            .addService(HelloWorldService())
            .build()

    // 서버 시작 함수
    fun start() {
        server.start()
        println("Server started, listening on $port")
        // addShutdownHook 함수를 사용하여 JVM 종료 시 서버를 안전하게 종료
        Runtime.getRuntime().addShutdownHook(
            Thread {
                println("*** shutting down gRPC server since JVM is shutting down")
                this@HelloWorldServer.stop()
                println("*** server shut down")
            },
        )
    }

    private fun stop() {
        server.shutdown()
    }

    // 서버가 종료될 때까지 대기하는 함수
    fun blockUntilShutdown() {
        server.awaitTermination()
    }

    // GreeterCoroutineImplBase 클래스를 상속받아 구현한 HelloWorldService 클래스
    internal class HelloWorldService : GreeterGrpcKt.GreeterCoroutineImplBase() {
        override suspend fun sayHello(request: HelloRequest) =
            helloReply {
                message = "Hello ${request.name}"
            }

        override suspend fun sayHelloAgain(request: HelloRequest) =
            helloReply {
                message = "Hello again ${request.name}"
            }

        override suspend fun sayHello2(request: HelloRequest) =
            helloReply {
                message = "Hello 2 ${request.name}"
            }

        override suspend fun sayHelloAgain2(request: HelloRequest) =
            helloReply {
                message = "Hello again 2 ${request.name}"
            }
    }
}

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 50051
    val server = HelloWorldServer(port)
    server.start()
    server.blockUntilShutdown()
}
