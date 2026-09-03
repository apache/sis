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
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.CurvePolygon;
import org.apache.sis.geometries.Geometries;


/**
 * A planar surface whose boundary rings may use any interpolation, not only the linear one that a
 * {@link org.apache.sis.geometries.Polygon} is restricted to.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DefaultCurvePolygon extends AbstractGeometry implements CurvePolygon {

    protected final Curve exterior;
    protected final List<Curve> interiors;

    public DefaultCurvePolygon(Curve exterior) {
        this(exterior, null);
    }

    public DefaultCurvePolygon(Curve exterior, List<Curve> interiors) {
        this.exterior = Objects.requireNonNull(exterior);
        this.interiors = (interiors == null) ? List.of() : List.copyOf(interiors);
        for (final Curve interior : this.interiors) {
            Geometries.ensureSameAttributes(exterior.getAttributesType(), interior.getAttributesType());
        }
    }

    @Override
    public Curve getExteriorRing() {
        return exterior;
    }

    @Override
    public List<Curve> getInteriorRings() {
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
        for (final Curve interior : interiors) {
            interior.setCoordinateReferenceSystem(cs);
        }
    }

    @Override
    public Envelope getEnvelope() {
        return exterior.getEnvelope();
    }

    @Override
    public double getArea() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String asText() {
        final StringBuilder sb = new StringBuilder(TYPE).append(" (").append(exterior.asText());
        for (final Curve interior : interiors) {
            sb.append(", ").append(interior.asText());
        }
        return sb.append(')').toString();
    }
}
