/*
 * Copyright 2022 gRPC authors.
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

import io.grpc.testing.GrpcServerRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class HelloWorldServerTest {

    @get:Rule
    val grpcServerRule: GrpcServerRule = GrpcServerRule().directExecutor()

    @Test
    fun sayHello() = runBlocking {
        val service = HelloWorldServer.HelloWorldService()
        grpcServerRule.serviceRegistry.addService(service)

        val stub = GreeterGrpcKt.GreeterCoroutineStub(grpcServerRule.channel)
        val testName = "test name"

        val reply = stub.sayHello(helloRequest { name = testName })

        assertEquals("Hello $testName", reply.message)
    }
}
