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
import javax.measure.Quantity;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Geometry;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.Solid;
import org.apache.sis.geometries.Surface;
import org.apache.sis.geometry.GeneralEnvelope;


/**
 * A solid defined by its bounding shells, each shell being a closed set of polygons.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultSolid extends AbstractGeometry implements Solid {

    private final List<Surface> boundaries;

    public DefaultSolid(List<Surface> boundaries) {
        this.boundaries = (boundaries == null) ? List.of() : List.copyOf(boundaries);
    }

    @Override
    public GeometryType getGeometryType() {
        return GeometryType.SOLID;
    }

    @Override
    public boolean isEmpty() {
        return boundaries.isEmpty();
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return boundaries.get(0).getCoordinateReferenceSystem();
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
        for (final Surface boundary : boundaries) {
            boundary.setCoordinateReferenceSystem(cs);
        }
    }

    /**
     * Returns the envelope of the exterior shell. The interior shells are voids inside it,
     * so they cannot extend it.
     */
    @Override
    public Envelope getEnvelope() {
        GeneralEnvelope e = null;
        for (int i = 0, n = boundaries.size(); i < n; i++) {
            Surface sn = boundaries.get(i);
            Envelope envelope = sn.getEnvelope();
            if (envelope != null) {
                if (e == null) {
                    e = new GeneralEnvelope(envelope);
                } else {
                    e.add(envelope);
                }
            }
        }
        if (e == null) {
            e = new GeneralEnvelope(getCoordinateReferenceSystem());
            e.setToNaN();
        }
        return e;
    }

    @Override
    public DataPoints getDataPoints() {
        if (boundaries.isEmpty()) {
            return new EmptyDataPoints(getCoordinateReferenceSystem());
        }
        return ConcatenatedDataPoints.of(boundaries.toArray(Geometry[]::new));
    }

    @Override
    public DataPointsType getDataPointsType() {
        return boundaries.get(0).getDataPointsType();
    }

    @Override
    public GeometryCollection<? extends Surface> getBoundary() {
        return GeometryFactory.DEFAULT.createGeometryCollection(GeometryType.SURFACE, getCoordinateReferenceSystem(), boundaries.toArray(Surface[]::new));
    }

    @Override
    public Quantity<?> getArea() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Quantity<?> getVolume() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<DirectPosition> getControlPoints() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<double[]> getKnots() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
