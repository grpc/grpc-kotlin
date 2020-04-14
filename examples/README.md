gRPC Kotlin Example
-------------------

1. Build the example:
    ```
    ../gradlew installDist
    ```

1. Start the Hello World server:
    ```
    build/install/examples/bin/hello-world-server
    ```

1. In another terminal, run the Hello World client:
    ```
    build/install/examples/bin/hello-world-client
    ```
    You should see: `Greeter client received: Hello world`

1. Start the Route Guide Server:
    ```
    build/install/examples/bin/route-guide-server
    ```

1. In another terminal, run the Hello World client:
    ```
    build/install/examples/bin/route-guide-client
    ```
    You should see a stream of routing points.
