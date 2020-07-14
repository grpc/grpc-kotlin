plugins {
    application
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":common"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.7")
    runtimeOnly("io.grpc:grpc-netty:${rootProject.ext["grpcVersion"]}")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_7
}

application {
    mainClassName = "ServerKt"
}
