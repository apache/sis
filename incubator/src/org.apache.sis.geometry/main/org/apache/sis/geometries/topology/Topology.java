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
package org.apache.sis.geometries.topology;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * Root type of the topological primitives and complexes.
 *
 * <p>Topology describes how objects are connected, independently of their coordinates. A topological
 * complex turns geometric computations, such as containment, adjacency, boundaries or network tracing,
 * into combinatorial ones. Its primitives usually have a geometric realization, but a complex may also
 * describe a logical structure which has none.</p>
 *
 * <p>This type is not a {@link org.apache.sis.geometries.Geometry}. The topological boundary and the
 * geometric boundary are two different operations, kept separate to avoid any confusion, even though
 * they agree on any geometric realization of the topology.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 10.1.2
 */
@UML(identifier="Topology", specification=ISO_19107)
public interface Topology {

    /**
     * Returns {@code true} if this topology contains nothing.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>An empty topology has a {@linkplain #getDimension() dimension} of −1.</li>
     *   <li>An empty topology is realized only by empty geometries.</li>
     *   <li>Every other attribute and operation behaves consistently on an empty topology.</li>
     * </ul>
     *
     * <p>An empty topology is meant as a temporary state, for example until a transaction completes.</p>
     *
     * @return whether this topology is empty.
     *
     * @see ISO 19107:2019 - 10.1.2.2
     */
    @UML(identifier="isEmpty", specification=ISO_19107)
    boolean isEmpty();

    /**
     * Topological dimension of this object: 0 for nodes, 1 for edges, 2 for faces and 3 for solids.
     * It depends only on the type of this object, and is −1 for an empty topology.
     *
     * @return topological dimension of this object.
     *
     * @see ISO 19107:2019 - 10.1.2.3
     */
    @UML(identifier="dimension", specification=ISO_19107)
    int getDimension();

    /**
     * Oriented primitives of the next lower dimension which bound this object.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>The dimension of the boundary is the dimension of this object minus one.</li>
     *   <li>The orientations agree with the geometric realization of this object.</li>
     * </ul>
     *
     * <p>This list is usually a valid {@link Expression}, except that a primitive may appear twice
     * with opposite orientations, as a dangling edge which has the same face on both sides. Those two
     * terms would cancel out in the expression.</p>
     *
     * @return boundary of this object, empty if none.
     *
     * @see ISO 19107:2019 - 10.1.2.4
     */
    @UML(identifier="boundary", specification=ISO_19107)
    List<? extends DirectedTopo> getBoundary();

    /**
     * Oriented primitives of the next higher dimension having this object on their boundary.
     *
     * <p>This list is usually a valid {@link Expression}, except for an object lying on the boundary of a
     * closed object, such as the node at both ends of a closed edge. That object appears twice, with
     * opposite orientations, and would cancel out in the expression.</p>
     *
     * @return co-boundary of this object, empty if none.
     *
     * @see ISO 19107:2019 - 10.1.2.5
     */
    @UML(identifier="coBoundary", specification=ISO_19107)
    List<? extends DirectedTopo> getCoBoundary();

    /**
     * Primitives forming the interior of this object within its {@linkplain #getMaximalComplex() maximal complex}.
     * A primitive is its own interior. The interior of a complex is made of its elements which are not on its
     * boundary.
     *
     * @return interior of this object.
     *
     * @see ISO 19107:2019 - 10.1.2.6
     */
    @UML(identifier="interior", specification=ISO_19107)
    Set<Primitive> getInterior();

    /**
     * Primitives forming the exterior of this object within its {@linkplain #getMaximalComplex() maximal complex}:
     * those which are neither in the interior nor on the boundary of this object.
     *
     * @return exterior of this object.
     *
     * @see ISO 19107:2019 - 10.1.2.7
     */
    @UML(identifier="exterior", specification=ISO_19107)
    Set<Primitive> getExterior();

    /**
     * Union of the {@linkplain #getInterior() interior} and of the {@linkplain #getBoundary() boundary} of this object.
     *
     * <p>The default implementation adds the interior, then the primitives on the boundary,
     * then those on their boundaries down to the nodes.</p>
     *
     * @return closure of this object.
     *
     * @see ISO 19107:2019 - 10.1.2.8
     */
    @UML(identifier="closure", specification=ISO_19107)
    default Set<Primitive> getClosure() {
        final Set<Primitive> closure = new LinkedHashSet<>(getInterior());
        final Deque<DirectedTopo> pending = new ArrayDeque<>(getBoundary());
        DirectedTopo next;
        while ((next = pending.poll()) != null) {
            if (closure.add(next.getTopo())) {
                pending.addAll(next.getBoundary());
            }
        }
        return closure;
    }

    /**
     * The largest complex containing this object. Every object is contained in exactly one maximal complex.
     *
     * @return maximal complex containing this object.
     *
     * @see ISO 19107:2019 - 10.1.2.9
     */
    @UML(identifier="maximalComplex", specification=ISO_19107)
    Complex getMaximalComplex();

}
