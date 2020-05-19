package io.grpc.kotlin.generator.protoc

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Tests for [ProtoMethodName]. */
@RunWith(JUnit4::class)
class ProtoMethodNameTest {
  @Test
  fun toMemberSimpleNameWithSingleUnderscore(){
    assertThat(ProtoMethodName("say_hello").toMemberSimpleName())
      .isEqualTo(MemberSimpleName("sayHello"))
  }

  @Test
  fun toMemberSimpleNameWithMultipleUnderscores(){
    assertThat(ProtoMethodName("say_hello_again").toMemberSimpleName())
      .isEqualTo(MemberSimpleName("sayHelloAgain"))
  }

  @Test
  fun toMemberSimpleNameWithRecommendedNamingStyle(){
    assertThat(ProtoMethodName("SayHello").toMemberSimpleName())
      .isEqualTo(MemberSimpleName("sayHello"))
  }
}