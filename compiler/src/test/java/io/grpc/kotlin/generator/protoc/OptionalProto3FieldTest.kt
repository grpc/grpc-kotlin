package io.grpc.kotlin.generator.protoc

import com.google.common.truth.Truth.assertThat
import io.grpc.testing.TestProto3Optional
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Tests for supporting proto3 optional fields. */
@RunWith(JUnit4::class)
class OptionalProto3FieldTest {

  @Test
  fun compileProto3OptionalField() {
    assertThat(TestProto3Optional.OptionalProto3.getDescriptor().findFieldByName("optional_field"))
      .isNotNull()
  }

  @Test
  fun generateHasOptionalFieldMethod() {
    assertThat(TestProto3Optional.OptionalProto3::class.java.getMethod("hasOptionalField"))
      .isNotNull()
  }

}
