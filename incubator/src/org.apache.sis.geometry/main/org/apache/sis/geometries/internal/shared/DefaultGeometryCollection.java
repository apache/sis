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
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.ArgumentChecks;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultGeometryCollection<T extends Geometry> extends AbstractGeometry implements GeometryCollection<T> {

    private final GeometryType elementType;
    protected final List<T> geometries = new ArrayList<>();

    /**
     * The coordinate reference system to report when this collection is empty.
     * Ignored when at least one geometry is present, in which case the first
     * geometry is authoritative. Never null.
     */
    private CoordinateReferenceSystem fallbackCRS;

    public DefaultGeometryCollection(GeometryType elementType, T[] geometries) {
        this(elementType, null, geometries);
    }

    /**
     * Creates a collection which reports the given coordinate reference system
     * when {@code geometries} is empty.
     */
    public DefaultGeometryCollection(GeometryType elementType, CoordinateReferenceSystem fallbackCRS, T[] geometries) {
        this.elementType = elementType;
        if (geometries != null) {
            for (int i = 0; i < geometries.length; i++) {
                ArgumentChecks.ensureNonNullElement("geometries", i, geometries[i]);
            }
            this.geometries.addAll(List.of(geometries));
        }
        this.fallbackCRS = (fallbackCRS != null) ? fallbackCRS : Geometries.getUndefinedCRS(2);
    }

    @Override
    public GeometryType getElementType() {
        return elementType;
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return (!geometries.isEmpty()) ? geometries.get(0).getCoordinateReferenceSystem() : fallbackCRS;
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("crs", crs);
        fallbackCRS = crs;
        for (T c : geometries) {
            c.setCoordinateReferenceSystem(crs);
        }
    }

    @Override
    public int getNumGeometries() {
        return geometries.size();
    }

    @Override
    public T getGeometryN(int n) {
        return geometries.get(n);
    }

    @Override
    public Geometry getBoundary() {
        if (isEmpty()) {
            // The boundary of the empty set is empty.
            return GeometryFactory.DEFAULT.createEmpty(getCoordinateReferenceSystem());
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

    /**
     * Add primitives.
     *
     * @param other Primitives to merge in, without null element.
     */
    public void append(Collection<? extends Geometry> other) {
        int i = 0;
        for (Geometry p : other) {
            ArgumentChecks.ensureNonNullElement("other", i++, p);
            if (!CRS.equivalent(fallbackCRS, p.getCoordinateReferenceSystem())) {
                throw new IllegalArgumentException("Primitives must have the same CRS as the MultiPrimitive.");
            }
        }
        geometries.addAll((Collection) other);
    }

    @Override
    public int hashCode() {
        return 23 * Objects.hashCode(this.geometries);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DefaultGeometryCollection other = (DefaultGeometryCollection) obj;
        if (!Objects.equals(this.geometries, other.geometries)) {
            return false;
        }
        return Objects.equals(this.getCoordinateReferenceSystem(), other.getCoordinateReferenceSystem());
    }
}
