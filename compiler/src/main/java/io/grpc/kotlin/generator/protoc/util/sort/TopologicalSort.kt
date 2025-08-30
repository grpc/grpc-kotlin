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

package io.grpc.kotlin.generator.protoc.util.sort

import java.util.PriorityQueue

/**
 * Topological sorting. The algorithm is an adaptation of the topological sorting algorithm
 * described in TAOCP Section 2.2.3, with a little bit of extra state to provide a stable sort. The
 * constructed ordering is guaranteed to be deterministic.
 *
 * The elements to be sorted should implement the standard [Object.hashCode] and [ ][Object.equals]
 * methods.
 */
object TopologicalSort {
  /**
   * Does a topological sorting. If there is a tie in the topographical ordering, it is resolved in
   * favor of the order of the original input list. Thus, an arbitrary "lexicographical topological
   * ordering" can be achieved by first lexicographically sorting the input list according to your
   * own criteria and then calling this method.
   *
   * A high-level sketch of toplogical sort from Wikipedia:
   * (http://en.wikipedia.org/wiki/Topological_sorting)
   *
   * ```
   * L ← Empty list that will contain the sorted elements
   * S ← Set of all nodes with no incoming edges
   * while S is non-empty do
   *     remove a node n from S
   *     add n to tail of L
   *     for each node m with an edge e from n to m do
   *         remove edge e from the graph
   *         if m has no other incoming edges then
   *             insert m into S
   * if graph has edges then
   *     return error (graph has at least one cycle)
   * else
   *     return L (a topologically sorted order)
   * ```
   *
   * We extend the basic algorithm to traverse `S` in a particular order based on the original order
   * of the elements to enforce a deterministic result (lexicographically based on the order of
   * elements in the original input list).
   *
   * @param elements a mutable list of elements to be sorted.
   * @param order the partial order between elements.
   * @throws CyclicalGraphException if the graph is cyclical or any predecessor is not present in
   *   the input list.
   */
  fun <T> sortLexicographicallyLeast(elements: Collection<T>, order: PartialOrdering<T>): List<T> {
    val internalElements = internalizeElements(elements, order)
    val sortedElements: MutableList<InternalElement<T>> = mutableListOf()

    // The "S" set of the above algorithm pseudocode is represented here by readyElements.
    val readyElements = PriorityQueue<InternalElement<T>>()
    internalElements.filterTo(readyElements) { it.predecessorCount == 0 }
    while (!readyElements.isEmpty()) {
      val currentElement: InternalElement<T> = readyElements.remove()
      sortedElements.add(currentElement)
      for (successor in currentElement.successors) {
        successor.predecessorCount--
        if (successor.predecessorCount == 0) {
          readyElements.add(successor)
        }
      }
    }
    if (sortedElements.size != elements.size) {
      val elementsInCycle = internalElements.filter { it.predecessorCount > 0 }.map { it.element }
      throw CyclicalGraphException(
        "Cyclical graphs can not be topologically sorted.",
        elementsInCycle
      )
    }
    return sortedElements.map { it.element }
  }

  /**
   * Internalizes the elements of the input list, representing the dependency structure to make
   * topological sort easier to compute.
   *
   * @param elements the list to be sorted.
   * @param order the partial ordering used to find the predecessors of each element in the list.
   * @return a list of [InternalElement]s initialized with dependency structure.
   */
  private fun <T> internalizeElements(
    elements: Iterable<T>,
    order: PartialOrdering<T>
  ): List<InternalElement<T>> {
    val internalElements: MutableList<InternalElement<T>> = mutableListOf()
    // Subtle: due to the potential for duplicates in elements, we need to map every element to a
    // list of the corresponding InternalElements.
    val internalElementsByValue: MutableMap<T, MutableList<InternalElement<T>>> = mutableMapOf()
    for ((index, element) in elements.withIndex()) {
      val internalElement = InternalElement(element, index)
      internalElements.add(internalElement)
      internalElementsByValue.getOrPut(element) { mutableListOf() }.add(internalElement)
    }
    for (internalElement in internalElements) {
      for (predecessor in order.getPredecessors(internalElement.element)) {
        val internalPredecessors: List<InternalElement<T>>? = internalElementsByValue[predecessor]
        if (internalPredecessors != null) {
          for (internalPredecessor in internalPredecessors) {
            internalPredecessor.successors.add(internalElement)
            internalElement.predecessorCount++
          }
        } else {
          // Subtle: we must leave the predecessor count incremented here to properly
          // be able to report CyclicGraphExceptions. In this case, a predecessor was
          // reported by the order relation, but the predecessor was not a member of
          // elements.
          internalElement.predecessorCount++
        }
      }
    }
    return internalElements
  }

  /**
   * This exception is thrown whenever the input to our topological sort algorithm is a cyclical
   * graph, or the predecessor relation refers to elements that have not been presented as input to
   * the topological sort.
   */
  class CyclicalGraphException(
    message: String, // not parameterized because exceptions can't be parameterized
    /**
     * A list of the elements that are part of the cycle, as well as elements that are greater than
     * the elements in the cycle, according to the partial ordering. The elements in this list are
     * not in a meaningful order.
     */
    val elementsInCycle: List<*>
  ) : RuntimeException(message)

  /**
   * To bundle an element with a mutable structure of the dependency graph.
   *
   * Each [InternalElement] counts how many predecessors it has left. Rather than keep a list of
   * predecessors, we reverse the relation so that it's easy to navigate to the successors when an
   * [InternalElement] is selected for sorting.
   *
   * This maintains a `originalIndex` to allow a "stable" sort based on the original position in the
   * input list.
   */
  private data class InternalElement<T>(val element: T, val originalIndex: Int) :
    Comparable<InternalElement<T>> {
    val successors: MutableList<InternalElement<T>> = mutableListOf()
    var predecessorCount = 0

    override operator fun compareTo(other: InternalElement<T>): Int =
      originalIndex.compareTo(other.originalIndex)
  }
}
