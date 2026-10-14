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
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.curve.CompoundCurve;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.util.ArgumentChecks;


/**
 * A curve made of several curves joined end to end.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultCompoundCurve extends AbstractGeometry implements CompoundCurve {

    private final Curve[] curves;

    /**
     * The coordinate reference system to report when this curve has no component.
     * Ignored otherwise, in which case the first component is authoritative. Never null.
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
        this.fallbackCRS = (fallbackCRS != null) ? fallbackCRS : Geometries.getUndefinedCRS(2);
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
        ArgumentChecks.ensureNonNull("crs", crs);
        this.fallbackCRS = crs;
        for (final Curve c : curves) {
            c.setCoordinateReferenceSystem(crs);
        }
    }

    @Override
    public DataPointsType getDataPointsType() {
        return (curves.length != 0) ? curves[0].getDataPointsType() : DataPointsType.EMPTY;
    }

    @Override
    public Envelope getEnvelope() {
        return envUnion(curves);
    }

    /**
     * Returns the control points of the components, in the order of the components.
     * A component without control points, such as a line string, contributes none.
     *
     * @return control points of the components, or {@code null} if none of them has control points.
     */
    @Override
    public Array getControlPoints() {
        final var arrays = new ArrayList<Array>(curves.length);
        DataType dataType = null;
        boolean mixed = false;
        for (final Curve curve : curves) {
            final Array array = curve.getControlPoints();
            if (array != null && array.getLength() != 0) {
                arrays.add(array);
                if (dataType == null) {
                    dataType = array.getDataType();
                } else {
                    mixed |= (dataType != array.getDataType());
                }
            }
        }
        switch (arrays.size()) {
            case 0: return null;
            case 1: return arrays.get(0);
        }
        if (mixed) {
            arrays.replaceAll((array) -> array.retype(DataType.DOUBLE));
        }
        return NDArrays.concatenate(arrays.toArray(Array[]::new));
    }

}
