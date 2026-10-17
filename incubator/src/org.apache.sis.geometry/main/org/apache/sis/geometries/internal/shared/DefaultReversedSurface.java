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

import java.util.List;
import java.util.Objects;
import javax.measure.Quantity;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.Orientable;
import org.apache.sis.geometries.Primitive;
import org.apache.sis.geometries.Surface;
import org.apache.sis.geometries.SurfaceInterpolation;
import org.apache.sis.geometries.DataPointsType;


/**
 * A surface whose up-normal points the opposite way to the one it is defined with.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultReversedSurface extends AbstractGeometry implements Surface {

    private final Surface base;

    public DefaultReversedSurface(Surface base) {
        this.base = Objects.requireNonNull(base);
    }

    @Override
    public Sign getOrientationSign() {
        return Sign.NEGATIVE;
    }

    @Override
    public Primitive getPrimitive() {
        return base;
    }

    @Override
    public Orientable getProxy() {
        return base;
    }

    @Override
    public Surface getReverse() {
        return base;
    }

    /**
     * Returns the interpolations of the base surface: reversing a surface does not change how it is built.
     */
    @Override
    public List<SurfaceInterpolation> getInterpolation() {
        return base.getInterpolation();
    }

    /**
     * Returns the geometry type of the base surface: reversing a surface does not change what kind
     * of surface it is.
     */
    @Override
    public GeometryType getGeometryType() {
        return base.getGeometryType();
    }

    /**
     * Returns the area of the base surface. Area is unsigned, so the orientation does not affect it.
     */
    @Override
    public Quantity<?> getArea() {
        return base.getArea();
    }

    @Override
    public boolean isEmpty() {
        return base.isEmpty();
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return base.getCoordinateReferenceSystem();
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
        base.setCoordinateReferenceSystem(cs);
    }

    @Override
    public DataPointsType getDataPointsType() {
        return base.getDataPointsType();
    }

    @Override
    public Envelope getEnvelope() {
        return base.getEnvelope();
    }

    /**
     * Returns the boundary of the base surface with each curve reversed, so that this surface,
     * whose up-normal is flipped, stays on the left of its boundary.
     */
    @Override
    public Geometry getBoundary() {
        final Geometry boundary = base.getBoundary();
        if (boundary instanceof Orientable o) {
            return o.getReverse();
        }
        if (boundary instanceof GeometryCollection<?> c) {
            final var reversed = new Geometry[c.getNumGeometries()];
            for (int i = 0; i < reversed.length; i++) {
                final Geometry g = c.getGeometryN(i);
                reversed[i] = (g instanceof Orientable o) ? o.getReverse() : g;
            }
            return GeometryFactory.DEFAULT.createGeometryCollection(c.getElementType(), c.getCoordinateReferenceSystem(), reversed);
        }
        return boundary;
    }

    @Override
    public DataPoints getDataPoints() {
        return base.getDataPoints();
    }

}
