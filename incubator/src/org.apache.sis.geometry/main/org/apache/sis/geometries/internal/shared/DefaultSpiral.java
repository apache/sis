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
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.curve.RealFunction;
import org.apache.sis.geometries.curve.Spiral;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.Vector;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * A curve defined indirectly by its curvature, and by its torsion when it is not planar.
 * The spiral is built in the tangent space at its start point, then projected on the geometric
 * reference surface.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultSpiral extends AbstractGeometry implements Spiral {

    /**
     * Points of the spiral, the first one being the start point in whose tangent space the spiral
     * is constructed.
     */
    protected final DataPoints points;

    /**
     * Curvature of this spiral as a function of arc length.
     */
    protected final RealFunction curvature;

    /**
     * Torsion of this spiral as a function of arc length, or {@code null} if this spiral is planar.
     */
    protected final RealFunction torsion;

    /**
     * Orthonormal frame located at the start point, made of two vectors if this spiral is planar
     * and of three vectors otherwise.
     */
    protected final List<Vector> startFrame;

    /**
     * Creates a spiral from its curvature, its torsion and the frame at its start point.
     *
     * @param  points      points of the spiral, the first one being its start point.
     * @param  curvature   curvature as a function of arc length, measured from a fixed point of the
     *                     infinite spiral which is not necessarily on this curve.
     * @param  torsion     torsion as a function of arc length, or {@code null} if this spiral is planar.
     * @param  startFrame  two or three mutually orthogonal unit vectors forming a right-handed frame
     *                     at the start point.
     */
    public DefaultSpiral(final DataPoints points, final RealFunction curvature,
            final RealFunction torsion, final List<Vector> startFrame)
    {
        this.points     = Objects.requireNonNull(points);
        this.curvature  = Objects.requireNonNull(curvature);
        this.torsion    = torsion;
        this.startFrame = (startFrame == null) ? List.of() : List.copyOf(startFrame);
    }

    @Override
    public RealFunction getCurvature() {
        return curvature;
    }

    @Override
    public RealFunction getTorsion() {
        return torsion;
    }

    @Override
    public List<Vector> getStartFrame() {
        return startFrame;
    }

    @Override
    public DataPoints getDataPoints() {
        return points;
    }

    /**
     * Returns {@code null}: a spiral is defined by its curvature, not by control points.
     */
    @Override
    public Array getControlPoints() {
        return null;
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
