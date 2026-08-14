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
package org.apache.sis.referencing.dggs.internal.shared;

import javax.measure.Quantity;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.dggs.DiscreteGlobalGrid;
import org.apache.sis.referencing.operation.transform.AbstractMathTransform;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class AbstractDiscreteGlobalGrid<T extends AbstractDiscreteGlobalGridHierarchy> implements DiscreteGlobalGrid {

    protected final T hierarchy;
    protected final int level;

    //cache transforms
    private CoordinateReferenceSystem crs;
    private int crsDim;
    private MathTransform crsToId;
    private MathTransform idToCrs;

    public AbstractDiscreteGlobalGrid(T hierarchy, int level) {
        this.hierarchy = hierarchy;
        this.level = level;
    }

    @Override
    public Quantity<?> getPrecision() {
        final double area = this.hierarchy.dggrs.getGridSystem().getCelestialBodySurface() / getZoneCount();
        return Quantities.create(Math.sqrt(area), Units.METRE);
    }

    @Override
    public final int getRefinementLevel() {
        return level;
    }

    /**
     * Create the long identifer to/from transforms once.
     * @return true if suceeded
     */
    private synchronized boolean initTransform() {
        if (crs != null) return crsToId != null;

        crs = hierarchy.dggrs.getGridSystem().getCrs();
        crsDim = crs.getCoordinateSystem().getDimension();
        if (!hierarchy.dggrs.getZonalSystem().supportUInt64Form()) return false;

        idToCrs = new AbstractMathTransform() {
            @Override
            public int getSourceDimensions() {
                return 1;
            }

            @Override
            public int getTargetDimensions() {
                return crsDim;
            }

            @Override
            public Matrix transform(double[] source, int soffset, double[] target, int toffset, boolean bln) throws TransformException {
                final long zone = getZoneLongIdentifier(source, soffset);
                target[toffset] = Double.longBitsToDouble(zone);
                return null;
            }
        };
        crsToId = new AbstractMathTransform() {
            @Override
            public int getSourceDimensions() {
                return crsDim;
            }

            @Override
            public int getTargetDimensions() {
                return 1;
            }

            @Override
            public Matrix transform(double[] source, int soffset, double[] target, int toffset, boolean bln) throws TransformException {
                getZonePosition(Double.doubleToRawLongBits(source[soffset]), target, toffset);
                return null;
            }
        };

        return true;
    }

    @Override
    public final MathTransform createTransformToCrs() throws UnsupportedOperationException {
        if (initTransform()) throw new UnsupportedOperationException();
        return idToCrs;
    }

    @Override
    public final MathTransform createTransformToIdentifiers() throws UnsupportedOperationException {
        if (initTransform()) throw new UnsupportedOperationException();
        return crsToId;
    }

    /**
     * Convert on coordinate in base CRS to zone long identifier.
     */
    protected abstract long getZoneLongIdentifier(double[] source, int soffset);

    /**
     * Convert zone long identifier to coordinate in base CRS.
     */
    protected abstract void getZonePosition(long zoneId, double[] target, int toffset);

}
