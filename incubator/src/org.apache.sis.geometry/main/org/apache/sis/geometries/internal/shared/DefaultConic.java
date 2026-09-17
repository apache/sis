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

import java.util.Objects;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.curve.Conic;
import org.apache.sis.maths.Array;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * A chain of conic section arcs, each of them determined by five data points and drawn in the
 * tangent plane at one control point.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultConic extends AbstractGeometry implements Conic {

    /**
     * Points lying on the conic, four of them being consumed by each arc plus the one shared with
     * the previous arc.
     */
    protected final DataPoints points;

    /**
     * Centers of the exponential maps in which the arcs are constructed, one per arc.
     * May be {@code null} if this conic declares no control point.
     */
    protected final Array controlPoints;

    /**
     * Whether this conic closes on itself.
     */
    protected final boolean cycle;

    /**
     * Creates a conic from the given data points and control points.
     *
     * @param  points         points lying on the conic. At least five of them, the first one also
     *                        acting as the last one when {@code cycle} is {@code true}.
     * @param  controlPoints  centers of the exponential maps of the arcs, or {@code null} if none.
     * @param  cycle          whether this conic closes on itself.
     */
    public DefaultConic(final DataPoints points, final Array controlPoints, final boolean cycle) {
        this.points        = Objects.requireNonNull(points);
        this.controlPoints = controlPoints;
        this.cycle         = cycle;
    }

    @Override
    public DataPoints getDataPoints() {
        return points;
    }

    @Override
    public Array getControlPoints() {
        return controlPoints;
    }

    @Override
    public boolean isCycle() {
        return cycle;
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return points.getCoordinateReferenceSystem();
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) throws IllegalArgumentException {
        points.setCoordinateReferenceSystem(crs);
    }

    @Override
    public AttributesType getAttributesType() {
        return points.getAttributesType();
    }

    @Override
    public boolean isEmpty() {
        return points.isEmpty();
    }

    @Override
    public Envelope getEnvelope() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
