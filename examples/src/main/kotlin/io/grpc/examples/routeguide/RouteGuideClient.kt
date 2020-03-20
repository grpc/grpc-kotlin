/*
 * Copyright 2020 gRPC authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.examples.routeguide

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.examples.routeguide.RouteGuideGrpcKt.RouteGuideCoroutineStub
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.random.nextLong

class RouteGuideClient private constructor(
  private val channel: ManagedChannel
) : Closeable {
  private val random = Random(314159)
  private val stub = RouteGuideCoroutineStub(channel)

  constructor(
    channelBuilder: ManagedChannelBuilder<*>,
    dispatcher: CoroutineDispatcher
  ) : this(channelBuilder.executor(dispatcher.asExecutor()).build())

  override fun close() {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
  }

  fun getFeature(latitude: Int, longitude: Int) = runBlocking {
    println("*** GetFeature: lat=$latitude lon=$longitude")

    val request = point(latitude, longitude)
    val feature = stub.getFeature(request)

    if (feature.exists()) {
      println("Found feature called \"${feature.name}\" at ${feature.location.toStr()}")
    } else {
      println("Found no feature at ${request.toStr()}")
    }
  }

  fun listFeatures(lowLat: Int, lowLon: Int, hiLat: Int, hiLon: Int) = runBlocking {
    println("*** ListFeatures: lowLat=$lowLat lowLon=$lowLon hiLat=$hiLat liLon=$hiLon")

    val request = Rectangle.newBuilder()
      .setLo(point(lowLat, lowLon))
      .setHi(point(hiLat, hiLon))
      .build()
    var i = 1
    stub.listFeatures(request).collect { feature ->
      println("Result #${i++}: $feature")
    }
  }

  fun recordRoute(points: Flow<Point>) = runBlocking {
    println("*** RecordRoute")
    val summary = stub.recordRoute(points)
    println("Finished trip with ${summary.pointCount} points.")
    println("Passed ${summary.featureCount} features.")
    println("Travelled ${summary.distance} meters.")
    val duration = summary.elapsedTime.seconds
    println("It took $duration seconds.")
  }

  fun generateRoutePoints(features: List<Feature>, numPoints: Int): Flow<Point> = flow {
    for (i in 1..numPoints) {
      val feature = features.random(random)
      println("Visiting point ${feature.location.toStr()}")
      emit(feature.location)
      delay(timeMillis = random.nextLong(500L..1500L))
    }
  }

  fun routeChat() = runBlocking {
    println("*** RouteChat")
    val requests = generateOutgoingNotes()
    stub.routeChat(requests).collect { note ->
      println("Got message \"${note.message}\" at ${note.location.toStr()}")
    }
    println("Finished RouteChat")
  }

  private fun generateOutgoingNotes(): Flow<RouteNote> = flow {
    val notes = listOf(
      RouteNote.newBuilder().apply {
        message = "First message"
        location = point(0, 0)
      }.build(),
      RouteNote.newBuilder().apply {
        message = "Second message"
        location = point(0, 0)
      }.build(),
      RouteNote.newBuilder().apply {
        message = "Third message"
        location = point(10000000, 0)
      }.build(),
      RouteNote.newBuilder().apply {
        message = "Fourth message"
        location = point(10000000, 10000000)
      }.build(),
      RouteNote.newBuilder().apply {
        message = "Last message"
        location = point(0, 0)
      }.build()
    )
    for (note in notes) {
      println("Sending message \"${note.message}\" at ${note.location.toStr()}")
      emit(note);
      delay(500)
    }
  }

}

fun main(args: Array<String>) {
  val features = defaultFeatureSource().parseJsonFeatures()
  Executors.newFixedThreadPool(10).asCoroutineDispatcher().use { dispatcher ->
    val channel = ManagedChannelBuilder.forAddress("localhost", 8980).usePlaintext()
    RouteGuideClient(channel, dispatcher).use {
      it.getFeature(409146138, -746188906)
      it.getFeature(0, 0)
      it.listFeatures(400000000, -750000000, 420000000, -730000000)
      it.recordRoute(it.generateRoutePoints(features, 10))
      it.routeChat()
    }
  }
}

private fun point(lat: Int, lon: Int): Point =
  Point.newBuilder()
    .setLatitude(lat)
    .setLongitude(lon)
    .build()
