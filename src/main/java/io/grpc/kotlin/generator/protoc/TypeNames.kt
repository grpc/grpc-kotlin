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

package io.grpc.kotlin.generator.protoc

import com.google.protobuf.ByteString
import com.squareup.kotlinpoet.asTypeName

object TypeNames {
  internal val ITERABLE = Iterable::class.asTypeName()
  internal val PAIR = Pair::class.asTypeName()
  internal val MAP = Map::class.asTypeName()
  internal val STRING = String::class.asTypeName()
  val BYTE_STRING = ByteString::class.asTypeName()
}
