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
import javax.measure.Quantity;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.surface.Capsule;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.ReadOnly;
import org.apache.sis.maths.Vector;


/**
 * Default implementation of a capsule, storing the centers of its two capping spheres
 * in a {@code DataPoints}.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultCapsule extends AbstractGeometry implements Capsule {

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
    public DefaultCapsule(Vector<?> bottom, Vector<?> top) {
        points = GeometryFactory.DEFAULT.createDataPoints(NDArrays.of(bottom.getSampleSystem(), bottom.getDataType(), 2));
        points.setPosition(0, bottom);
        points.setPosition(1, top);
    }

    /**
     * Creates a capsule whose capping spheres are centered on the two positions of the given
     * {@code DataPoints}. The {@code DataPoints} is taken as-is, so the caller may give the
     * capsule the attributes carried by that {@code DataPoints}.
     *
     * @param  points  the centers of the bottom and top spheres, in that order.
     * @throws IllegalArgumentException if the given {@code DataPoints} does not hold exactly two positions.
     */
    public DefaultCapsule(DataPoints points) {
        if (points.size() != 2) {
            throw new IllegalArgumentException("Capsule sequence must contain two points");
        }
        this.points = points;
    }

    @Override
    public DataPoints getDataPoints() {
        return points;
    }

    @Override
    public ReadOnly.Vector<?> getBottom() {
        return points.getPosition(0);
    }

    @Override
    public void setBottom(ReadOnly.Vector<?> position) {
        points.setPosition(0, position);
    }

    @Override
    public ReadOnly.Vector<?> getTop() {
        return points.getPosition(1);
    }

    @Override
    public void setTop(ReadOnly.Vector<?> position) {
        points.setPosition(1, position);
    }

    @Override
    public double getRadiusTop() {
        return radiusTop;
    }

    @Override
    public void setRadiusTop(double radius) {
        this.radiusTop = radius;
    }

    @Override
    public double getRadiusBottom() {
        return radiusBottom;
    }

    @Override
    public void setRadiusBottom(double radius) {
        this.radiusBottom = radius;
    }

    @Override
    public Quantity<?> getArea() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DefaultCapsule other = (DefaultCapsule) obj;
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
