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
import org.apache.sis.geometries.CompoundCurve;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.math.Array;


/**
 * A curve made of several curves joined end to end.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DefaultCompoundCurve extends AbstractGeometry implements CompoundCurve {

    private final Curve[] curves;

    /**
     * The coordinate reference system to report when this curve has no component.
     * Ignored otherwise, in which case the first component is authoritative. May be {@code null}.
     */
    private CoordinateReferenceSystem fallbackCRS;

    public DefaultCompoundCurve(Curve... curves) {
        this(null, curves);
    }

    /**
     * Creates a compound curve which reports the given coordinate reference system
     * when {@code curves} is empty.
     */
    public DefaultCompoundCurve(CoordinateReferenceSystem fallbackCRS, Curve... curves) {
        this.curves      = curves;
        this.fallbackCRS = fallbackCRS;
    }

    @Override
    public int getNumCurves() {
        return curves.length;
    }

    @Override
    public Curve getCurveN(int n) {
        return curves[n];
    }

    @Override
    public boolean isEmpty() {
        for (final Curve c : curves) {
            if (!c.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return (curves.length != 0) ? curves[0].getCoordinateReferenceSystem() : fallbackCRS;
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) throws IllegalArgumentException {
        this.fallbackCRS = crs;
        for (final Curve c : curves) {
            c.setCoordinateReferenceSystem(crs);
        }
    }

    @Override
    public AttributesType getAttributesType() {
        return (curves.length != 0) ? curves[0].getAttributesType() : AttributesType.EMPTY;
    }

    @Override
    public Envelope getEnvelope() {
        return envUnion(curves);
    }

    @Override
    public String asText() {
        final StringBuilder sb = new StringBuilder(TYPE).append(" (");
        for (int i = 0; i < curves.length; i++) {
            if (i != 0) sb.append(", ");
            sb.append(curves[i].asText());
        }
        return sb.append(')').toString();
    }

    @Override
    public Array getControlPoints() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Array getDataPoints() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
