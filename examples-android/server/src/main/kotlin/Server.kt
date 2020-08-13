import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.examples.helloworld.GreeterGrpcKt
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest

class Server constructor(private val port: Int) {
    val server: Server = ServerBuilder
            .forPort(port)
            .addService(HelloWorldService())
            .build()

    fun start() {
        server.start()
        println("Server started, listening on $port")
        Runtime.getRuntime().addShutdownHook(
                Thread {
                    println("*** shutting down gRPC server since JVM is shutting down")
                    this@Server.stop()
                    println("*** server shut down")
                }
        )
    }

    private fun stop() {
        server.shutdown()
    }

    fun blockUntilShutdown() {
        server.awaitTermination()
    }

    private class HelloWorldService : GreeterGrpcKt.GreeterCoroutineImplBase() {
        override suspend fun sayHello(request: HelloRequest) = HelloReply
                .newBuilder()
                .setMessage("hello, ${request.name}")
                .build()
    }
}

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 50051
    val server = Server(port)
    server.start()
    server.blockUntilShutdown()
}
