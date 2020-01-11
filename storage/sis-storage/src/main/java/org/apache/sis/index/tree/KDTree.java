/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.index.tree;


/**
 * Base class of <var>k</var>-dimensional tree index. For <var>k</var>=2, this is a {@link QuadTree}.
 * For <var>k</var>=3, this is an {@code Octree}. Higher dimensions are also accepted.
 *
 * <h2>Thread-safety</h2>
 * This class is not thread-safe when the tree content is modified. But if the tree is kept unmodified
 * after construction, then multiple read operations in concurrent threads are safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param  <E>  the type of elements stored in this tree.
 *
 * @since 1.1
 * @module
 */
abstract class KDTree<E> {
    /**
     * Creates an initially empty tree.
     */
    KDTree() {
    }
}
