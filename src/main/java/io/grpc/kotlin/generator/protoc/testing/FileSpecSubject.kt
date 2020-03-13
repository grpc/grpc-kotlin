/*
 * Copyright 2020 Google LLC
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

package io.grpc.kotlin.generator.protoc.testing

import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout
import com.squareup.kotlinpoet.FileSpec

val fileSpecs: Subject.Factory<FileSpecSubject, FileSpec> = Subject.Factory(::FileSpecSubject)

/** Make Truth assertions about [fileSpec]. */
fun assertThat(fileSpec: FileSpec): FileSpecSubject = assertAbout(fileSpecs).that(fileSpec)

/** A Truth subject for [FileSpec]. */
class FileSpecSubject(
  failureMetadata: FailureMetadata,
  private val actual: FileSpec
) : Subject(failureMetadata, actual) {
  fun generates(indentedCode: String) {
    val expectedCode = indentedCode.trimIndent()
    val actualCode = actual.toString().trim().lines().joinToString("\n") { it.trimEnd() }
    check("code").that(actualCode).isEqualTo(expectedCode)
  }

  fun hasName(name: String) {
    check("name").that(actual.name).isEqualTo(name)
  }
}
