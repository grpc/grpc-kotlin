import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.ofSourceSet
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

val grpcVersion = "1.30.0"
val grpcKotlinVersion = "0.1.5" // CURRENT_GRPC_KOTLIN_VERSION
val protobufVersion = "3.12.2"
val coroutinesVersion = "1.3.9"

plugins {
    application
    kotlin("jvm") version "1.4.0"
    id("com.google.protobuf") version "0.8.12"
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
}

repositories {
    mavenLocal()
    google()
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("javax.annotation:javax.annotation-api:1.2")
    implementation("com.google.protobuf:protobuf-java-util:$protobufVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    runtimeOnly("io.grpc:grpc-netty-shaded:$grpcVersion")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:$grpcKotlinVersion"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc")
                id("grpckt")
            }
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClassName = "io.grpc.examples.helloworld.HelloWorldServerKt"
}

tasks.register<JavaExec>("HelloWorldServer") {
    dependsOn("classes")
    classpath = sourceSets["main"].runtimeClasspath
    main = "io.grpc.examples.helloworld.HelloWorldServerKt"
}

tasks.register<JavaExec>("HelloWorldClient") {
    dependsOn("classes")
    classpath = sourceSets["main"].runtimeClasspath
    main = "io.grpc.examples.helloworld.HelloWorldClientKt"
}

tasks.register<JavaExec>("RouteGuideServerKt") {
    dependsOn("classes")
    classpath = sourceSets["main"].runtimeClasspath
    main = "io.grpc.examples.routeguide.RouteGuideServerKt"
}

tasks.register<JavaExec>("RouteGuideClientKt") {
    dependsOn("classes")
    classpath = sourceSets["main"].runtimeClasspath
    main = "io.grpc.examples.routeguide.RouteGuideClientKt"
}

val helloWorldServerStartScripts = tasks.register<CreateStartScripts>("helloWorldServerStartScripts") {
    mainClassName = "io.grpc.examples.helloworld.HelloWorldServerKt"
    applicationName = "hello-world-server"
    outputDir = tasks.named<CreateStartScripts>("startScripts").get().outputDir
    classpath = tasks.named<CreateStartScripts>("startScripts").get().classpath
}

val helloWorldClientStartScripts = tasks.register<CreateStartScripts>("helloWorldClientStartScripts") {
    mainClassName = "io.grpc.examples.helloworld.HelloWorldClientKt"
    applicationName = "hello-world-client"
    outputDir = tasks.named<CreateStartScripts>("startScripts").get().outputDir
    classpath = tasks.named<CreateStartScripts>("startScripts").get().classpath
}

val routeGuideServerStartScripts = tasks.register<CreateStartScripts>("routeGuideServerStartScripts") {
    mainClassName = "io.grpc.examples.routeguide.RouteGuideServerKt"
    applicationName = "route-guide-server"
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
    dependsOn(helloWorldServerStartScripts)
    dependsOn(helloWorldClientStartScripts)
    dependsOn(routeGuideServerStartScripts)
    dependsOn(routeGuideClientStartScripts)
}
