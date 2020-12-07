# gRPC Kotlin examples

## Examples

This directory contains several Kotlin gRPC examples. You can find detailed
instructions for building and running the two main examples from the [grpc.io
Kotlin/JVM][] pages:

- **Greeter** ("hello world"): for details, see [Quick start][].
- **Route guide**: for details, see [Basics tutorial][]

Instructions for the remaining examples are provided below.

## File organization

The example sources are organized into the following top-level folders:

- [protos][]: `.proto` files (shared across examples)
- [stub][]: regular Java & Kotlin stub artifacts from [protos][]
- [stub-lite][]: lite Java & Kotlin stub artifacts from [protos][]
- [stub-android][]: Android-compatible Java & Kotlin stub artifacts from [protos][]
- [client](client): Kotlin clients based on regular [stub][] artifacts
- [server](server): Kotlin servers based on regular [stub][] artifacts
- [native-client](native-client) : GraalVM Native Image clients based [stub-lite][]
- [android](android): Kotlin Android app based on [stub-android][]

## Instructions for running other examples

- <details>
  <summary>Multiple-services animals example</summary>

  Start the server:

  ```sh
  ./gradlew :server:AnimalsServer
  ```

  In another console, run the client against the "dog", "pig", and "sheep" services:

  ```sh
  ./gradlew :client:AnimalsClient --args=dog
  ./gradlew :client:AnimalsClient --args=pig
  ./gradlew :client:AnimalsClient --args=sheep
  ```

- <details>
  <summary>GraalVM native image example</summary>

  Start the native server:

  ```sh
  ./gradlew :native-server:nativeImage
  native-server/build/graal/hello-world-server
  ```

  In another console, create the native image client and run it:

  ```sh
  ./gradlew :native-client:nativeImage
  native-client/build/graal/hello-world-client
  ```

- <details>
  <summary>Android example</summary>

  Start the server:

  ```sh
  ./gradlew :server:HelloWorldServer
  ```

  Run the Client:

  1. [Download Android Command Line Tools](https://developer.android.com/studio)

  1. Install the SDK:

      ```sh
      mkdir android-sdk
      cd android-sdk
      unzip PATH_TO_SDK_ZIP/sdk-tools-linux-VERSION.zip
      tools/bin/sdkmanager --update
      tools/bin/sdkmanager "platforms;android-30" "build-tools;30.0.2" "extras;google;m2repository" "extras;android;m2repository"
      tools/bin/sdkmanager --licenses
      ```

  1. Set an env var pointing to the `android-sdk`

      ```sh
      export ANDROID_SDK_ROOT=PATH_TO_SDK/android-sdk
      ```

  1. Run the build from this project's dir:

      ```sh
      ./gradlew :android:build
      ```

  1. You can either run on an emulator or a physical device and you can either
      connect to the server running on your local machine, or connect to a server
      you deployed on the cloud.

      * Emulator + Local Server:

        * From the command line:

          ```sh
          ./gradlew :android:installDebug
          ```

        * From Android Studio / IntelliJ, navigate to
          `android/src/main/kotlin/io/grpc/examples/helloworld` and right-click on
          `MainActivity` and select `Run`.

      * Physical Device + Local Server:

        * From the command line:

          1. [Setup adb](https://developer.android.com/studio/run/device)
          1. `./gradlew :android:installDebug -PserverUrl=http://YOUR_MACHINE_IP:50051/`

        * From Android Studio / IntelliJ:

          1. Create a `gradle.properties` file in your root project directory containing:

              ```sh
              serverUrl=http://YOUR_MACHINE_IP:50051/
              ```

          1. Navigate to `android/src/main/kotlin/io/grpc/examples/helloworld` and right-click on `MainActivity` and select `Run`.

      * Emulator or Physical Device + Cloud:

        * From the command line:

          1. [setup adb](https://developer.android.com/studio/run/device)
          1. `./gradlew :android:installDebug -PserverUrl=https://YOUR_SERVER/`

        * From Android Studio / IntelliJ:

          1. Create a `gradle.properties` file in your root project directory containing:

              ```sh
              serverUrl=https://YOUR_SERVER/
              ```

          1. Navigate to `android/src/main/kotlin/io/grpc/examples/helloworld` and right-click on `MainActivity` and select `Run`.

[Basics tutorial]: https://grpc.io/docs/languages/kotlin/basics/
[grpc.io Kotlin/JVM]: https://grpc.io/docs/languages/kotlin/
[protos]: protos
[Quick start]: https://grpc.io/docs/languages/kotlin/quickstart/
[stub]: stub
[stub-android]: stub-android
[stub-lite]: stub-lite
