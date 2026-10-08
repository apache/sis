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
package org.apache.sis.geometries.solid;

import java.util.Objects;
import static org.opengis.annotation.Specification.ISO_12113;
import org.opengis.annotation.UML;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.BBox;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.internal.shared.AbstractGeometry;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.ReadOnly;
import org.apache.sis.maths.Vectors;
import org.apache.sis.maths.Vector;


/**
 * A capsule (cylinder with hemispherical ends) defined by two "capping" spheres centered on two
 * positions, with potentially different radii.
 *
 *
 * @author Johann Sorel (Geomatys)
 * @spec ISO_12113 KHR_implicit_shapes extension Capsule
 */
@UML(identifier="Capsule", specification=ISO_12113)
public final class Capsule extends AbstractGeometry{

    /**
     * Must contain two points : the center of the bottom sphere, then the center of the top one.
     */
    private final DataPoints points;

    private double radiusTop = 1.0;
    private double radiusBottom = 1.0;

    /**
     * Creates a capsule whose capping spheres are centered on the two given positions.
     * The reference system of the capsule is the one of those positions.
     *
     * @param  bottom  the center of the bottom sphere, not null.
     * @param  top     the center of the top sphere, not null.
     */
    public Capsule(Vector<?> bottom, Vector<?> top) {
        points = GeometryFactory.DEFAULT.createDataPoints(NDArrays.of(bottom.getSampleSystem(), bottom.getDataType(), 2));
        points.setPosition(0, bottom);
        points.setPosition(1, top);
    }

    /**
     * Creates a capsule whose capping spheres are centered on the two positions of the given
     * sequence. The sequence is taken as-is, so the caller may give the capsule the attributes
     * carried by that sequence.
     *
     * @param  points  the centers of the bottom and top spheres, in that order.
     * @throws IllegalArgumentException if the given sequence does not hold exactly two positions.
     */
    public Capsule(DataPoints points) {
        if (points.size() != 2) {
            throw new IllegalArgumentException("Capsule sequence must contain two points");
        }
        this.points = points;
    }

    @Override
    public GeometryType getGeometryType() {
        return GeometryType.CAPSULE;
    }

    public DataPoints getDataPoints() {
        return points;
    }

    /**
     * @return the center of the bottom sphere
     */
    public ReadOnly.Vector<?> getBottom() {
        return points.getPosition(0);
    }

    /**
     * @param position new center of the bottom sphere
     */
    public void setBottom(ReadOnly.Vector<?> position) {
        points.setPosition(0, position);
    }

    /**
     * @return the center of the top sphere
     */
    public ReadOnly.Vector<?> getTop() {
        return points.getPosition(1);
    }

    /**
     * @param position new center of the top sphere
     */
    public void setTop(ReadOnly.Vector<?> position) {
        points.setPosition(1, position);
    }

    /**
     * Returns the vector going from the center of the bottom sphere to the center of the top one.
     * It is built on doubles whatever the type of the positions, the direction and the length of
     * an axis being real values even when the positions they are derived from are integers.
     */
    private Vector<?> getAxis() {
        final ReadOnly.Vector<?> bottom = getBottom();
        final Vector<?> axis = Vectors.create(bottom.getSampleSystem(), DataType.DOUBLE);
        axis.set(getTop());
        axis.subtract(bottom);
        return axis;
    }

    /**
     * The height is the length of the axis going from the center of the bottom sphere to the
     * center of the top one, so it is not set but derived from the two positions this capsule
     * is built on. It does not include the two hemispherical ends.
     *
     * @return the capsule height
     */
    public double getHeight() {
        return getAxis().length();
    }

    /**
     * @return capsule top sphere radius
     */
    public double getRadiusTop() {
        return radiusTop;
    }

    /**
     * @param radius new capsule top radius
     */
    public void setRadiusTop(double radius) {
        this.radiusTop = radius;
    }

    /**
     * @return capsule bottom sphere radius
     */
    public double getRadiusBottom() {
        return radiusBottom;
    }

    /**
     * @param radius new capsule bottom radius
     */
    public void setRadiusBottom(double radius) {
        this.radiusBottom = radius;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * <p>A capsule is the convex hull of its two capping spheres, so it reaches the whole radius
     * of each of them on every axis. Contrarily to a {@linkplain Cylinder cylinder}, whose flat
     * ends spread less than their radius on the axis they face, a capsule is therefore bounded
     * by the union of the two boxes bounding its spheres.</p>
     */
    @Override
    public BBox getEnvelope() {
        final ReadOnly.Vector<?> bottom = getBottom();
        final ReadOnly.Vector<?> top = getTop();
        final int dim = points.getDimension();
        final BBox bbox = new BBox(dim);
        for (int i=0;i<dim;i++){
            final double b = bottom.get(i);
            final double t = top.get(i);
            bbox.setRange(i, Math.min(b - radiusBottom, t - radiusTop),
                             Math.max(b + radiusBottom, t + radiusTop));
        }
        return bbox;
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
    public DataPointsType getDataPointsType() {
        return points.getType();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Capsule other = (Capsule) obj;
        if (!Objects.equals(this.points, other.points)) {
            return false;
        }
        return Double.doubleToLongBits(this.radiusTop) == Double.doubleToLongBits(other.radiusTop)
            && Double.doubleToLongBits(this.radiusBottom) == Double.doubleToLongBits(other.radiusBottom);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + Objects.hashCode(this.points);
        hash = 89 * hash + Double.hashCode(this.radiusTop);
        hash = 89 * hash + Double.hashCode(this.radiusBottom);
        return hash;
    }
}
