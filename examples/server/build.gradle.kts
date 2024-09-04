plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    // 자바 애플리케이션을 docker 이미지로 패키징하는 플러그인
    // https://jh-labs.tistory.com/509
    alias(libs.plugins.jib)
}

kotlin {
    jvmToolchain(8)
}

dependencies {
    implementation(project(":stub"))

    runtimeOnly(libs.grpc.netty)

    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.grpc.testing)
}

// HelloWorldServer, RouteGuideServer, AnimalsServer 실행을 위한 task 설정
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

// HelloWorldServer, RouteGuideServer, AnimalsServer 실행을 위한 스크립트 설정
val helloWorldServerStartScripts =
    tasks.register<CreateStartScripts>("helloWorldServerStartScripts") {
        mainClass.set("io.grpc.examples.helloworld.HelloWorldServerKt")
        applicationName = "hello-world-server"
        outputDir = tasks.named<CreateStartScripts>("startScripts").get().outputDir
        classpath = tasks.named<CreateStartScripts>("startScripts").get().classpath
    }

val routeGuideServerStartScripts =
    tasks.register<CreateStartScripts>("routeGuideServerStartScripts") {
        mainClass.set("io.grpc.examples.routeguide.RouteGuideServerKt")
        applicationName = "route-guide-server"
        outputDir = tasks.named<CreateStartScripts>("startScripts").get().outputDir
        classpath = tasks.named<CreateStartScripts>("startScripts").get().classpath
    }

val animalsServerStartScripts =
    tasks.register<CreateStartScripts>("animalsServerStartScripts") {
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

// 테스트 실행 시 로그 설정
tasks.withType<Test> {
    useJUnit()

    testLogging {
        events =
            setOf(
                org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
                org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED,
                org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
            )
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true
    }
}

jib {
    // docker 컨테이너의 메인 클래스 설정
    container {
        mainClass = "io.grpc.examples.helloworld.HelloWorldServerKt"
    }
}
