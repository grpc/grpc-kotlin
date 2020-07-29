plugins {
    application
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":stub"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.7")

    api("com.google.protobuf:protobuf-java-util:${rootProject.ext["protobufVersion"]}")

    runtimeOnly("io.grpc:grpc-netty:${rootProject.ext["grpcVersion"]}")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.register<JavaExec>("HelloWorldClient") {
    dependsOn("classes")
    classpath = sourceSets["main"].runtimeClasspath
    main = "io.grpc.examples.helloworld.HelloWorldClientKt"
}

tasks.register<JavaExec>("RouteGuideClientKt") {
    dependsOn("classes")
    classpath = sourceSets["main"].runtimeClasspath
    main = "io.grpc.examples.routeguide.RouteGuideClientKt"
}

val helloWorldClientStartScripts = tasks.register<CreateStartScripts>("helloWorldClientStartScripts") {
    mainClassName = "io.grpc.examples.helloworld.HelloWorldClientKt"
    applicationName = "hello-world-client"
    outputDir = tasks.named<CreateStartScripts>("startScripts").get().outputDir
    classpath = tasks.named<CreateStartScripts>("startScripts").get().classpath
}

val routeGuideClientStartScripts = tasks.register<CreateStartScripts>("routeGuideClientStartScripts") {
    mainClassName = "io.grpc.examples.routeguide.RouteGuideClientKt"
    applicationName = "route-guide-client"
    outputDir = tasks.named<CreateStartScripts>("startScripts").get().outputDir
    classpath = tasks.named<CreateStartScripts>("startScripts").get().classpath
}

tasks.named("startScripts") {
    dependsOn(helloWorldClientStartScripts)
    dependsOn(routeGuideClientStartScripts)
}
