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

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class DefaultGeometryCollection<T extends Geometry> extends AbstractGeometry implements GeometryCollection<T> {

    private final T[] geometries;

    /**
     * The coordinate reference system to report when this collection is empty.
     * Ignored when at least one geometry is present, in which case the first
     * geometry is authoritative. May be {@code null}.
     */
    private CoordinateReferenceSystem fallbackCRS;

    public DefaultGeometryCollection(T[] geometries) {
        this(null, geometries);
    }

    /**
     * Creates a collection which reports the given coordinate reference system
     * when {@code geometries} is empty.
     */
    public DefaultGeometryCollection(CoordinateReferenceSystem fallbackCRS, T[] geometries) {
        this.geometries  = geometries;
        this.fallbackCRS = fallbackCRS;
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return (geometries.length != 0) ? geometries[0].getCoordinateReferenceSystem() : fallbackCRS;
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) throws IllegalArgumentException {
        fallbackCRS = crs;
        for (T c : geometries) {
            c.setCoordinateReferenceSystem(crs);
        }
    }

    @Override
    public int getNumGeometries() {
        return geometries.length;
    }

    @Override
    public T getGeometryN(int n) {
        return geometries[n];
    }

}
