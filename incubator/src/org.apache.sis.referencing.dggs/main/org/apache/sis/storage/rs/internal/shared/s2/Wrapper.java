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
package org.apache.sis.storage.rs.internal.shared.s2;

import com.google.common.geometry.S2Region;
import java.util.Iterator;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * The wrapper of S2 geometries.
 *
 * @author Johann Sorel (Geomatys)
 */
final class Wrapper extends GeometryWrapper {

    /**
     * The wrapped implementation.
     */
    private final S2Region geometry;

    /**
     * Creates a new wrapper around the given geometry.
     */
    Wrapper(final S2Region geometry) {
        this.geometry = geometry;
    }

    /**
     * Returns the implementation-dependent factory of geometric object.
     */
    @Override
    public Geometries<S2Region> factory() {
        return Factory.INSTANCE;
    }

    /**
     * Returns the geometry specified at construction time.
     */
    @Override
    protected S2Region implementation() {
        return geometry;
    }

    @Override
    public GeneralEnvelope getEnvelope() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DirectPosition getCentroid() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public double[] getPointCoordinates() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public double[] getAllCoordinates() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Object mergePolylines(Iterator<?> paths) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public String formatWKT(double flatness) {
        throw new UnsupportedOperationException("Unsupported operation");
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}
