plugins {
    application
    kotlin("jvm")
    id("com.palantir.graal") version "0.7.2"
}

dependencies {
    implementation(project(":stub-lite"))
    runtimeOnly("io.grpc:grpc-okhttp:${rootProject.ext["grpcVersion"]}")
}

application {
    mainClass.set("io.grpc.examples.helloworld.HelloWorldClientKt")
}

// todo: add graalvm-config-create task
// JAVA_HOME=~/.gradle/caches/com.palantir.graal/20.2.0/8/graalvm-ce-java8-20.2.0 JAVA_OPTS=-agentlib:native-image-agent=config-output-dir=native-client/src/graal native-client/build/install/native-client/bin/native-client

graal {
    graalVersion("20.2.0")
    mainClass(application.mainClass.get())
    outputName("hello-world")
    option("--verbose")
    option("--no-server")
    option("--no-fallback")
    option("-H:+ReportExceptionStackTraces")
    option("-H:+TraceClassInitialization")
    option("-H:+PrintClassInitialization")
    option("-H:ReflectionConfigurationFiles=src/graal/reflect-config.json")
}
