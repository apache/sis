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
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.internal.shared.AbstractGeometry;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.Vector;


/**
 * A ray is a one direction infinite line.
 * It is build from a position and a direction.
 *
 * @author Johann Sorel
 */
public final class Ray extends AbstractGeometry{

    /**
     * Must contain a single point.
     */
    private final DataPoints points;

    private Vector<?> direction;

    /**
     * Creates a ray starting from the given position.
     * The reference system of the ray is the one of that position.
     *
     * @param  position   the position the ray starts from, not null.
     * @param  direction  the direction the ray extends toward, not null.
     */
    public Ray(Vector<?> position, Vector<?> direction) {
        points = GeometryFactory.DEFAULT.createDataPoints(NDArrays.of(position.getSampleSystem(), position.getDataType(), 1));
        points.setPosition(0, position);
        this.direction = direction;
    }

    /**
     * Creates a ray starting from the single position of the given sequence.
     * The sequence is taken as-is, so the caller may give the ray the attributes
     * carried by that sequence.
     *
     * @param  points     the position the ray starts from, as a sequence of exactly one position.
     * @param  direction  the direction the ray extends toward, not null.
     * @throws IllegalArgumentException if the given sequence does not hold exactly one position.
     */
    public Ray(DataPoints points, Vector<?> direction) {
        if (points.size() != 1) {
            throw new IllegalArgumentException("Ray sequence must contain one point");
        }
        this.points = points;
        this.direction = direction;
    }

    @Override
    public GeometryType getGeometryType() {
        return GeometryType.RAY;
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

    public Vector<?> getDirection() {
        return direction;
    }

    public void setDirection(Vector<?> direction) {
        this.direction = direction;
    }

    @Override
    public BBox getEnvelope() {
        final Vector<?> position = getPosition();
        final int dim = direction.getDimension();
        final BBox bbox = new BBox(dim);
        for (int i=0;i<dim;i++){
            double d = direction.get(i);
            if (d<0){
                bbox.setRange(i, Double.NEGATIVE_INFINITY, position.get(i));
            } else if (d>0){
                bbox.setRange(i, position.get(i), Double.POSITIVE_INFINITY);
            } else {
                bbox.setRange(i, position.get(i), position.get(i));
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
        final Ray other = (Ray) obj;
        if (!Objects.equals(this.points, other.points)) {
            return false;
        }
        return this.direction == other.direction || (this.direction != null && this.direction.equals(other.direction));
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + Objects.hashCode(this.points);
        hash = 89 * hash + Objects.hashCode(this.direction);
        return hash;
    }
}
