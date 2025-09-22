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

import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusException
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking

/**
 * Extracts the value of a [Deferred] known to be completed, or throws its exception if it was not
 * completed successfully. (Non-experimental variant of `getDone`.)
 */
internal val <T> Deferred<T>.doneValue: T
  get() {
    check(isCompleted) { "doneValue should only be called on completed Deferred values" }
    return runBlocking(Dispatchers.Unconfined) { await() }
  }

/** Cancels a [Job] with a cause and suspends until the job completes/is finished cancelling. */
internal suspend fun Job.cancelAndJoin(message: String, cause: Exception? = null) {
  cancel(message, cause)
  join()
}

/**
 * Returns this flow, save that if there is not exactly one element, it throws a [StatusException].
 *
 * The purpose of this function is to enable the one element to get processed before we have
 * confirmation that the input flow is done.
 */
internal fun <T> Flow<T>.singleOrStatusFlow(expected: String, descriptor: Any): Flow<T> = flow {
  var found = false
  collect {
    if (!found) {
      found = true
      emit(it)
    } else {
      throw StatusException(
        Status.INTERNAL.withDescription("Expected one $expected for $descriptor but received two")
      )
    }
  }
  if (!found) {
    throw StatusException(
      Status.INTERNAL.withDescription("Expected one $expected for $descriptor but received none")
    )
  }
}

/**
 * Returns the one and only element of this flow, and throws a [StatusException] if there is not
 * exactly one element.
 */
internal suspend fun <T> Flow<T>.singleOrStatus(expected: String, descriptor: Any): T =
  singleOrStatusFlow(expected, descriptor).single()

/** Returns gRPC Metadata. */
suspend fun grpcMetadata(): Metadata {
  val metadataElement =
    coroutineContext[MetadataElement]
      ?: throw StatusException(
        Status.INTERNAL.withDescription(
          "gRPC Metadata not found in coroutineContext. Ensure that MetadataCoroutineContextInterceptor is used in gRPC server."
        )
      )
  return metadataElement.value
}
