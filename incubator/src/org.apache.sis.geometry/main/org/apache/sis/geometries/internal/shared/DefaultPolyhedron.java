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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.measure.quantity.Area;
import javax.measure.quantity.Volume;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.Knot;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.MultiPolygon;
import org.apache.sis.geometries.Polyhedron;
import org.apache.sis.geometries.SolidInterpolation;
import org.opengis.geometry.DirectPosition;


/**
 * A solid defined by its bounding shells, each shell being a closed set of polygons.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DefaultPolyhedron extends AbstractGeometry implements Polyhedron {

    private final MultiPolygon exterior;
    private final List<MultiPolygon> interiors;

    public DefaultPolyhedron(MultiPolygon exterior) {
        this(exterior, null);
    }

    public DefaultPolyhedron(MultiPolygon exterior, List<MultiPolygon> interiors) {
        this.exterior  = Objects.requireNonNull(exterior);
        this.interiors = (interiors == null) ? List.of() : List.copyOf(interiors);
    }

    @Override
    public MultiPolygon getExteriorShell() {
        return exterior;
    }

    @Override
    public List<MultiPolygon> getInteriorShells() {
        return interiors;
    }

    @Override
    public boolean isEmpty() {
        return exterior.isEmpty();
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return exterior.getCoordinateReferenceSystem();
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
        exterior.setCoordinateReferenceSystem(cs);
        for (final MultiPolygon interior : interiors) {
            interior.setCoordinateReferenceSystem(cs);
        }
    }

    /**
     * Returns the envelope of the exterior shell. The interior shells are voids inside it,
     * so they cannot extend it.
     */
    @Override
    public Envelope getEnvelope() {
        return exterior.getEnvelope();
    }

    @Override
    public String asText() {
        final List<MultiPolygon> shells = new ArrayList<>(interiors.size() + 1);
        shells.add(exterior);
        shells.addAll(interiors);
        final StringBuilder sb = new StringBuilder(TYPE).append(" (");
        for (int i = 0; i < shells.size(); i++) {
            if (i != 0) sb.append(", ");
            sb.append(shells.get(i).asText());
        }
        return sb.append(')').toString();
    }

    @Override
    public Geometry getBoundary() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Area getArea() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Volume getVolume() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<DirectPosition> getDataPoints() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<DirectPosition> getControlPoints() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SolidInterpolation getInterpolation() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Knot> getKnots() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
