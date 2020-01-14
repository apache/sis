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

import java.util.function.Function;
import org.opengis.geometry.Envelope;


/**
 * Implementation of a point QuadTree Index.
 * See {@link KDTree} for a note about thread-safety.
 *
 * <p><b>References:</b></p>
 * Insertion algorithm implemented based on design of QuadTree index
 * in H. Samet, The Design and Analysis of Spatial Data Structures.
 * Massachusetts: Addison Wesley Publishing Company, 1989.
 *
 * @author  Chris Mattmann
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param  <E>  the type of elements stored in this tree.
 *
 * @since 0.1
 * @module
 */
public final class QuadTree<E> extends KDTree<E> {
    /**
     * Creates an initially empty QuadTree with the given capacity for each node.
     * The given envelope must be two-dimensional.
     *
     * @param  bounds     bounds of the region of data to be inserted in the QuadTree.
     * @param  evaluator  function computing a position for an arbitrary element of this tree.
     * @param  capacity   the capacity of each node.
     */
    public QuadTree(final Envelope bounds, final Function<? super E, double[]> evaluator, final int capacity) {
        super(bounds, evaluator, capacity);
    }
}
