package io.grpc.examples.routeguide

import com.google.common.base.Stopwatch
import com.google.common.base.Ticker
import com.google.common.io.ByteSource
import com.google.protobuf.util.JavaTimeConversions
import io.grpc.Server
import io.grpc.ServerBuilder
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

/**
 * Kotlin adaptation of RouteGuideServer from the Java gRPC example.
 */
class RouteGuideServer private constructor(
  val port: Int,
  val server: Server
) {
  constructor(port: Int) : this(port, defaultFeatureSource())

  constructor(port: Int, featureData: ByteSource) :
  this(
    serverBuilder = ServerBuilder.forPort(port),
    port = port,
    features = featureData.parseJsonFeatures()
  )

  constructor(
    serverBuilder: ServerBuilder<*>,
    port: Int,
    features: Collection<Feature>
  ) : this(
    port = port,
    server = serverBuilder.addService(RouteGuideService(features)).build()
  )

  fun start() {
    server.start()
    println("Server started, listening on $port")
    Runtime.getRuntime().addShutdownHook(
      Thread {
        println("*** shutting down gRPC server since JVM is shutting down")
        this@RouteGuideServer.stop()
        println("*** server shut down")
      }
    )
  }

  fun stop() {
    server.shutdown()
  }

  fun blockUntilShutdown() {
    server.awaitTermination()
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      val port = 8980
      val server = RouteGuideServer(port)
      server.start()
      server.blockUntilShutdown()
    }
  }

  class RouteGuideService(
    val features: Collection<Feature>,
    val ticker: Ticker = Ticker.systemTicker()
  ) : RouteGuideGrpcKt.RouteGuideCoroutineImplBase() {
    private val routeNotes = ConcurrentHashMap<Point, MutableList<RouteNote>>()

    override suspend fun getFeature(request: Point): Feature {
      return features.find { it.location == request }
        ?: feature { location = request } // No feature was found, return an unnamed feature.
    }

    override suspend fun listFeatures(request: Rectangle, responses: SendChannel<Feature>) {
      for (feature in features) {
        if (feature.exists() && feature.location in request) {
          responses.send(feature)
        }
      }
    }

    override suspend fun recordRoute(requests: ReceiveChannel<Point>): RouteSummary {
      var pointCount = 0
      var featureCount = 0
      var distance = 0
      var previous: Point? = null
      val stopwatch = Stopwatch.createStarted(ticker)
      for (request in requests) {
        pointCount++
        if (getFeature(request).exists()) {
          featureCount++
        }
        if (previous != null) {
          distance += previous distanceTo request
        }
        previous = request
      }
      return routeSummary {
        this.pointCount = pointCount
        this.featureCount = featureCount
        this.distance = distance
        this.elapsedTime = JavaTimeConversions.toProtoDuration(stopwatch.elapsed())
      }
    }

    override suspend fun routeChat(
      requests: ReceiveChannel<RouteNote>,
      responses: SendChannel<RouteNote>
    ) {
      for (note in requests) {
        val notes: MutableList<RouteNote> = routeNotes.computeIfAbsent(note.location) {
          Collections.synchronizedList(mutableListOf<RouteNote>())
        }
        for (prevNote in notes.toTypedArray()) { // thread-safe snapshot
          responses.send(prevNote)
        }
        notes += note
      }
    }
  }
}
