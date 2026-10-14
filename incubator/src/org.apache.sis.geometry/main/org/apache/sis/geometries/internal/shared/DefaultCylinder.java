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
import org.apache.sis.geometries.surface.Cylinder;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.ReadOnly;
import org.apache.sis.maths.Vector;


/**
 * Default implementation of a cylinder, storing the centers of its two end circles
 * in a {@code DataPoints}.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultCylinder extends AbstractGeometry implements Cylinder {

    /**
     * Must contain two points : the center of the bottom circle, then the center of the top one.
     */
    private final DataPoints points;

    private double radiusTop = 1.0;
    private double radiusBottom = 1.0;

    /**
     * Whether the ends are closed by disks.
     */
    private boolean closed = true;

    /**
     * Creates a cylinder along the axis joining the two given positions.
     * The reference system of the cylinder is the one of those positions.
     *
     * @param  bottom  the center of the bottom circle, not null.
     * @param  top     the center of the top circle, not null.
     */
    public DefaultCylinder(Vector<?> bottom, Vector<?> top) {
        points = GeometryFactory.DEFAULT.createDataPoints(NDArrays.of(bottom.getSampleSystem(), bottom.getDataType(), 2));
        points.setPosition(0, bottom);
        points.setPosition(1, top);
    }

    /**
     * Creates a cylinder along the axis joining the two positions of the given {@code DataPoints}.
     * The {@code DataPoints} is taken as-is, so the caller may give the cylinder the attributes
     * carried by that {@code DataPoints}.
     *
     * @param  points  the centers of the bottom and top circles, in that order.
     * @throws IllegalArgumentException if the given {@code DataPoints} does not hold exactly two positions.
     */
    public DefaultCylinder(DataPoints points) {
        if (points.size() != 2) {
            throw new IllegalArgumentException("Cylinder sequence must contain two points");
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
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DefaultCylinder other = (DefaultCylinder) obj;
        if (!Objects.equals(this.points, other.points)) {
            return false;
        }
        return Double.doubleToLongBits(this.radiusTop) == Double.doubleToLongBits(other.radiusTop)
            && Double.doubleToLongBits(this.radiusBottom) == Double.doubleToLongBits(other.radiusBottom)
            && this.closed == other.closed;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + Objects.hashCode(this.points);
        hash = 89 * hash + Double.hashCode(this.radiusTop);
        hash = 89 * hash + Double.hashCode(this.radiusBottom);
        hash = 89 * hash + Boolean.hashCode(this.closed);
        return hash;
    }
}
