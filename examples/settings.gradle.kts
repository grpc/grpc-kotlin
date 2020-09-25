rootProject.name = "grpc-kotlin-examples"

// when running the assemble task, ignore the android subproject
if (startParameter.taskRequests.find { it.args.contains("assemble") } == null) {
    include("protos", "stub", "client", "server", "stub-lite", "android")
} else {
    include("protos", "stub", "client", "server")
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        jcenter()
        google()
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.android.application") {
                useModule("com.android.tools.build:gradle:${requested.version}")
            }
        }
    }
}
