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

import java.util.Objects;
import static org.opengis.annotation.Specification.ISO_12113;
import org.opengis.annotation.UML;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.internal.shared.AbstractGeometry;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.Vector;


/**
 * A plane centered on a position, perpendicular to a normal,
 * optionally with finite extents.
 * <p>
 * A plane has no side: it is a sheet, and both of its faces belong to it. A surface which
 * divides the space in two, and which therefore has a side, is a {@link HyperPlane}.
 * <p>
 * Synonym : Quad , for a plane with finite extents
 * <p>
 * Synonym : Sheet
 *
 *
 * @author Johann Sorel (Geomatys)
 * @spec ISO_12113 KHR_implicit_shapes extension Plane
 */
@UML(identifier="Plane", specification=ISO_12113)
public final class Plane extends AbstractGeometry{

    /**
     * Must contain a single point.
     */
    private final DataPoints points;

    private Vector<?> normal;

    private double sizeX = Double.NaN;
    private double sizeZ = Double.NaN;

    /**
     * Creates a plane centered on the given position.
     * The reference system of the plane is the one of that position.
     *
     * @param  position  the position the plane is centered on, not null.
     * @param  normal    the direction the plane is perpendicular to, not null.
     */
    public Plane(Vector<?> position, Vector<?> normal) {
        points = GeometryFactory.DEFAULT.createDataPoints(NDArrays.of(position.getSampleSystem(), position.getDataType(), 1));
        points.setPosition(0, position);
        this.normal = normal;
    }

    /**
     * Creates a plane centered on the single position of the given sequence.
     * The sequence is taken as-is, so the caller may give the plane the attributes
     * carried by that sequence.
     *
     * @param  points  the position the plane is centered on, as a sequence of exactly one position.
     * @param  normal  the direction the plane is perpendicular to, not null.
     * @throws IllegalArgumentException if the given sequence does not hold exactly one position.
     */
    public Plane(DataPoints points, Vector<?> normal) {
        if (points.size() != 1) {
            throw new IllegalArgumentException("Plane sequence must contain one point");
        }
        this.points = points;
        this.normal = normal;
    }

    @Override
    public GeometryType getGeometryType() {
        return GeometryType.PLANE;
    }

    public DataPoints getDataPoints() {
        return points;
    }

    public Vector<?> getPosition() {
        return points.getPosition(0);
    }

    public void setPosition(Vector<?> position) {
        points.setPosition(0, position);
    }

    public Vector<?> getNormal() {
        return normal;
    }

    public void setNormal(Vector<?> normal) {
        this.normal = normal;
    }

    /**
     * @return the plane X size
     */
    public double getSizeX() {
        return sizeX;
    }

    /**
     * @param size new plane X size
     */
    public void setSizeX(double size) {
        this.sizeX = size;
    }

    /**
     * @return the plane Z size
     */
    public double getSizeZ() {
        return sizeZ;
    }

    /**
     * @param size new plane Z size
     */
    public void setSizeZ(double size) {
        this.sizeZ = size;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    /**
     * A plane becomes a quad when the sizes are set.
     *
     * @return true if plane is a quad
     */
    public boolean isQuad() {
        return Double.isFinite(sizeX) && Double.isFinite(sizeZ);
    }

    /**
     * {@inheritDoc}
     *
     * <p>A plane is perpendicular to its normal, so it is bounded on an axis only when the
     * normal is aligned with that axis. On the axes it spans, a plane which is not a
     * {@linkplain #isQuad() quad} extends to infinity.</p>
     *
     * <p>TODO / Limitation: the sizes of a quad are stated along two axes named <var>X</var> and
     * <var>Z</var>, which this class no longer has the local frame to interpret. They are read
     * here as the two axes the plane spans, in their order in the coordinate system, which is
     * the expected reading of a normal aligned with the second axis of a three-dimensional
     * system. A quad whose normal is not aligned with an axis, or which lies in a space of
     * another number of dimensions, is reported as unbounded on the axes it spans: a bounding
     * box is allowed to be larger than the geometry it contains, never smaller.</p>
     */
    @Override
    public BBox getEnvelope() {
        final Vector<?> position = getPosition();
        final int dim = normal.getDimension();
        final BBox bbox = new BBox(dim);
        /*
         * Search the axis the normal is aligned with. There is one only if every other
         * dimension of the normal is null, in which case the plane is flat on it.
         */
        int flat = -1;
        for (int i=0;i<dim;i++){
            if (normal.get(i) != 0){
                if (flat >= 0){
                    flat = -1;
                    break;
                }
                flat = i;
            }
        }
        final boolean bounded = isQuad() && flat >= 0 && dim == 3;
        int spanned = 0;
        for (int i=0;i<dim;i++){
            if (i == flat){
                bbox.setRange(i, position.get(i), position.get(i));
            } else if (bounded){
                final double half = ((spanned++ == 0) ? sizeX : sizeZ) / 2;
                bbox.setRange(i, position.get(i) - half, position.get(i) + half);
            } else {
                bbox.setRange(i, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            }
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
        final Plane other = (Plane) obj;
        if (!Objects.equals(this.points, other.points)) {
            return false;
        }
        if (!Objects.equals(this.normal, other.normal)) {
            return false;
        }
        return Double.doubleToLongBits(this.sizeX) == Double.doubleToLongBits(other.sizeX)
            && Double.doubleToLongBits(this.sizeZ) == Double.doubleToLongBits(other.sizeZ);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + Objects.hashCode(this.points);
        hash = 89 * hash + Objects.hashCode(this.normal);
        hash = 89 * hash + Double.hashCode(this.sizeX);
        hash = 89 * hash + Double.hashCode(this.sizeZ);
        return hash;
    }
}
