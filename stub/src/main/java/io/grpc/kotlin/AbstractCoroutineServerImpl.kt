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

package io.grpc.kotlin

import io.grpc.BindableService
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Skeleton implementation of a coroutine-based gRPC server implementation.  Intended to be
 * subclassed by generated code.
 */
abstract class AbstractCoroutineServerImpl(
  /** The context in which to run server coroutines. */
  val context: CoroutineContext = EmptyCoroutineContext
) : BindableService {
  /*
   * Each RPC is executed in its own coroutine scope built from [context].  We could have a parent
   * scope, but it doesn't really add anything: we don't want users to be able to launch tasks
   * in that scope easily, since almost all coroutines should be scoped to the RPC and cancelled
   * if the RPC is cancelled.  Users who don't want that behavior should manage their own scope for
   * it.  Additionally, gRPC server objects don't have their own notion of shutdown: shutting down
   * a server means cancelling the RPCs, not calling a teardown on the server object.
   */
}