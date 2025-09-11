plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.protobuf) apply false
}

tasks.create("assemble").dependsOn(":server:installDist")
