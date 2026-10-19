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
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.curve.Ray;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.ReadOnly;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.maths.Vector;


/**
 * Default implementation of a ray, storing its start position in a {@code DataPoints}.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultRay extends AbstractGeometry implements Ray {

    /**
     * Must contain a single point.
     */
    private final DataPoints points;

    private Vector<?> direction;

    /**
     * Creates a ray starting from the given position.
     *
     * @param  crs        reference system of the ray, or {@code null} for an undefined one.
     * @param  position   the position the ray starts from, not null.
     * @param  direction  the direction the ray extends toward, not null.
     */
    public DefaultRay(CoordinateReferenceSystem crs, Vector<?> position, Vector<?> direction) {
        points = GeometryFactory.DEFAULT.createDataPoints(NDArrays.of(SampleSystem.of(Geometries.orUndefinedCRS(crs, position.getDimension())), position.getDataType(), 1));
        points.setPosition(0, position);
        this.direction = direction;
    }

    /**
     * Creates a ray starting from the single position of the given {@code DataPoints}.
     * The {@code DataPoints} is taken as-is, so the caller may give the ray the attributes
     * carried by that {@code DataPoints}.
     *
     * @param  points     the position the ray starts from, as a {@code DataPoints} of exactly one position.
     * @param  direction  the direction the ray extends toward, not null.
     * @throws IllegalArgumentException if the given {@code DataPoints} does not hold exactly one position.
     */
    public DefaultRay(DataPoints points, Vector<?> direction) {
        if (points.size() != 1) {
            throw new IllegalArgumentException("Ray sequence must contain one point");
        }
        this.points = points;
        this.direction = direction;
    }

    @Override
    public DataPoints getDataPoints() {
        return points;
    }

    @Override
    public ReadOnly.Vector<?> getPosition() {
        return points.getPosition(0);
    }

    @Override
    public void setPosition(ReadOnly.Vector<?> position) {
        points.setPosition(0, position);
    }

    @Override
    public ReadOnly.Vector<?> getDirection() {
        return direction;
    }

    @Override
    public void setDirection(ReadOnly.Vector<?> direction) {
        this.direction = direction.copy();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DefaultRay other = (DefaultRay) obj;
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
