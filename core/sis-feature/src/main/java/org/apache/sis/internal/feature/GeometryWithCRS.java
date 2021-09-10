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
package org.apache.sis.internal.feature;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.util.ArgumentChecks;


/**
 * A geometry wrapper with a field for CRS information. This base class is used when the geometry implementation
 * to wrap does not store CRS information by itself. See {@link GeometryWrapper} for more information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param  <G>  root class of geometry instances of the underlying library.
 *
 * @since 1.1
 * @module
 */
public abstract class GeometryWithCRS<G> extends GeometryWrapper<G> {
    /**
     * The coordinate reference system, or {@code null} if unspecified.
     */
    private CoordinateReferenceSystem crs;

    /**
     * Creates a new instance initialized with null CRS.
     */
    protected GeometryWithCRS() {
    }

    /**
     * Gets the Coordinate Reference System (CRS) of this geometry.
     *
     * @return the geometry CRS, or {@code null} if unknown.
     */
    @Override
    public final CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return crs;
    }

    /**
     * Sets the coordinate reference system, which shall be two-dimensional.
     *
     * @param  crs  the coordinate reference system to set.
     */
    @Override
    public final void setCoordinateReferenceSystem(final CoordinateReferenceSystem crs) {
        ArgumentChecks.ensureDimensionMatches("crs", Geometries.BIDIMENSIONAL, crs);
        this.crs = crs;
    }

    /**
     * Returns {@code true} if the given geometry use the same CRS than this geometry, or conservatively
     * returns {@code false} in case of doubt. This method should perform only a cheap test; it is used
     * as a way to filter rapidly if {@link #transform(CoordinateReferenceSystem)} needs to be invoked.
     *
     * @param  other  the second geometry.
     * @return {@code true} if the two geometries use equivalent CRS or if the CRS is undefined on both side,
     *         or {@code false} in case of doubt.
     */
    @Override
    public final boolean isSameCRS(final GeometryWrapper<G> other) {
        /*
         * Identity comparison is often sufficient since all geometries typically share the same CRS.
         * If they are not the same instance, a more expansive `equalsIgnoreMetadata(…)` method here
         * would probably duplicate the work done later by the `transform(Geometry, …)` method.
         */
        return crs == ((GeometryWithCRS<G>) other).crs;
    }

    /**
     * Creates an initially empty envelope with the CRS of this geometry.
     * If this geometry has no CRS, then a two-dimensional envelope is created.
     * This is a convenience method for {@link #getEnvelope()} implementations.
     *
     * @return an initially empty envelope.
     */
    protected final GeneralEnvelope createEnvelope() {
        return (crs != null) ? new GeneralEnvelope(crs) : new GeneralEnvelope(Geometries.BIDIMENSIONAL);
    }
}
