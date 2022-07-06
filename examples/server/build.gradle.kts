plugins {
    application
    kotlin("jvm")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

dependencies {
    implementation(project(":stub"))
    runtimeOnly("io.grpc:grpc-netty:${rootProject.ext["grpcVersion"]}")

    testImplementation(kotlin("test-junit"))
    testImplementation("io.grpc:grpc-testing:${rootProject.ext["grpcVersion"]}")
}

tasks.register<JavaExec>("HelloWorldServer") {
    dependsOn("classes")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("io.grpc.examples.helloworld.HelloWorldServerKt")
}

tasks.register<JavaExec>("RouteGuideServer") {
    dependsOn("classes")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("io.grpc.examples.routeguide.RouteGuideServerKt")
}

tasks.register<JavaExec>("AnimalsServer") {
    dependsOn("classes")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("io.grpc.examples.animals.AnimalsServerKt")
}

val helloWorldServerStartScripts = tasks.register<CreateStartScripts>("helloWorldServerStartScripts") {
    mainClass.set("io.grpc.examples.helloworld.HelloWorldServerKt")
    applicationName = "hello-world-server"
    outputDir = tasks.named<CreateStartScripts>("startScripts").get().outputDir
    classpath = tasks.named<CreateStartScripts>("startScripts").get().classpath
}

val routeGuideServerStartScripts = tasks.register<CreateStartScripts>("routeGuideServerStartScripts") {
    mainClass.set("io.grpc.examples.routeguide.RouteGuideServerKt")
    applicationName = "route-guide-server"
    outputDir = tasks.named<CreateStartScripts>("startScripts").get().outputDir
    classpath = tasks.named<CreateStartScripts>("startScripts").get().classpath
}

val animalsServerStartScripts = tasks.register<CreateStartScripts>("animalsServerStartScripts") {
    mainClass.set("io.grpc.examples.animals.AnimalsServerKt")
    applicationName = "animals-server"
    outputDir = tasks.named<CreateStartScripts>("startScripts").get().outputDir
    classpath = tasks.named<CreateStartScripts>("startScripts").get().classpath
}

tasks.named("startScripts") {
    dependsOn(helloWorldServerStartScripts)
    dependsOn(routeGuideServerStartScripts)
    dependsOn(animalsServerStartScripts)
}

tasks.withType<Test> {
    useJUnit()

    testLogging {
        events = setOf(org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED, org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED, org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED)
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true
    }
}
