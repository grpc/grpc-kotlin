plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
}

dependencies {
  implementation(project(":stub-android"))

  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.foundation.layout)
  implementation(libs.androidx.compose.material)
  implementation(libs.androidx.compose.runtime)
  implementation(libs.androidx.compose.ui)

  runtimeOnly(libs.grpc.okhttp)
}

kotlin { jvmToolchain(17) }

android {
  compileSdk = 34
  buildToolsVersion = "34.0.0"
  namespace = "io.grpc.examples.helloworld"

  defaultConfig {
    applicationId = "io.grpc.examples.hello"
    minSdk = 26
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"

    val serverUrl: String? by project
    if (serverUrl != null) {
      resValue("string", "server_url", serverUrl!!)
    } else {
      resValue("string", "server_url", "http://10.0.2.2:50051/")
    }
  }

  buildFeatures { compose = true }

  composeOptions { kotlinCompilerExtensionVersion = libs.androidx.compose.compiler.get().version }
}
