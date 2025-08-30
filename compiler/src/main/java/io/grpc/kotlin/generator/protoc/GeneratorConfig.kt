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

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.EnumDescriptor
import com.google.protobuf.Descriptors.FileDescriptor
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier

/**
 * Configuration for proto code generation, including settings on inlining and on the mapping
 * between protos and Java packages.
 */
data class GeneratorConfig(
  val javaPackagePolicy: JavaPackagePolicy,
  val aggressiveInlining: Boolean
) {

  private val inlineModifiers: Array<KModifier> =
    if (aggressiveInlining) arrayOf(KModifier.INLINE) else arrayOf()

  /** Generates a [FunSpec.Builder] with appropriate modifiers. */
  fun funSpecBuilder(name: MemberSimpleName): FunSpec.Builder =
    FunSpec.builder(name).addModifiers(*inlineModifiers)

  /** Generates a [FunSpec.Builder] for a getter with appropriate modifiers. */
  fun getterBuilder(): FunSpec.Builder = FunSpec.getterBuilder().addModifiers(*inlineModifiers)

  /** Generates a [FunSpec.Builder] for a setter with appropriate modifiers. */
  fun setterBuilder(): FunSpec.Builder = FunSpec.setterBuilder().addModifiers(*inlineModifiers)

  /** Returns the package associated with Java APIs for protos in the specified file. */
  fun javaPackage(fileDescriptor: FileDescriptor): PackageScope =
    javaPackagePolicy.javaPackage(fileDescriptor.toProto())

  // Helpers on FileDescriptor.

  /** Returns the fully qualified name of the outer class generated for this proto file. */
  fun FileDescriptor.outerClass(): ClassName = javaPackage(this).nestedClass(outerClassSimpleName)

  // Helpers on EnumDescriptor.

  /** Returns the fully qualified name of the JVM enum type generated for this proto enum. */
  fun EnumDescriptor.enumClass(): ClassName {
    val contType: Descriptor? = containingType
    return when {
      contType != null -> contType.messageClass().nestedClass(enumClassSimpleName)
      file.options.javaMultipleFiles -> javaPackage(file).nestedClass(enumClassSimpleName)
      else -> file.outerClass().nestedClass(enumClassSimpleName)
    }
  }

  // Helpers on Descriptor.

  /** Returns the fully qualified name of the JVM class generated for this message type. */
  fun Descriptor.messageClass(): ClassName {
    val contType: Descriptor? = containingType
    return when {
      contType != null -> contType.messageClass().nestedClass(messageClassSimpleName)
      file.options.javaMultipleFiles -> javaPackage(file).nestedClass(messageClassSimpleName)
      else -> file.outerClass().nestedClass(messageClassSimpleName)
    }
  }
}
