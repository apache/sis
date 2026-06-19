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
package org.apache.sis.geometry.wrapper.jts;

import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.util.resources.Errors;


/**
 * Walks through the component of a geometry for finding the number of dimensions
 * and the range of <abbr>z</abbr> and <abbr>m</abbr> values.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class GeometryWalker {
    /**
     * Whether to search for ranges of <var>z</var> and <var>M</var> values.
     * If {@code false}, this method will check only for the number of dimensions.
     */
    private boolean wantZM;

    /**
     * Whether the coordinate sequence declares to have <var>z</var> or <var>M</var> values.
     */
    private boolean hasZ, hasM;

    /**
     * Range of <var>z</var> values.
     */
    private double minZ, maxZ;

    /**
     * Range of <var>M</var> values.
     */
    private double minM, maxM;

    /**
     * The maximal number of <em>vertex</em> dimensions found. Note that this is different than the
     * {@linkplain Geometry#getDimension() geometry topological dimension}, which can be 0, 1 or 2.
     * The <abbr>JTS</abbr> vertex dimension can be 2, 3 or 4.
     */
    private int dimension;

    /**
     * Creates a new walker.
     * By default, the walker does <em>not</em> search for ranges of <var>z</var> and <var>M</var> values.
     * If this search is desired, the {@link #wantZM()} method must be invoked after construction.
     */
    GeometryWalker() {
    }

    /**
     * Requests the search of ranges of <var>z</var> and <var>M</var> values.
     */
    final void wantZM() {
        wantZM = true;
        minZ = minM = Double.POSITIVE_INFINITY;
        maxZ = maxM = Double.NEGATIVE_INFINITY;
    }

    /**
     * Returns the number of dimensions found by the calls to {@link #scan(Geometry)}.
     */
    final int dimension() {
        if (hasZ & hasM) return Math.max(4, dimension);
        if (hasZ | hasM) return Math.max(3, dimension);
        return dimension;
    }

    /**
     * Sets the <var>z</var> and <var>M</var> coordinates in the given envelope.
     *
     * @param  env  the envelope where to set the coordinates.
     */
    final void setZM(final GeneralEnvelope env) {
        if (wantZM) {
            final int limit = env.getDimension();
            int target = Factory.BIDIMENSIONAL;
            if (target < limit) {
                if (minZ <= maxZ) {
                    env.setRange(target, minZ, maxZ);
                }
                if (minM <= maxM) {
                    if (hasZ) {
                        // Increment even if the range of Z was undefined.
                        target = Factory.TRIDIMENSIONAL;
                        if (target >= limit) return;
                    }
                    env.setRange(target, minM, maxM);
                }
            }
        }
    }

    /**
     * Scans the given geometry for its number of dimensions and range of <var>z</var> and <var>M</var> values.
     * This method may invoke itself recursively for walking through components of the given geometry.
     *
     * @param  geometry  the geometry to inspect.
     * @throws IllegalArgumentException if the type of the given geometry is not recognized.
     */
    final void scan(final Geometry geometry) {
        if (geometry instanceof Point) {
            // Most efficient method (no allocation) in JTS 1.18.
            addDimensionAndRanges(((Point) geometry).getCoordinateSequence());
        } else if (geometry instanceof LineString) {
            // Most efficient method (no allocation) in JTS 1.18.
            addDimensionAndRanges(((LineString) geometry).getCoordinateSequence());
        } else if (geometry instanceof Polygon) {
            final var polygon = (Polygon) geometry;
            scan(polygon.getExteriorRing());
            if (wantZM) {
                final int n = polygon.getNumInteriorRing();
                for (int i=0; i<n; i++) {
                    scan(polygon.getInteriorRingN(i));
                }
            }
        } else if (geometry instanceof GeometryCollection) {
            final var gc = (GeometryCollection) geometry;
            final int n = gc.getNumGeometries();
            if (n > 0) {
                for (int i=0; i<n; i++) {
                    scan(gc.getGeometryN(i));
                }
            } else if (dimension == 0) {
                // Undefined coordinates, JTS assumes 3 for empty geometries.
                dimension = Factory.TRIDIMENSIONAL;
            }
        } else {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.UnknownType_1, geometry.getGeometryType()));
        }
    }

    /**
     * Updates the number of dimensions and the range of <var>z</var> and <var>M</var> values
     * from the given coordinate sequence.
     *
     * @param  cs  the coordinate sequence from which to get the number of dimensions
     *             and the range of (<var>z</var>, <var>M</var>) values.
     */
    private void addDimensionAndRanges(final CoordinateSequence cs) {
        dimension = Math.max(dimension, cs.getDimension());
        if (wantZM) {
            final int n = cs.size();
            if (cs.hasZ()) {
                hasZ = true;
                for (int i=0; i<n; i++) {
                    double z = cs.getZ(i);
                    if (z < minZ) minZ = z;
                    if (z > maxZ) maxZ = z;
                }
            }
            if (cs.hasM()) {
                hasM = true;
                for (int i=0; i<n; i++) {
                    double m = cs.getM(i);
                    if (m < minM) minM = m;
                    if (m > maxM) maxM = m;
                }
            }
        }
    }
}
