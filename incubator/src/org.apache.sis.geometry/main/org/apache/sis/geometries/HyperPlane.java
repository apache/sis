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
import org.apache.sis.maths.Tuple;
import org.apache.sis.maths.Vector;


/**
 * A hyperplane divides the geometric space in two.
 * It is build from a position and a normal.
 *
 * @author Johann Sorel (Geomatys)
 */
@UML(identifier="Plane", specification=ISO_12113)
public final class HyperPlane extends AbstractGeometry{

    /**
     * Must contain a single point.
     */
    private final DataPoints points;

    private Vector<?> normal;

    /**
     * Creates a hyperplane passing by the given position.
     * The reference system of the hyperplane is the one of that position.
     *
     * @param  position  a position the hyperplane passes by, not null.
     * @param  normal    the direction the hyperplane is perpendicular to, not null.
     */
    public HyperPlane(Tuple<?> position, Vector<?> normal) {
        points = GeometryFactory.createSequence(NDArrays.of(position.getSampleSystem(), position.getDataType(), 1));
        points.setPosition(0, position);
        this.normal = normal;
    }

    /**
     * Creates a hyperplane passing by the single position of the given sequence.
     * The sequence is taken as-is, so the caller may give the hyperplane the attributes
     * carried by that sequence.
     *
     * @param  points  a position the hyperplane passes by, as a sequence of exactly one position.
     * @param  normal  the direction the hyperplane is perpendicular to, not null.
     * @throws IllegalArgumentException if the given sequence does not hold exactly one position.
     */
    public HyperPlane(DataPoints points, Vector<?> normal) {
        if (points.size() != 1) {
            throw new IllegalArgumentException("HyperPlane sequence must contain one point");
        }
        this.points = points;
        this.normal = normal;
    }

    @Override
    public GeometryType getGeometryType() {
        return GeometryType.HYPERPLANE;
    }

    public DataPoints getDataPoints() {
        return points;
    }

    public Tuple<?> getPosition() {
        return points.getPosition(0);
    }

    public void setPosition(Tuple<?> position) {
        points.setPosition(0, position);
    }

    public Vector<?> getNormal() {
        return normal;
    }

    public void setNormal(Vector<?> normal) {
        this.normal = normal;
    }

    /**
     * {@inheritDoc}
     *
     * <p>A hyperplane extends to infinity along every direction it contains. It is therefore
     * bounded on one axis only when it is perpendicular to that axis, which happens when the
     * normal is aligned with it. In every other case, including a normal which is null in all
     * its dimensions, the hyperplane spans the whole space on every axis.</p>
     */
    @Override
    public BBox getEnvelope() {
        final Tuple<?> position = getPosition();
        final int dim = normal.getDimension();
        final BBox bbox = new BBox(dim);
        /*
         * Search the axis the normal is aligned with. There is one only if every other
         * dimension of the normal is null, in which case the hyperplane is flat on it.
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
        for (int i=0;i<dim;i++){
            if (i == flat){
                bbox.setRange(i, position.get(i), position.get(i));
            } else {
                bbox.setRange(i, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            }
        }
        return bbox;
    }

    @Override
    public boolean isEmpty() {
        return false;
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
        final HyperPlane other = (HyperPlane) obj;
        if (!Objects.equals(this.points, other.points)) {
            return false;
        }
        return this.normal == other.normal || (this.normal != null && this.normal.equals(other.normal));
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + Objects.hashCode(this.points);
        hash = 89 * hash + Objects.hashCode(this.normal);
        return hash;
    }
}
