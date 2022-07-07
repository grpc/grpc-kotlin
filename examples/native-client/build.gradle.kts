plugins {
    application
    kotlin("jvm")
    id("com.palantir.graal") version "0.12.0"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    implementation(project(":stub-lite"))
    runtimeOnly("io.grpc:grpc-okhttp:${rootProject.ext["grpcVersion"]}")
}

application {
    mainClass.set("io.grpc.examples.helloworld.HelloWorldClientKt")
}

// todo: add graalvm-config-create task
// ./gradlew :native-client:install
// JAVA_HOME=~/.gradle/caches/com.palantir.graal/22.1.0/11/graalvm-ce-java11-22.1.0 JAVA_OPTS=-agentlib:native-image-agent=config-output-dir=native-client/src/graal native-client/build/install/native-client/bin/native-client

graal {
    graalVersion("22.1.0")
    javaVersion("11")
    mainClass(application.mainClass.get())
    outputName("hello-world")
    option("--verbose")
    option("--no-fallback")
    option("-H:+ReportExceptionStackTraces")
    option("-H:+PrintClassInitialization")
    option("-H:ReflectionConfigurationFiles=src/graal/reflect-config.json")
}
