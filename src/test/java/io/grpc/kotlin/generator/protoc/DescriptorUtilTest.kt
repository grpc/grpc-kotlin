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

import com.google.common.truth.Truth.assertThat
import com.google.protos.testing.ProtoFileWithHyphen
import io.grpc.kotlin.generator.protoc.testproto.Example3
import io.grpc.kotlin.generator.protoc.testproto.Example3.ExampleEnum
import io.grpc.kotlin.generator.protoc.testproto.Example3.ExampleMessage
import io.grpc.kotlin.generator.protoc.testproto.HasNestedClassNameConflictOuterClass
import io.grpc.kotlin.generator.protoc.testproto.HasOuterClassNameConflictOuterClass
import io.grpc.kotlin.generator.protoc.testproto.MyExplicitOuterClassName
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Tests for the methods in DescriptorUtil.kt. */
@RunWith(JUnit4::class)
class DescriptorUtilTest {
  @Test
  fun messageClassSimpleName() {
    val messageDescriptor = ExampleMessage.getDescriptor()
    assertThat(messageDescriptor.messageClassSimpleName)
      .isEqualTo(ClassSimpleName(ExampleMessage::class.simpleName!!))
  }

  @Test
  fun enumClassSimpleName() {
    val enumDescriptor = ExampleEnum.getDescriptor()
    assertThat(enumDescriptor.enumClassSimpleName)
      .isEqualTo(ClassSimpleName(ExampleEnum::class.simpleName!!))
  }

  @Test
  fun outerClassSimpleName_simple() {
    val fileDescriptor = Example3.getDescriptor()
    assertThat(fileDescriptor.outerClassSimpleName)
      .isEqualTo(ClassSimpleName(Example3::class.simpleName!!))
  }

  @Test
  fun outerClassSimpleName_hasOuterClassNameConflict() {
    val fileDescriptor = HasOuterClassNameConflictOuterClass.getDescriptor()
    assertThat(fileDescriptor.fileName.name).isEqualTo("has_outer_class_name_conflict")
    assertThat(fileDescriptor.outerClassSimpleName)
      .isEqualTo(ClassSimpleName(HasOuterClassNameConflictOuterClass::class.simpleName!!))
  }

  @Test
  fun outerClassSimpleName_hasHyphenInFileName() {
    val fileDescriptor = ProtoFileWithHyphen.getDescriptor()
    assertThat(fileDescriptor.fileName.name).isEqualTo("proto-file-with-hyphen")
    assertThat(fileDescriptor.outerClassSimpleName)
      .isEqualTo(ClassSimpleName(ProtoFileWithHyphen::class.simpleName!!))
  }

  @Test
  fun outerClassSimpleName_hasNestedClassNameConflict() {
    val fileDescriptor = HasNestedClassNameConflictOuterClass.getDescriptor()
    assertThat(fileDescriptor.fileName.name).isEqualTo("has_nested_class_name_conflict")
    assertThat(fileDescriptor.outerClassSimpleName)
      .isEqualTo(ClassSimpleName(HasNestedClassNameConflictOuterClass::class.simpleName!!))
  }

  @Test
  fun outerClassSimpleName_hasExplicitOuterClassName() {
    val fileDescriptor = MyExplicitOuterClassName.getDescriptor()
    assertThat(fileDescriptor.fileName.name).isEqualTo("has_explicit_outer_class_name")
    assertThat(fileDescriptor.outerClassSimpleName)
      .isEqualTo(ClassSimpleName(MyExplicitOuterClassName::class.simpleName!!))
  }
}
