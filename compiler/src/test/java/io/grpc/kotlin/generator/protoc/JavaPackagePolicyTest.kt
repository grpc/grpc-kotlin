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
import io.grpc.kotlin.generator.protoc.testproto.Example3
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import testing.ImplicitJavaPackage

/** Tests for [JavaPackagePolicy]. */
@RunWith(JUnit4::class)
class JavaPackagePolicyTest {
  @Test
  fun explicitJavaPackageGoogle() {
    with(JavaPackagePolicy.OPEN_SOURCE) {
      assertThat(javaPackage(Example3.getDescriptor().toProto()))
        .isEqualTo(PackageScope(Example3::class.java.`package`.name))
    }
  }

  @Test
  fun explicitJavaPackageExternal() {
    with(JavaPackagePolicy.OPEN_SOURCE) {
      assertThat(javaPackage(Example3.getDescriptor().toProto()))
        .isEqualTo(PackageScope(Example3::class.java.`package`.name))
    }
  }

  @Test
  fun implicitJavaPackageGoogle() {
    with(JavaPackagePolicy.OPEN_SOURCE) {
      assertThat(javaPackage(ImplicitJavaPackage.getDescriptor().toProto()))
        .isEqualTo(
          PackageScope("testing")
        )
    }
  }

  @Test
  fun implicitJavaPackageExternal() {
    with(JavaPackagePolicy.OPEN_SOURCE) {
      assertThat(javaPackage(ImplicitJavaPackage.getDescriptor().toProto()))
        .isEqualTo(PackageScope("testing"))
    }
  }
}
