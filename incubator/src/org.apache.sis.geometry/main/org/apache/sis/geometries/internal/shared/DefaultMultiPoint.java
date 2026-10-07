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
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.point.MultiPoint;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultMultiPoint extends AbstractGeometry implements MultiPoint<Point> {

    private final DataPoints points;

    public DefaultMultiPoint(DataPoints points) {
        this.points = points;
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return points.getCoordinateReferenceSystem();
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
        points.setCoordinateReferenceSystem(cs);
    }

    @Override
    public Envelope getEnvelope() {
        return points.getEnvelope();
    }

    @Override
    public int getNumGeometries() {
        return points.size();
    }

    @Override
    public Point getGeometryN(int n) {
        return points.getPoint(n);
    }

    @Override
    public DataPointsType getDataPointsType() {
        return points.getType();
    }

    @Override
    public DataPoints asDataPoints() {
        return points;
    }

    @Override
    public Point getCentroid() {
        if (isEmpty()) {
            // The centroid of the empty set is undefined.
            return null;
        }
        //TODO : fallback on JTS until implemented
        return (Point) fromJTS(asJTS().getCentroid());
    }

    @Override
    public Point getRepresentativePoint() {
        if (isEmpty()) {
            // The empty set has no interior position.
            return null;
        }
        //TODO : fallback on JTS until implemented
        return (Point) fromJTS(asJTS().getInteriorPoint());
    }

    @Override
    public Geometry boundary() {
        if (isEmpty()) {
            // The boundary of the empty set is empty.
            return GeometryFactory.createEmpty(getCoordinateReferenceSystem());
        }
        //TODO : fallback on JTS until implemented
        return fromJTS(asJTS().getBoundary());
    }

    @Override
    public boolean isSimple() {
        //TODO : fallback on JTS until implemented
        return asJTS().isSimple();
    }

    @Override
    public boolean isValid() {
        //TODO : fallback on JTS until implemented
        return asJTS().isValid();
    }

    @Override
    public int hashCode() {
        return 13 * Objects.hashCode(points);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return Objects.equals(points, ((DefaultMultiPoint) obj).points);
    }
}
