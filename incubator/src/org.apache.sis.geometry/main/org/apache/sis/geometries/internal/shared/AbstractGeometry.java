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
package org.apache.sis.geometries.internal.shared;

import java.util.HashMap;
import java.util.Map;
import org.opengis.geometry.Envelope;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDRange;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vectors;


/**
 * Abstract geometry, manages crs only.
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract non-sealed class AbstractGeometry implements Geometry {

    private Map<String,Object> properties;

    @Override
    public synchronized Map<String, Object> userProperties() {
        if (properties == null) {
            properties = new HashMap<>();
        }
        return properties;
    }

    @Override
    public String toString() {
        return asText();
    }

    /**
     * Returns a view of this geometry as a JTS geometry, for delegating to JTS an operation
     * which is not yet implemented in this package. The view shares the coordinates of this
     * geometry, no copy is performed.
     *
     * <p>Geometry types which have no JTS equivalent, such as the parametric curves and the
     * solids, cannot be delegated this way. For them this method reports the operation as
     * unsupported, which is the contract of the operations relying on this fallback.</p>
     *
     * @return this geometry seen as a JTS geometry.
     * @throws UnsupportedOperationException if this geometry has no JTS equivalent.
     */
    protected final org.locationtech.jts.geom.Geometry asJTS() {
        try {
            return Geometries.asJTS(this, false, null);
        } catch (IllegalArgumentException e) {
            throw new UnsupportedOperationException(e.getMessage(), e);
        }
    }

    /**
     * Converts back to a geometry of this package the result of an operation delegated to JTS.
     * The JTS geometries produced by such an operation carry no reference system, therefore the
     * system of this geometry is assigned to the result.
     *
     * @param  result  the geometry computed by JTS.
     * @return the result as a geometry of this package.
     */
    protected final Geometry fromJTS(final org.locationtech.jts.geom.Geometry result) {
        result.setUserData(getCoordinateReferenceSystem());
        return Geometries.fromJTS(result, true);
    }

    /**
     * Returns the union of the envelopes of the given geometries,
     * or {@code null} if there is nothing to compute a union of.
     */
    protected static Envelope envUnion(final Geometry... geometries) {
        GeneralEnvelope union = null;
        for (final Geometry geometry : geometries) {
            union = add(union, geometry);
        }
        return union;
    }

    /**
     * Returns the envelope of the given data points and control points, or {@code null} if both are empty.
     * A spline whose basis functions are a partition of unity, such as a b-spline, stays inside the convex
     * hull of its control points, so the returned envelope contains it.
     *
     * @param  points         data points of the spline, or {@code null} if none.
     * @param  controlPoints  the control points, or {@code null} if none.
     * @param  homogeneous    whether the control points have one more coordinate than the reference system,
     *                        the weight by which the other coordinates are divided. Weights are assumed positive.
     * @return envelope of the data points and of the control points, or {@code null} if there is none.
     */
    protected static Envelope controlHullEnvelope(final DataPoints points, final Array controlPoints,
                                                  final boolean homogeneous)
    {
        NDRange envelope = (points == null || points.isEmpty()) ? null : new NDRange(points.getEnvelope());
        final long n = (controlPoints != null) ? controlPoints.getLength() : 0;
        for (long i = 0; i < n; i++) {
            Vector<?> position = controlPoints.get(i);
            if (homogeneous) {
                final int dimension = position.getDimension() - 1;
                final double weight = position.get(dimension);
                position = Vectors.createDouble(dimension).set(position).scale(1 / weight);
            }
            if (envelope == null) {
                final Vector<?> start = (points != null)
                        ? Vectors.create(points.getCoordinateReferenceSystem(), DataType.DOUBLE).set(position)
                        : position.copy(DataType.DOUBLE);
                envelope = new NDRange(start, start);
            } else {
                envelope.add(position);
            }
        }
        return envelope;
    }

    /**
     * Adds the envelope of the given geometry to the given union, creating it if needed.
     * Geometries with no envelope at all (an empty one, typically) are skipped.
     */
    private static GeneralEnvelope add(GeneralEnvelope union, final Geometry geometry) {
        final Envelope envelope = geometry.getEnvelope();
        if (envelope != null) {
            if (union == null) {
                union = new GeneralEnvelope(envelope);
            } else {
                union.add(envelope);
            }
        }
        return union;
    }
}
