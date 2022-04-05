/*
 * Copyright 2020 Google LLC
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
package io.grpc.kotlin.generator.protoc.util.graph

import com.google.common.annotations.Beta
import com.google.common.base.Preconditions.checkArgument
import com.google.common.graph.Graph
import io.grpc.kotlin.generator.protoc.util.sort.PartialOrdering
import io.grpc.kotlin.generator.protoc.util.sort.TopologicalSort.sortLexicographicallyLeast

@Beta
object TopologicalSortGraph {
    fun <N> topologicalOrdering(graph: Graph<N>): List<N> {
        checkArgument(graph.isDirected, "Cannot get topological ordering of an undirected graph.")
        val partialOrdering: PartialOrdering<N> = object : PartialOrdering<N> {
            override fun getPredecessors(element: N): Set<N> = element?.let {
                graph.predecessors(it)
            } ?: emptySet()
        }
        return sortLexicographicallyLeast(graph.nodes(), partialOrdering)
    }
}
