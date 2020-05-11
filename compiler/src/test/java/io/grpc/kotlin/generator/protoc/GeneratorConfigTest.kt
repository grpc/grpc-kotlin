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
import com.google.protobuf.Descriptors.FileDescriptor
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asClassName
import io.grpc.kotlin.generator.protoc.testproto.Example3
import io.grpc.kotlin.generator.protoc.testproto.HasOuterClassNameConflictOuterClass
import io.grpc.kotlin.generator.protoc.testproto.MyExplicitOuterClassName
import io.grpc.testing.ServiceNameConflictsWithFileOuterClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import testing.ImplicitJavaPackage
import kotlin.reflect.KClass

/** Tests for [GeneratorConfig]. */
@RunWith(JUnit4::class)
class GeneratorConfigTest {
  private val javaPackagePolicy = JavaPackagePolicy.OPEN_SOURCE
  private val defaultConfig = GeneratorConfig(javaPackagePolicy, false)

  private fun generateFile(block: Declarations.Builder.() -> Unit): String {
    return FileSpec.builder("com.google", "FooBar.kt")
      .apply {
        declarations(block).writeAllAtTopLevel(this)
      }
      .build()
      .toString()
      .trim()
  }

  @Test
  fun funSpecBuilder() {
    with(GeneratorConfig(javaPackagePolicy, aggressiveInlining = false)) {
      assertThat(
        generateFile {
          addFunction(funSpecBuilder(MemberSimpleName("fooBar")).build())
        }
      ).isEqualTo(
        """
        package com.google

        fun fooBar() {
        }
        """.trimIndent()
      )
    }
    with(GeneratorConfig(javaPackagePolicy, aggressiveInlining = true)) {
      assertThat(
        generateFile {
          addFunction(funSpecBuilder(MemberSimpleName("fooBar")).build())
        }
      ).isEqualTo(
        """
        package com.google

        inline fun fooBar() {
        }
        """.trimIndent()
      )
    }
  }

  @Test
  fun getterBuilder() {
    with(GeneratorConfig(javaPackagePolicy, aggressiveInlining = false)) {
      assertThat(
        generateFile {
          addProperty(
            PropertySpec.builder("someProp", INT)
              .getter(getterBuilder().addStatement("return 1").build())
              .build()
          )
        }
      ).isEqualTo(
        """
        package com.google

        import kotlin.Int

        val someProp: Int
          get() = 1
        """.trimIndent()
      )
    }

    with(GeneratorConfig(javaPackagePolicy, aggressiveInlining = true)) {
      assertThat(
        generateFile {
          addProperty(
            PropertySpec.builder("someProp", INT)
              .getter(getterBuilder().addStatement("return 1").build())
              .build()
          )
        }
      ).isEqualTo(
        """
        package com.google

        import kotlin.Int

        val someProp: Int
          inline get() = 1
        """.trimIndent()
      )
    }
  }

  @Test
  fun setterBuilder() {
    val param = ParameterSpec.builder("newValue", INT).build()
    with(GeneratorConfig(javaPackagePolicy, aggressiveInlining = false)) {
      assertThat(
        generateFile {
          addProperty(
            PropertySpec.builder("someProp", INT)
              .mutable(true)
              .setter(setterBuilder().addParameter(param).build())
              .build()
          )
        }
      ).isEqualTo(
        """
        package com.google

        import kotlin.Int

        var someProp: Int
          set(newValue) {
          }
        """.trimIndent()
      )
    }

    with(GeneratorConfig(javaPackagePolicy, aggressiveInlining = true)) {
      assertThat(
        generateFile {
          addProperty(
            PropertySpec.builder("someProp", INT)
              .mutable(true)
              .setter(setterBuilder().addParameter(param).build())
              .build()
          )
        }
      ).isEqualTo(
        """
        package com.google

        import kotlin.Int

        var someProp: Int
          inline set(newValue) {
          }
        """.trimIndent()
      )
    }
  }

  private val fileDescriptorToTopLevelClass: List<Pair<FileDescriptor, KClass<out Any>>> =
    listOf(
      Example3.getDescriptor() to Example3::class,
      MyExplicitOuterClassName.getDescriptor() to MyExplicitOuterClassName::class,
      HasOuterClassNameConflictOuterClass.getDescriptor() to
        HasOuterClassNameConflictOuterClass::class,
      ImplicitJavaPackage.getDescriptor() to ImplicitJavaPackage::class,
      ServiceNameConflictsWithFileOuterClass.getDescriptor() to
        ServiceNameConflictsWithFileOuterClass::class
    )

  @Test
  fun javaPackage() {
    with(defaultConfig) {
      for ((descriptor, clazz) in fileDescriptorToTopLevelClass) {
        assertThat(javaPackage(descriptor)).isEqualTo(PackageScope(clazz.java.`package`.name))
      }
    }
  }

  @Test
  fun outerClass() {
    with(defaultConfig) {
      for ((descriptor, clazz) in fileDescriptorToTopLevelClass) {
        assertThat(descriptor.outerClass()).isEqualTo(clazz.asClassName())
      }
    }
  }

  @Test
  fun enumClass() {
    with(defaultConfig) {
      assertThat(Example3.ExampleEnum.getDescriptor().enumClass())
        .isEqualTo(Example3.ExampleEnum::class.asClassName())
    }
  }

  @Test
  fun messageClass() {
    with(defaultConfig) {
      assertThat(Example3.ExampleMessage.getDescriptor().messageClass())
        .isEqualTo(Example3.ExampleMessage::class.asClassName())
    }
  }
}
