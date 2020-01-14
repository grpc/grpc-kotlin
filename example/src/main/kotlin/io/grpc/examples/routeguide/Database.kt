package io.grpc.examples.routeguide;

import com.google.common.io.ByteSource
import com.google.common.io.Resources
import com.google.protobuf.util.JsonFormat

internal fun defaultFeatureSource(): ByteSource =
  Resources.asByteSource(Resources.getResource("example/resources/io/grpc/examples/routeguide/route_guide_db.json"))

internal fun ByteSource.parseJsonFeatures(): List<Feature> =
  asCharSource(Charsets.UTF_8)
    .openBufferedStream()
    .use { reader ->
      FeatureDatabase.newBuilder().apply {
        JsonFormat.parser().merge(reader, this)
      }.build().featureList
    }
