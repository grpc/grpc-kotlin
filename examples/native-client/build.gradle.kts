plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.palantir.graal)
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(project(":stub-lite"))

    runtimeOnly(libs.grpc.okhttp)
}

application {
    mainClass.set("io.grpc.examples.helloworld.HelloWorldClientKt")
}

// todo: add graalvm-config-create task
// ./gradlew :native-client:install
// JAVA_HOME=~/.gradle/caches/com.palantir.graal/22.3.3/11/graalvm-ce-java11-22.3.3 JAVA_OPTS=-agentlib:native-image-agent=config-output-dir=native-client/src/main/resources/META-INF/native-image native-client/build/install/native-client/bin/native-client

graal {
    graalVersion("22.3.3")
    javaVersion("11")
    mainClass(application.mainClass.get())
    outputName("hello-world")
    option("--verbose")
    option("--no-fallback")
    option("-H:+ReportExceptionStackTraces")
    option("-H:+PrintClassInitialization")
}
