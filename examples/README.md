TODO











# gRPC Kotlin Android Sample

## Run Server Locally:

```sh
./gradlew -t :server:run
```

## Deploy and Run Server on Cloud Run:

[![Run on Google Cloud](https://deploy.cloud.run/button.png)](https://deploy.cloud.run/?cloudshell_context=cloudrun-gbp)

## Run the Client:

1. [Download Android Command Line Tools](https://developer.android.com/studio)

 1. Install the SDK:

    ```sh
    mkdir android-sdk
    cd android-sdk
    unzip PATH_TO_SDK_ZIP/sdk-tools-linux-VERSION.zip
    tools/bin/sdkmanager --update
    tools/bin/sdkmanager "platforms;android-29" "build-tools;29.0.3" "extras;google;m2repository" "extras;android;m2repository"
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
