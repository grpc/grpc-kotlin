## Change Log

### 1.4.1

#### Changes

* fix(ServerCalls): Fix regression in Status cause for exceptions thrown by implementations by @andrewparmet in https://github.com/grpc/grpc-kotlin/pull/456
* Bazel: update to use guava 32.0.1 consistently. by @brettchabot in https://github.com/grpc/grpc-kotlin/pull/436
* Bump composeVersion from 1.5.1 to 1.5.2 in /examples by @dependabot in https://github.com/grpc/grpc-kotlin/pull/438
* Bump org.gradle.test-retry from 1.5.5 to 1.5.6 by @dependabot in https://github.com/grpc/grpc-kotlin/pull/439
* Bump androidx.activity:activity-compose from 1.7.2 to 1.8.0 in /examples by @dependabot in https://github.com/grpc/grpc-kotlin/pull/441
* Bump composeVersion from 1.5.2 to 1.5.3 in /examples by @dependabot in https://github.com/grpc/grpc-kotlin/pull/440
* Bump org.jetbrains.dokka from 1.9.0 to 1.9.10 by @dependabot in https://github.com/grpc/grpc-kotlin/pull/446
* Bump org.jlleitschuh.gradle.ktlint from 11.6.0 to 11.6.1 in /examples by @dependabot in https://github.com/grpc/grpc-kotlin/pull/443
* Bump com.google.guava:guava from 32.1.2-jre to 32.1.3-jre by @dependabot in https://github.com/grpc/grpc-kotlin/pull/445
* FIXED: Missing double quotes in compiler docs leading to error. by @prodbyola in https://github.com/grpc/grpc-kotlin/pull/453
* Bump androidx.activity:activity-compose from 1.8.0 to 1.8.1 in /examples by @dependabot in https://github.com/grpc/grpc-kotlin/pull/457
* Bump composeVersion from 1.5.3 to 1.5.4 in /examples by @dependabot in https://github.com/grpc/grpc-kotlin/pull/447
* Bump jvm from 1.9.10 to 1.9.20 in /examples by @dependabot in https://github.com/grpc/grpc-kotlin/pull/450
* Bump org.junit.jupiter:junit-jupiter-engine from 5.10.0 to 5.10.1 by @dependabot in https://github.com/grpc/grpc-kotlin/pull/454

#### New Contributors
* @brettchabot made their first contribution in https://github.com/grpc/grpc-kotlin/pull/436
* @prodbyola made their first contribution in https://github.com/grpc/grpc-kotlin/pull/453
* @andrewparmet made their first contribution in https://github.com/grpc/grpc-kotlin/pull/456

**Full Changelog**: https://github.com/grpc/grpc-kotlin/compare/v1.4.0...v1.4.1


### 1.4.0

#### Changes

* Bump org.gradle.test-retry from 1.5.2 to 1.5.4 by @dependabot in https://github.com/grpc/grpc-kotlin/pull/413
* Bump uraimo/run-on-arch-action from 2.0.5 to 2.5.1 by @dependabot in https://github.com/grpc/grpc-kotlin/pull/404
* Bump actions/cache from 1 to 3 by @dependabot in https://github.com/grpc/grpc-kotlin/pull/406
* Bump actions/checkout from 1 to 3 by @dependabot in https://github.com/grpc/grpc-kotlin/pull/408
* Bump actions/setup-java from 2 to 3 by @dependabot in https://github.com/grpc/grpc-kotlin/pull/409
* Bump org.junit.jupiter:junit-jupiter-engine from 5.8.2 to 5.10.0 by @dependabot in https://github.com/grpc/grpc-kotlin/pull/405
* Bump com.google.jimfs:jimfs from 1.2 to 1.3.0 by @dependabot in https://github.com/grpc/grpc-kotlin/pull/407
* Bump com.google.protobuf from 0.8.18 to 0.9.4 by @dependabot in https://github.com/grpc/grpc-kotlin/pull/412
* Bump org.jetbrains.dokka from 1.6.21 to 1.8.20 by @dependabot in https://github.com/grpc/grpc-kotlin/pull/416
* Bump jvm from 1.9.0 to 1.9.10 in /examples by @dependabot in https://github.com/grpc/grpc-kotlin/pull/410
* Bump com.google.truth.extensions:truth-proto-extension from 1.1.3 to 1.1.5 by @dependabot in https://github.com/grpc/grpc-kotlin/pull/419
* Bump io.github.gradle-nexus.publish-plugin from 1.1.0 to 1.3.0 by @dependabot in https://github.com/grpc/grpc-kotlin/pull/418
* Bump org.mockito:mockito-core from 4.5.1 to 4.11.0 by @dependabot in https://github.com/grpc/grpc-kotlin/pull/422
* Bump com.google.protobuf:protobuf-gradle-plugin from 0.8.18 to 0.9.4 by @dependabot in https://github.com/grpc/grpc-kotlin/pull/421
* Bump org.jetbrains.dokka from 1.8.20 to 1.9.0 by @dependabot in https://github.com/grpc/grpc-kotlin/pull/424
* Bump actions/checkout from 3 to 4 by @dependabot in https://github.com/grpc/grpc-kotlin/pull/426
* Bump composeVersion from 1.5.0 to 1.5.1 in /examples by @dependabot in https://github.com/grpc/grpc-kotlin/pull/427
* Bump org.gradle.test-retry from 1.5.4 to 1.5.5 by @dependabot in https://github.com/grpc/grpc-kotlin/pull/430
* version bumps and related fixes by @jamesward in https://github.com/grpc/grpc-kotlin/pull/417

## New Contributors
* @brettchabot made their first contribution in https://github.com/grpc/grpc-kotlin/pull/417
* @dependabot made their first contribution in https://github.com/grpc/grpc-kotlin/pull/413

**Full Changelog**: https://github.com/grpc/grpc-kotlin/compare/v1.3.1...v1.4.0


### 1.3.1

#### Changes

* bump coroutines to 1.6.2 by @jamesward in https://github.com/grpc/grpc-kotlin/pull/340
* bump versions by @jamesward in https://github.com/grpc/grpc-kotlin/pull/346
* Support generating lite protos with Bazel by @Kernald in https://github.com/grpc/grpc-kotlin/pull/350
* Recover cancellation when close responses flow by @akandratovich in https://github.com/grpc/grpc-kotlin/pull/344
* Fixed Examples by Updating to Gradle 7.5.1 and Java 11 by @handstandsam in https://github.com/grpc/grpc-kotlin/pull/362
* Provide action mnemonics for kt_jvm_proto_library implementation. by @plobsing in https://github.com/grpc/grpc-kotlin/pull/368
* Migrate run command to work on a windows machine by @jlyon12345 in https://github.com/grpc/grpc-kotlin/pull/375
* Enable sourceSets in the stub example project by @nkhoshini in https://github.com/grpc/grpc-kotlin/pull/376
* bumps and build cleanup by @jamesward in https://github.com/grpc/grpc-kotlin/pull/377
* add foojar resolver convention plugin - fixes #391 by @jamesward in https://github.com/grpc/grpc-kotlin/pull/392
* [README][fix] Adding missing character on readme by @andrsGutirrz in https://github.com/grpc/grpc-kotlin/pull/393
* Replace deprecated command with environment file by @jongwooo in https://github.com/grpc/grpc-kotlin/pull/395
* fix(ServerCalls): Ensure failure cause is propagated in Status to interceptors by @zakhenry in https://github.com/grpc/grpc-kotlin/pull/400

## New Contributors
* @akandratovich made their first contribution in https://github.com/grpc/grpc-kotlin/pull/344
* @handstandsam made their first contribution in https://github.com/grpc/grpc-kotlin/pull/362
* @plobsing made their first contribution in https://github.com/grpc/grpc-kotlin/pull/368
* @jlyon12345 made their first contribution in https://github.com/grpc/grpc-kotlin/pull/375
* @nkhoshini made their first contribution in https://github.com/grpc/grpc-kotlin/pull/376
* @andrsGutirrz made their first contribution in https://github.com/grpc/grpc-kotlin/pull/393
* @jongwooo made their first contribution in https://github.com/grpc/grpc-kotlin/pull/395
* @zakhenry made their first contribution in https://github.com/grpc/grpc-kotlin/pull/400

**Full Changelog**: https://github.com/grpc/grpc-kotlin/compare/v1.3.0...v1.3.1

### 1.3.0

#### Changes
* Pin Bazel version by @Kernald in https://github.com/grpc/grpc-kotlin/pull/322
* Use proper Maven targets rather than legacy compat ones by @Kernald in https://github.com/grpc/grpc-kotlin/pull/321
* bump versions by @jamesward in https://github.com/grpc/grpc-kotlin/pull/325
* add server examples - fixes #317 by @jamesward in https://github.com/grpc/grpc-kotlin/pull/319
* bump versions by @jamesward in https://github.com/grpc/grpc-kotlin/pull/329
* send headers when failure is null by @sangyongchoi in https://github.com/grpc/grpc-kotlin/pull/335
* Upgrade coroutines version to 1.6.1. by @lowasser in https://github.com/grpc/grpc-kotlin/pull/327

## New Contributors
* @Kernald made their first contribution in https://github.com/grpc/grpc-kotlin/pull/322
* @sangyongchoi made their first contribution in https://github.com/grpc/grpc-kotlin/pull/335

**Full Changelog**: https://github.com/grpc/grpc-kotlin/compare/v1.2.1...v1.3.0

### 1.2.1

#### Changes
* ServerCalls: cancel only the request's Job by @goj in https://github.com/grpc/grpc-kotlin/pull/303
* Update README.md by @Tails128 in https://github.com/grpc/grpc-kotlin/pull/304
* Load rules_kotlin rules from jvm.bzl by @fmeum in https://github.com/grpc/grpc-kotlin/pull/300
* Depend on tools in the exec configuration by @fmeum in https://github.com/grpc/grpc-kotlin/pull/301
* Remove hardcoded references to @bazel by @aragos in https://github.com/grpc/grpc-kotlin/pull/305
* Compose UI by @jamesward in https://github.com/grpc/grpc-kotlin/pull/296
* add an integration test by @jamesward in https://github.com/grpc/grpc-kotlin/pull/310
* Add kotlinx-coroutines-core-jvm dependency by @bu3 in https://github.com/grpc/grpc-kotlin/pull/311

#### New Contributors
* @goj made their first contribution in https://github.com/grpc/grpc-kotlin/pull/303
* @Tails128 made their first contribution in https://github.com/grpc/grpc-kotlin/pull/304
* @fmeum made their first contribution in https://github.com/grpc/grpc-kotlin/pull/300
* @bu3 made their first contribution in https://github.com/grpc/grpc-kotlin/pull/311

**Full Changelog**: https://github.com/grpc/grpc-kotlin/compare/v1.2.0...v1.2.1

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
