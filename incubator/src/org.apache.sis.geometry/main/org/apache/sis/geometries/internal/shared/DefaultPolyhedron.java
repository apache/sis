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
import java.util.Objects;
import javax.measure.Quantity;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.Surface;
import org.apache.sis.geometries.solid.Polyhedron;
import org.apache.sis.geometries.surface.Polygon;


/**
 * A solid defined by its bounding shells, each shell being a closed set of polygons.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultPolyhedron extends AbstractGeometry implements Polyhedron {

    private final GeometryCollection<Polygon> exterior;
    private final List<GeometryCollection<Polygon>> interiors;

    public DefaultPolyhedron(GeometryCollection<Polygon> exterior) {
        this(exterior, null);
    }

    public DefaultPolyhedron(GeometryCollection<Polygon> exterior, List<GeometryCollection<Polygon>> interiors) {
        this.exterior  = Objects.requireNonNull(exterior);
        this.interiors = (interiors == null) ? List.of() : List.copyOf(interiors);
    }

    @Override
    public GeometryCollection<Polygon> getExteriorShell() {
        return exterior;
    }

    @Override
    public List<GeometryCollection<Polygon>> getInteriorShells() {
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
        for (final GeometryCollection<Polygon> interior : interiors) {
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
    public GeometryCollection<? extends Surface> getBoundary() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Returns the sum of the areas of the polygons of the exterior and interior shells.
     */
    @Override
    public Quantity<?> getArea() {
        Quantity<?> area = exterior.getArea();
        for (final GeometryCollection<Polygon> interior : interiors) {
            area = Geometries.add(area, interior.getArea());
        }
        return area;
    }


}
