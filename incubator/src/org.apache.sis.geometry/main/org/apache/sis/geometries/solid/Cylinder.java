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
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vectors;
import org.apache.sis.maths.Vector;


/**
 * A cylinder along the axis joining two positions, with potentially different radii at each end.
 * <p>
 * A cone is a special case of cylinder when one of the radii is zero.
 * <p>
 * Synonym : conical frustrum is a volume defined by 2 circles aligned on the same axis
 *
 * ISO 19107 : classified as a conic surface
 *
 * @author Johann Sorel (Geomatys)
 * @spec ISO_19107 section 8.5.4  Cylinder
 * @spec ISO_19107 section 8.5.3  Cone
 * @spec ISO_12113 KHR_implicit_shapes extension Cylinder
 */
@UML(identifier="Cylinder", specification=ISO_12113)
public final class Cylinder extends AbstractGeometry{

    /**
     * Must contain two points : the center of the bottom circle, then the center of the top one.
     */
    private final DataPoints points;

    private double radiusTop = 1.0;
    private double radiusBottom = 1.0;

    /**
     * Creates a cylinder along the axis joining the two given positions.
     * The reference system of the cylinder is the one of those positions.
     *
     * @param  bottom  the center of the bottom circle, not null.
     * @param  top     the center of the top circle, not null.
     */
    public Cylinder(Vector<?> bottom, Vector<?> top) {
        points = GeometryFactory.DEFAULT.createDataPoints(NDArrays.of(bottom.getSampleSystem(), bottom.getDataType(), 2));
        points.setPosition(0, bottom);
        points.setPosition(1, top);
    }

    /**
     * Creates a cylinder along the axis joining the two positions of the given sequence.
     * The sequence is taken as-is, so the caller may give the cylinder the attributes
     * carried by that sequence.
     *
     * @param  points  the centers of the bottom and top circles, in that order.
     * @throws IllegalArgumentException if the given sequence does not hold exactly two positions.
     */
    public Cylinder(DataPoints points) {
        if (points.size() != 2) {
            throw new IllegalArgumentException("Cylinder sequence must contain two points");
        }
        this.points = points;
    }

    @Override
    public GeometryType getGeometryType() {
        return GeometryType.CYLINDER;
    }

    public DataPoints getDataPoints() {
        return points;
    }

    /**
     * @return the center of the bottom circle
     */
    public Vector<?> getBottom() {
        return points.getPosition(0);
    }

    /**
     * @param position new center of the bottom circle
     */
    public void setBottom(Vector<?> position) {
        points.setPosition(0, position);
    }

    /**
     * @return the center of the top circle
     */
    public Vector<?> getTop() {
        return points.getPosition(1);
    }

    /**
     * @param position new center of the top circle
     */
    public void setTop(Vector<?> position) {
        points.setPosition(1, position);
    }

    /**
     * Returns the vector going from the center of the bottom circle to the center of the top one.
     * It is built on doubles whatever the type of the positions, the direction and the length of
     * an axis being real values even when the positions they are derived from are integers.
     */
    private Vector<?> getAxis() {
        final Vector<?> bottom = getBottom();
        final Vector<?> axis = Vectors.create(bottom.getSampleSystem(), DataType.DOUBLE);
        axis.set(getTop());
        axis.subtract(bottom);
        return axis;
    }

    /**
     * The height is the length of the axis going from the center of the bottom circle to the
     * center of the top one, so it is not set but derived from the two positions this cylinder
     * is built on.
     *
     * @return the cylinder height
     */
    public double getHeight() {
        return getAxis().length();
    }

    /**
     * @return cylinder top circle radius
     */
    public double getRadiusTop() {
        return radiusTop;
    }

    /**
     * @param radius new cylinder top radius
     */
    public void setRadiusTop(double radius) {
        this.radiusTop = radius;
    }

    /**
     * @return cylinder bottom circle radius
     */
    public double getRadiusBottom() {
        return radiusBottom;
    }

    /**
     * @param radius new cylinder bottom radius
     */
    public void setRadiusBottom(double radius) {
        this.radiusBottom = radius;
    }

    /**
     * A cylinder becomes a cone when the top or bottom radius is set to 0.0.
     *
     * @return true if cylinder is a cone, top or bottom radius is 0.0.
     */
    public boolean isCone() {
        return radiusBottom == 0.0 || radiusTop == 0.0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * <p>A circle of radius <var>r</var> perpendicular to a unit axis <var>n</var> spreads by
     * <var>r</var>·√(1 − <var>n</var>ᵢ²) on the axis <var>i</var>, which is the whole radius on
     * the axes the circle faces and nothing on the axis it is perpendicular to. The envelope is
     * the union of the two circles spread that way. A cylinder whose two positions are the same
     * has no axis to speak of, and is then bounded by its radii on every axis.</p>
     */
    @Override
    public BBox getEnvelope() {
        final Vector<?> bottom = getBottom();
        final Vector<?> top = getTop();
        final Vector<?> axis = getAxis();
        final double height = axis.length();
        final int dim = points.getDimension();
        final BBox bbox = new BBox(dim);
        for (int i=0;i<dim;i++){
            final double b = bottom.get(i);
            final double t = top.get(i);
            final double spread;
            if (height > 0) {
                final double n = axis.get(i) / height;
                spread = Math.sqrt(Math.max(0, 1 - n*n));
            } else {
                spread = 1;
            }
            bbox.setRange(i, Math.min(b - radiusBottom*spread, t - radiusTop*spread),
                             Math.max(b + radiusBottom*spread, t + radiusTop*spread));
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
        final Cylinder other = (Cylinder) obj;
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
