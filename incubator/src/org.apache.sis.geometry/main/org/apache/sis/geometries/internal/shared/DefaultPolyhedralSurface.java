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

import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.Polygon;
import org.apache.sis.geometries.PolyhedralSurface;


/**
 * A surface made of polygonal patches which share their common boundary segments.
 *
 * @author Johann Sorel (Geomatys)
 * @see GML Surface patches PolygonPatch CompositeSurface
 */
public class DefaultPolyhedralSurface<T extends Polygon> extends AbstractGeometry implements PolyhedralSurface<T> {

    private final T[] patches;

    /**
     * The coordinate reference system to report when this surface has no patch.
     * Ignored otherwise, in which case the first patch is authoritative. May be {@code null}.
     */
    private final CoordinateReferenceSystem fallbackCRS;

    @SafeVarargs
    public DefaultPolyhedralSurface(T... patches) {
        this(null, patches);
    }

    /**
     * Creates a surface which reports the given coordinate reference system
     * when {@code patches} is empty.
     */
    public DefaultPolyhedralSurface(CoordinateReferenceSystem fallbackCRS, T[] patches) {
        this.patches     = patches;
        this.fallbackCRS = fallbackCRS;
    }

    @Override
    public int getNumPatches() {
        return patches.length;
    }

    @Override
    public T getPatchN(int n) {
        return patches[n];
    }

    @Override
    public boolean isEmpty() {
        for (final T patch : patches) {
            if (!patch.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return (patches.length != 0) ? patches[0].getCoordinateReferenceSystem() : fallbackCRS;
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
        for (final T patch : patches) {
            patch.setCoordinateReferenceSystem(cs);
        }
    }

    @Override
    public AttributesType getAttributesType() {
        return (patches.length != 0) ? patches[0].getAttributesType() : AttributesType.EMPTY;
    }

    @Override
    public Envelope getEnvelope() {
        return envUnion(patches);
    }

    @Override
    public String asText() {
        final StringBuilder sb = new StringBuilder(PolyhedralSurface.TYPE).append(" (");
        for (int i = 0; i < patches.length; i++) {
            if (i != 0) sb.append(", ");
            sb.append(patches[i].asText());
        }
        return sb.append(')').toString();
    }
}
