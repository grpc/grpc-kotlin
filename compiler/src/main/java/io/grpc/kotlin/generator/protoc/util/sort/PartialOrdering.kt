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

/**
 * The interface that imposes a partial order on elements in a DAG to be topologically sorted.
 *
 * @author Okhtay Ilghami (okhtay@google.com)
 */
interface PartialOrdering<T> {
    /**
     * Returns nodes that are considered "less than" `element` for purposes of a [ ]. Transitive predecessors do not need to be included.
     *
     *
     * For example, if `getPredecessors(a)` includes `b` and `getPredecessors(b)`
     * includes `c`, it is not necessary to include `c` in `getPredecessors(a)`.
     * `c` is not a "direct" predecessor of `a`.
     */
    fun getPredecessors(element: T): Set<T>
}
