package io.grpc.examples.routeguide

import com.google.protobuf.util.JavaTimeConversions
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.examples.routeguide.RouteGuideGrpcKt.RouteGuideCoroutineStub
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.random.nextLong
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class RouteGuideClient private constructor(
  val channel: ManagedChannel,
  val stub: RouteGuideCoroutineStub,
  val printer: Printer
) : Closeable {
  val random = Random(314159)

  constructor(
    channelBuilder: ManagedChannelBuilder<*>,
    dispatcher: CoroutineDispatcher,
    printer: Printer
  ) : this(channelBuilder.executor(dispatcher.asExecutor()).build(), printer)

  constructor(
    channel: ManagedChannel,
    printer: Printer
  ) : this(channel, RouteGuideCoroutineStub(channel), printer)

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      val features = defaultFeatureSource().parseJsonFeatures()
      Executors.newFixedThreadPool(10).asCoroutineDispatcher().use { dispatcher ->
        RouteGuideClient(
          ManagedChannelBuilder.forAddress("localhost", 8980)
            .usePlaintext(),
          dispatcher,
          Printer.stdout
        ).use {
          it.getFeature(409146138, -746188906)
          it.getFeature(0, 0)
          it.listFeatures(400000000, -750000000, 420000000, -730000000)
          it.recordRoute(features, 10)
          it.routeChat()
        }
      }
    }
  }

  override fun close() {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
  }


  interface Printer {
    companion object {
      val stdout = object : Printer {
        override fun println(str: String) {
          System.out.println(str)
        }
      }
    }
    fun println(str: String)
  }

  fun getFeature(latitude: Int, longitude: Int) = runBlocking {
    printer.println("*** GetFeature: lat=$latitude lon=$longitude")

    val request = point {
      this.latitude = latitude
      this.longitude = longitude
    }
    val feature = stub.getFeature(request)

    if (feature.exists()) {
      printer.println("Found feature called \"${feature.name}\" at ${feature.location.toStr()}")
    } else {
      printer.println("Found no feature at ${request.toStr()}")
    }
  }

  fun listFeatures(lowLat: Int, lowLon: Int, hiLat: Int, hiLon: Int) = runBlocking {
    printer.println("*** ListFeatures: lowLat=$lowLat lowLon=$lowLon hiLat=$hiLat liLon=$hiLon")

    val request = rectangle {
      lo = point {
        latitude = lowLat
        longitude = lowLon
      }
      hi = point {
        latitude = lowLat
        longitude = lowLon
      }
    }
    var i = 1
    for (feature in stub.listFeatures(request)) {
      printer.println("Result #$i: $feature")
      i++
    }
  }

  fun recordRoute(features: List<Feature>, numPoints: Int) = runBlocking {
    printer.println("*** RecordRoute")
    val requests = Channel<Point>()
    val finish = launch {
      val summary = stub.recordRoute(requests)
      printer.println("Finished trip with ${summary.pointCount} points.")
      printer.println("Passed ${summary.featureCount} features.")
      printer.println("Travelled ${summary.distance} meters.")
      val duration = JavaTimeConversions.toJavaDuration(summary.elapsedTime).seconds
      printer.println("It took $duration seconds.")
    }
    for (i in 1..numPoints) {
      val feature = features.random(random)
      println("Visiting point ${feature.location.toStr()}")
      requests.send(feature.location)
      delay(timeMillis = random.nextLong(500L..1500L))
    }
    requests.close()
    finish.join()
  }

  fun routeChat() = runBlocking {
    printer.println("*** RouteChat")
    val requests = Channel<RouteNote>()
    val rpc = launch {
      val responses = stub.routeChat(requests)
      for (note in responses) {
        printer.println("Got message \"${note.message}\" at ${note.location.toStr()}")
      }
      println("Finished RouteChat")
    }
    val requestList = listOf(
      routeNote {
        message = "First message"
        location = point {
          latitude = 0
          longitude = 0
        }
      },
      routeNote {
        message = "Second message"
        location = point {
          latitude = 0
          longitude = 1
        }
      },
      routeNote {
        message = "Third message"
        location = point {
          latitude = 1
          longitude = 0
        }
      },
      routeNote {
        message = "Fourth message"
        location = point {
          latitude = 1
          longitude = 1
        }
      }
    )

    for (request in requestList) {
      printer.println("Sending message \"${request.message}\" at ${request.location.toStr()}")
      requests.send(request)
    }
    requests.close()
    rpc.join()
  }
}
