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
package org.apache.sis.geometries;

import org.apache.sis.geometries.internal.shared.DefaultEmpty;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * The empty set, the unique geometry containing no position at all.
 *
 * <p>There is only one empty geometry, because set-theoretic equality makes any empty set equal to
 * any other one. It may however be represented either by an instance of this interface or by any
 * other {@link Geometry} whose {@link Geometry#isEmpty()} flag is set; both behave identically.
 * Since the empty set has no defined dimension and no defined coordinate system, it belongs to no
 * primitive class in particular.</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>The topological dimension is −1.</li>
 *   <li>{@link Geometry#isEmpty()}, {@link Geometry#isCycle()}, {@link Geometry#isSimple()} and
 *       {@link Geometry#isValid()} are all {@code true}.</li>
 *   <li>The boundary, closure, buffer, convex hull, envelope and transform of the empty geometry
 *       are the empty geometry.</li>
 *   <li>The centroid and the representative point of the empty geometry are undefined.</li>
 *   <li>The distance from the empty geometry to any geometry, including itself, is +∞.</li>
 *   <li>{@code A ∩ ∅ = ∅}, {@code A ∪ ∅ = A}, {@code A − ∅ = A} and {@code ∅ − A = ∅}.</li>
 *   <li>The empty geometry is a subset of every geometry, and a superset of itself only.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 6.4.10
 */
@UML(identifier="Empty", specification=ISO_19107)
public sealed interface Empty extends Geometry
        permits DefaultEmpty
{

    /**
     * Well-known text keyword of this geometry type.
     */
    static final String TYPE = "EMPTY";

    /**
     * Returns {@code true} since this geometry is the empty set.
     *
     * @see ISO 19107:2019 - 6.4.4.2
     */
    @Override
    default boolean isEmpty() {
        return true;
    }

    /**
     * Returns {@value #TYPE}.
     *
     * @see ISO 19107:2019 - 6.4.4.23
     */
    @Override
    default String getGeometryType() {
        return TYPE;
    }

    @Override
    default int getTopologicDimension() {
        return -1;
    }

}
