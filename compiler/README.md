gRPC Kotlin Codegen Plugin for Protobuf Compiler
================================================

This generates the Kotlin interfaces out of the service definition from a`.proto` file. It works with the Protobuf Compiler (`protoc`) and uses a `protoc` plugin to generate Kotlin wrappers for the generated Java classes.
> Note: You can use the gRPC Kotlin compiler without using the protoc Kotlin compiler, but these instructions assume you want to use both together.

### Build Tool Plugins

Usually this compiler is used via a build tool plugin, like in Gradle, Maven, etc.

- #### Gradle

For Gradle, include the [protobuf plugin](https://github.com/google/protobuf-gradle-plugin) with at least version `0.8.13`, like:
```
plugins {
    id("com.google.protobuf") version "YOUR_PROTOBUF_PLUGIN_VERSION"
}
```

Add dependencies on `grpc-kotlin-stub` and protobuf libraries like:
```
dependencies {
    implementation("io.grpc:grpc-kotlin-stub:YOUR_GRPC_KOTLIN_VERSION")
    implementation("io.grpc:grpc-protobuf:YOUR_GRPC_VERSION")
    implementation("com.google.protobuf:protobuf-kotlin:YOUR_PROTOBUF_VERSION")
}
```

Finally, setup the protobuf plugin:
```
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:YOUR_PROTOBUF_VERSION"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:YOUR_GRPC_VERSION"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:YOUR_GRPC_KOTLIN_VERSION:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
                id("grpckt")
            }
            it.builtins {
                id("kotlin")
            }
        }
    }
}
```

- #### Maven

For Maven, include the [protobuf plugin](https://www.xolstice.org/protobuf-maven-plugin/) and configure it to use the
`protoc-gen-grpc-kotlin` compiler, starting by adding `properties` for the versions of the plugins

```
<properties>
    <!-- This is just an example, don't mind the versions -->
    <java.version></java.version> <!-- you should already have this line -->
    <kotlin.version>1.5.31</kotlin.version> <!-- you should already have this line -->

    <!- the version is the lower of the ones found in these two links
    (they should always be synced, but double checking is better than being stuck on an error):
    https://search.maven.org/search?q=a:protoc-gen-grpc-kotlin
    https://search.maven.org/search?q=a:grpc-kotlin-stub -->
    <grpc.kotlin.version>1.2.0</grpc.kotlin.version>

    <!-- The version is the latest found here: https://search.maven.org/artifact/io.grpc/grpc-protobuf -->
    <!-- IMPORTANT: currently we support max 1.39.0 -->
    <java.grpc.version>1.39.0</java.grpc.version>

    <!-- the version is the latest found here: https://search.maven.org/search?q=a:protobuf-kotlin -->
    <!-- IMPORTANT: currently we support max 3.18.1 -->
    <protobuf.version>3.18.1</protobuf.version>
</properties>
```

Follow by adding the dependencies:
```
<dependency>
  <groupId>io.grpc</groupId>
  <artifactId>grpc-kotlin-stub</artifactId>
  <version>${grpc.kotlin.version}</version>
</dependency>
<dependency>
  <groupId>io.grpc</groupId>
  <artifactId>grpc-protobuf</artifactId>
  <version>${java.grpc.version}</version>
</dependency>
<dependency>
  <groupId>com.google.protobuf</groupId>
  <artifactId>protobuf-kotlin</artifactId>
  <version>${protobuf.version}</version>
</dependency>
```

Add `os-maven-plugin` as first plugin, if not already added:
```
<plugins>
    <plugin>
        <groupId>kr.motd.maven</groupId>
        <artifactId>os-maven-plugin</artifactId>
        <version>1.7.0</version> <!-- consider handling this version via properties as well -->
        <executions>
            <execution>
                <phase>initialize</phase>
                <goals>
                    <goal>detect</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
    <!-- (other plugins...) -->
</plugins>
```

And finally add the build job for the proto as last plugin:

```
<plugins>
    <!-- other plugins --> 
    <plugin>
        <groupId>org.xolstice.maven.plugins</groupId>
        <artifactId>protobuf-maven-plugin</artifactId>
        <version>0.6.1</version>
        <executions>
            <execution>
                <id>compile</id>
                <goals>
                    <goal>compile</goal>
                    <goal>compile-custom</goal>
                </goals>
                <configuration>
                    <protocArtifact>com.google.protobuf:protoc:${protobuf.version}:exe:${os.detected.classifier}</protocArtifact>
                    <pluginId>grpc-java</pluginId>
                    <pluginArtifact>io.grpc:protoc-gen-grpc-java:${java.grpc.version}:exe:${os.detected.classifier}</pluginArtifact>
                    <protocPlugins>
                        <protocPlugin>
                            <id>grpc-kotlin</id>
                            <groupId>io.grpc</groupId>
                            <artifactId>protoc-gen-grpc-kotlin</artifactId>
                            <version>${grpc.kotlin.version}</version>
                            <classifier>jdk8</classifier>
                            <mainClass>io.grpc.kotlin.generator.GeneratorRunner</mainClass>
                        </protocPlugin>
                    </protocPlugins>
                </configuration>
            </execution>
            <execution>
                <id>compile-kt</id>
                <goals>
                    <goal>compile-custom</goal>
                </goals>
                <configuration>
                    <protocArtifact>com.google.protobuf:protoc:${protobuf.version}:exe:${os.detected.classifier}</protocArtifact>
                    <outputDirectory>${project.build.directory}/generated-sources/protobuf/kotlin</outputDirectory>
                    <pluginId>kotlin</pluginId>
                </configuration>
            </execution>
        </executions>
    </plugin>
</plugins>
```

### Manual `protoc` Usage

If you want to use this compiler from `protoc` directly, you can
[grab the latest jar distribution from Maven Central](https://repo1.maven.org/maven2/io/grpc/protoc-gen-grpc-kotlin/).
Since the jar is not executable and `protoc` plugins must be executables, you will need to first create an executable
that just runs that `jar`, for example create a file named `protoc-gen-grpc-kotlin.sh` with the following contents:
```
#!/usr/bin/env sh

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
java -jar $DIR/protoc-gen-grpc-kotlin-SOME_VERSION-jdk8.jar "$@
```

Then make that file executable:
```
chmod +x protoc-gen-grpc-kotlin.sh
```

Now you can use the `protoc-gen-grpc-kotlin` as a plugin for `protoc`, like:
```
protoc --plugin=protoc-gen-grpckt=protoc-gen-grpc-kotlin.sh \
  --grpckt_out="$OUTPUT_FILE" --proto_path="$DIR_OF_PROTO_FILE" "$PROTO_FILE"
```

## Developer Info

* Linux, Mac OS X with Clang, or Windows with MSYS2
* Java 8 or up
* [Protobuf](https://github.com/google/protobuf) 3.0.0-beta-3 or up

### Compiling and testing the codegen

To compile the plugin:

```
./gradlew :compiler:build
```

To test the plugin with the compiler:

```
./gradlew :compiler:test
```

You will see a `PASS` if the test succeeds.

To create a `compiler` start script:

```
./gradlew :compiler:installDist
```

To compile a proto file and generate Kotlin interfaces out of the service definitions:

```
protoc --plugin=protoc-gen-grpckt=compiler/build/install/compiler/bin/compiler \
  --grpckt_out="$OUTPUT_FILE" --proto_path="$DIR_OF_PROTO_FILE" "$PROTO_FILE"
```

For example:
```
protoc --plugin=protoc-gen-grpckt=compiler/build/install/compiler/bin/compiler \
  --grpckt_out=compiler/build --proto_path=compiler/src/test/proto/helloworld \
  compiler/src/test/proto/helloworld/helloworld.proto
```

### Installing the codegen to Maven local repository

This will compile a codegen and put it under your `~/.m2/repository`. This
will make it available to any build tool that pulls codegens from Maven
repositories.

```
$ ../gradlew publishToMavenLocal
```
