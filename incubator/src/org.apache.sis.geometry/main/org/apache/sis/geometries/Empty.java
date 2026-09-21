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
import org.apache.sis.geometries.operation.GeometryProcessor;
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

    /**
     * Returns −1: the empty set has no dimension.
     *
     * @see ISO 19107:2019 - 6.4.4.22
     */
    @Override
    default int getTopologicDimension() {
        return -1;
    }

    /**
     * Returns {@code true}: the boundary of the empty set is empty.
     *
     * @see ISO 19107:2019 - 6.4.4.14
     */
    @UML(identifier="isCycle", specification=ISO_19107)
    @Override
    default boolean isCycle() {
        return true;
    }

    /**
     * Returns {@code true}: the empty set has no position, therefore no anomalous position.
     *
     * @see ISO 19107:2019 - 6.4.4.15
     */
    @UML(identifier="isSimple", specification=ISO_19107)
    @Override
    default boolean isSimple() {
        return true;
    }

    /**
     * Returns {@code true}: the empty set is always a valid geometry.
     *
     * @see ISO 19107:2019 - 6.4.4.16
     */
    @UML(identifier="isValid", specification=ISO_19107)
    @Override
    default boolean isValid() {
        return true;
    }

    /**
     * Returns {@code this}: the boundary of the empty set is empty.
     *
     * @see ISO 19107:2019 - 6.4.4.7
     */
    @UML(identifier="boundary", specification=ISO_19107)
    @Override
    default Geometry boundary() {
        return this;
    }

    /**
     * Returns {@code this}: the empty set is topologically closed.
     *
     * @see ISO 19107:2019 - 6.4.4.9
     */
    @UML(identifier="closure", specification=ISO_19107)
    @Override
    default Geometry getClosure() {
        return this;
    }

    /**
     * Returns {@code null}: the centroid of the empty set is undefined.
     *
     * @see ISO 19107:2019 - 6.4.4.8
     */
    @UML(identifier="centroid", specification=ISO_19107)
    @Override
    default Point getCentroid() {
        return null;
    }

    /**
     * Returns {@code null}: the empty set has no interior position.
     *
     * @see ISO 19107:2019 - 6.4.4.19
     */
    @UML(identifier="representativePoint", specification=ISO_19107)
    @Override
    default Point getRepresentativePoint() {
        return null;
    }

}
