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
import org.apache.sis.geometries.TIN;
import org.apache.sis.geometries.Triangle;


/**
 * A surface made entirely of triangular patches.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DefaultTriangulatedSurface extends DefaultPolyhedralSurface<Triangle> implements TIN {

    public DefaultTriangulatedSurface(Triangle... patches) {
        super(null, patches);
    }

    public DefaultTriangulatedSurface(CoordinateReferenceSystem fallbackCRS, Triangle[] patches) {
        super(fallbackCRS, patches);
    }

    /**
     * Returns {@value TIN#TYPE}, not {@code "POLYHEDRALSURFACE"}.
     */
    @Override
    public String getGeometryType() {
        return TIN.TYPE;
    }

    /**
     * Delegates to {@link TIN#asText()}, which formats the triangles as a WKT {@code TIN}.
     */
    @Override
    public String asText() {
        return TIN.super.asText();
    }
}
