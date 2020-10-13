plugins {
    application
    kotlin("jvm")
    id("com.palantir.graal") version "0.7.1"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":stub-lite"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.8")

    runtimeOnly("io.grpc:grpc-okhttp:${rootProject.ext["grpcVersion"]}")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClass.set("io.grpc.examples.helloworld.HelloWorldClientKt")
}

// todo: add graalvm-config-create task
// JAVA_HOME=~/.gradle/caches/com.palantir.graal/20.2.0/8/graalvm-ce-java8-20.2.0 JAVA_OPTS=-agentlib:native-image-agent=config-output-dir=native-client/src/graal native-client/build/install/native-client/bin/native-client

graal {
    graalVersion("20.2.0")
    mainClass(application.mainClassName)
    outputName("hello-world")
    option("--verbose")
    option("--no-server")
    option("--no-fallback")
    option("-H:+ReportExceptionStackTraces")
    option("-H:+TraceClassInitialization")
    option("-H:+PrintClassInitialization")
    option("-H:ReflectionConfigurationFiles=src/graal/reflect-config.json")
}
