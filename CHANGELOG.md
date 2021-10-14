## Change Log

### 1.2.0

#### Feature

 - Support StatusException in CoroutineContextServerInterceptor (#249)
 - Defer writing headers until the first message stanza is sent (#275)
 - Open context val in order to allow overriding (#287)
 - Restore metadata support to generated clients (#268)

#### Fixes

 - Fix application name in client (#284)
 - Mark deprecated service methods with @Deprecated. (#264)

### 1.1.0

#### Features

- Removed `grpc-kotlin-stub-lite` (#234)
  - The `grpc-kotlin-stub` library no longer depends on `grpc-protobuf` or `protobuf-java-util` (#234), so your project will need
  to include protobuf dependencies itself. For examples, see:
  [examples/stub/build.gradle.kts](examples/stub/build.gradle.kts),
  [examples/stub-lite/build.gradle.kts](examples/stub-lite/build.gradle.kts), or
  [examples/stub-android/build.gradle.kts](examples/stub-android/build.gradle.kts)
  - The `grpc-kotlin-stub-lite` library no longer exists, instead use `grpc-kotlin-stub`.
  - `grpc-kotlin-stub` now exports the `javax.annotation:javax.annotation-api` dependency, so you can drop it from your project's explicitly listed dependencies.
 - Added support for proto3 optional fields (#218)
 - Added `SERVICE_NAME` constant (#236)

#### Fixes

 - Updated to latest grpc version for ARM compatibility (#244)
 - Improved examples organization (#183)
