## Change Log

### 1.2.0

#### Changes
- Restore metadata support to generated clients by @lowasser in https://github.com/grpc/grpc-kotlin/pull/268
- fixed application name in client by @Arashi5 in https://github.com/grpc/grpc-kotlin/pull/284
- Mark deprecated service methods with `@Deprecated`. by @lowasser in https://github.com/grpc/grpc-kotlin/pull/264
- Open context val in order to allow overriding by @bjoernmayer in https://github.com/grpc/grpc-kotlin/pull/287
- Defer writing headers until the first message stanza is sent by @rwbergstrom in https://github.com/grpc/grpc-kotlin/pull/275
- Support StatusException in CoroutineContextServerInterceptor. by @hovinen in https://github.com/grpc/grpc-kotlin/pull/249
- Cleanup by @jamesward in https://github.com/grpc/grpc-kotlin/pull/293
- kotlin protos by @jamesward in https://github.com/grpc/grpc-kotlin/pull/266

#### New Contributors
- @Arashi5 made their first contribution in https://github.com/grpc/grpc-kotlin/pull/284
- @bjoernmayer made their first contribution in https://github.com/grpc/grpc-kotlin/pull/287
- @rwbergstrom made their first contribution in https://github.com/grpc/grpc-kotlin/pull/275
- @hovinen made their first contribution in https://github.com/grpc/grpc-kotlin/pull/249

**Full Changelog**: https://github.com/grpc/grpc-kotlin/compare/v1.1.0...v1.2.0

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
