plugins {
    application
    alias(libs.plugins.kotlin.jvm)
}

// JVM 버전 설정
kotlin {
    jvmToolchain(8)
}

dependencies {
    implementation(project(":stub"))
    runtimeOnly(libs.grpc.netty)
}

// HelloWorldClient, RouteGuideClient, AnimalsClient 실행을 위한 task 설정
tasks.register<JavaExec>("HelloWorldClient") {
    dependsOn("classes")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("io.grpc.examples.helloworld.HelloWorldClientKt") // mainClass 설정
}

tasks.register<JavaExec>("RouteGuideClient") {
    dependsOn("classes")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("io.grpc.examples.routeguide.RouteGuideClientKt")
}

tasks.register<JavaExec>("AnimalsClient") {
    dependsOn("classes")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("io.grpc.examples.animals.AnimalsClientKt")
}

// HelloWorldClient, RouteGuideClient, AnimalsClient 실행을 위한 task 설정
val helloWorldClientStartScripts =
    tasks.register<CreateStartScripts>("helloWorldClientStartScripts") {
        mainClass.set("io.grpc.examples.helloworld.HelloWorldClientKt")
        applicationName = "hello-world-client"
        outputDir = tasks.named<CreateStartScripts>("startScripts").get().outputDir
        classpath = tasks.named<CreateStartScripts>("startScripts").get().classpath
    }

val routeGuideClientStartScripts =
    tasks.register<CreateStartScripts>("routeGuideClientStartScripts") {
        mainClass.set("io.grpc.examples.routeguide.RouteGuideClientKt")
        applicationName = "route-guide-client"
        outputDir = tasks.named<CreateStartScripts>("startScripts").get().outputDir
        classpath = tasks.named<CreateStartScripts>("startScripts").get().classpath
    }

val animalsClientStartScripts =
    tasks.register<CreateStartScripts>("animalsClientStartScripts") {
        mainClass.set("io.grpc.examples.animals.AnimalsClientKt")
        applicationName = "animals-client"
        outputDir = tasks.named<CreateStartScripts>("startScripts").get().outputDir
        classpath = tasks.named<CreateStartScripts>("startScripts").get().classpath
    }

tasks.named("startScripts") {
    dependsOn(helloWorldClientStartScripts)
    dependsOn(routeGuideClientStartScripts)
    dependsOn(animalsClientStartScripts)
}
