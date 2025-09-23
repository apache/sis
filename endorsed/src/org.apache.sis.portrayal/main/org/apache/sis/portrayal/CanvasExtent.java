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
package org.apache.sis.portrayal;

import java.util.List;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.metadata.spatial.DimensionNameType;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.internal.shared.AxisDirections;
import org.apache.sis.util.internal.shared.Numerics;


/**
 * A {@link GridExtent} which remembers the {@link Canvas#getPointOfInterest(boolean)} coordinates.
 * This class also contains static help functions for the construction of {@link GridGeometry}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class CanvasExtent extends GridExtent {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 195789629521760720L;

    /**
     * The grid coordinates of a representative point. The {@code CanvasExtent} point of interest
     * is the {@code Canvas} point of interest converted to (typically) pixel coordinates.
     *
     * @see #getPointOfInterest(PixelInCell)
     */
    private final double[] pointOfInterest;

    /**
     * Creates a new grid extent.
     *
     * @param  axisTypes        the type of each grid axis, or {@code null} if unspecified.
     * @param  lower            the valid minimum grid coordinates (inclusive).
     * @param  upper            the valid maximum grid coordinates (exclusive).
     * @param  pointOfInterest  the grid coordinates of a representative point.
     */
    private CanvasExtent(final DimensionNameType[] axisTypes, final long[] lower, final long[] upper, final double[] pointOfInterest) {
        super(axisTypes, lower, upper, false);
        this.pointOfInterest = pointOfInterest;
    }

    /**
     * Returns the grid coordinates of a representative point.
     * This is the canvas point of interest converted to (typically) pixel coordinates.
     *
     * @param  anchor  the convention to be used for conversion to "real world" coordinates.
     * @return the grid coordinates of a representative point.
     *
     * @see Canvas#getPointOfInterest(boolean)
     */
    @Override
    public double[] getPointOfInterest(final PixelInCell anchor) {
        return pointOfInterest.clone();
    }

    /**
     * Creates a new grid extent from the given display bounds.
     * All supplemental dimensions will have the [0 … 0] grid range.
     * If a point of interest is available, its coordinates will be remembered.
     *
     * @param  bounds     bounds of the display device, typically in pixel units.
     * @param  poi        point of interest in pixel units, or {@code null} if unknown.
     * @param  axisTypes  name of display axes, or {@code null} if unknown.
     * @param  agmDim     augmented number of dimensions (i.e. including supplemental dimensions).
     * @return grid extent computed from the given display bounds.
     */
    static GridExtent create(final GeneralEnvelope bounds, final DirectPosition poi,
                             final DimensionNameType[] axisTypes, final int agmDim)
    {
        final long[] lower = new long[agmDim];
        final long[] upper = new long[agmDim];
        for (int i = bounds.getDimension(); --i >= 0;) {
            lower[i] = (long) Math.floor(bounds.getLower(i));
            upper[i] = (long) Math.ceil (bounds.getUpper(i));
        }
        if (poi == null) {
            return new GridExtent(axisTypes, lower, upper, false);
        }
        final double[] c = new double[agmDim];
        for (int i = poi.getDimension(); --i >= 0;) {
            c[i] = poi.getOrdinate(i);
        }
        return new CanvasExtent(axisTypes, lower, upper, c);
    }

    /**
     * Finds the dimensions in the given CRS that are not included in the objective CRS.
     * Those dimensions are discovered by inspection of the derivative of the transform
     * from the given CRS to the objective CRS.  In addition, this method also adds the
     * CRS component of those supplemental dimensions in the given list. If a component
     * cannot be separated from the CRS, then current implementation excludes it from
     * the set of supplemental dimensions.
     *
     * @param  crs         the coordinate reference system of the Point Of Interest (POI).
     * @param  derivative  derivative of the transform from Point Of Interest (POI) to objective CRS.
     * @param  addTo       the list where to add the CRS component for supplemental dimensions.
     * @return a bitmask of supplemental dimensions.
     *
     * @see Canvas#supplementalDimensions
     */
    static long findSupplementalDimensions(final CoordinateReferenceSystem crs, final Matrix derivative,
                                           final List<CoordinateReferenceSystem> addTo)
    {
        /*
         * Creates a mask with bits set to 1 for all source dimensions (columns)
         * that are used by at least one target dimension (row). After the loop,
         * that mask will be reverted and become the set of dimensions NOT used.
         */
        final int srcDim = Math.min(derivative.getNumCol(), Long.SIZE);
        final int dstDim = derivative.getNumRow();
        long mask = 0;                                  // Sources used by any target dimension.
        for (int j=0; j<dstDim; j++) {
            for (int i=0; i<srcDim; i++) {
                if (derivative.getElement(j,i) != 0) {
                    mask |= (1L << i);
                    break;
                }
            }
        }
        mask ^= Numerics.bitmask(srcDim) - 1;           // Sources NOT used by any target dimension.
        /*
         * Now we know the source dimensions of the CRS components to add in the `addTo` list.
         * We must ask for CRS components using ranges as much as possible. For example if some
         * supplemental dimensions are 1,2,3 then we must ask the component in range 1 inclusive
         * to 4 exclusive. This is done easily with bits arithmetic. If we can get all components,
         * the `supplementalDimensions` final value will be the `mask` initial value. If we had to
         * discard some components, then `supplementalDimensions` will have less bits than `mask`.
         */
        long supplementalDimensions = 0;
        while (mask != 0) {
            final int  lower = Long.numberOfTrailingZeros(mask);
            final int  upper = Long.numberOfTrailingZeros(mask + (1L << lower));
            final long clear = -(1L << upper);   // All bits on the right of `upper` are zero.
            final CoordinateReferenceSystem component = CRS.getComponentAt(crs, lower, upper);
            if (component != null) {
                addTo.add(component);
                supplementalDimensions |= (mask & ~clear);
            }
            mask &= clear;
        }
        return supplementalDimensions;
    }

    /**
     * Creates the "grid to CRS" transform of a grid geometry as the inverse of the
     * "objective to display" transform augmented with supplemental dimensions.
     *
     * @param  displayToObjective      inverse of {@link Canvas#getObjectiveToDisplay()}.
     * @param  pointOfInterest         value of {@link Canvas#getPointOfInterest(boolean)}.
     * @param  supplementalDimensions  value of {@link #findSupplementalDimensions(CoordinateReferenceSystem, Matrix, List)}.
     * @return the "grid to CRS" transform of a grid geometry for a {@link Canvas}.
     */
    static LinearTransform createGridToCRS(final Matrix displayToObjective,
            final DirectPosition pointOfInterest, long supplementalDimensions)
    {
        final int srcDim = displayToObjective.getNumCol();
        final int tgtDim = displayToObjective.getNumRow();
        final int agmDim = tgtDim + Long.bitCount(supplementalDimensions);
        final MatrixSIS gridToCRS = Matrices.createZero(agmDim + 1, agmDim + 1);
        int j;
        for (j=0; j<tgtDim; j++) {
            for (int i=0; i<srcDim; i++) {
                gridToCRS.setElement(j,i, displayToObjective.getElement(j,i));
            }
        }
        // Continue adding rows.
        while (supplementalDimensions != 0) {
            final int n = Long.numberOfTrailingZeros(supplementalDimensions);
            gridToCRS.setElement(j, j, Double.NaN);
            gridToCRS.setElement(j++, agmDim, pointOfInterest.getOrdinate(n));
            supplementalDimensions &= ~(1L << n);
        }
        return MathTransforms.linear(gridToCRS);
    }

    /**
     * Suggests axis types for supplemental dimensions not managed by the {@link Canvas}.
     * Those types are only a help for debugging purpose, by providing more information
     * to the developers. They should not be used for any "real" work.
     *
     * @param  crs  the coordinate reference system to use for inferring axis types.
     * @param  displayDimension  number of dimensions managed by the {@link Canvas}.
     * @return suggested axis types. Never null, but contains null elements.
     *
     * @see Canvas#axisTypes
     */
    static DimensionNameType[] suggestAxisTypes(final CoordinateReferenceSystem crs, final int displayDimension) {
        if (crs != null) {
            final CoordinateSystem cs = crs.getCoordinateSystem();
            if (cs != null) {                                       // Should never be null, but we are paranoiac.
                int i = cs.getDimension();
                final DimensionNameType[] axisTypes = new DimensionNameType[i];
                while (--i >= displayDimension) {
                    final AxisDirection dir = AxisDirections.absolute(cs.getAxis(i).getDirection());
                    if (dir == AxisDirection.FUTURE) {
                        axisTypes[i] = DimensionNameType.TIME;
                    } else if (dir == AxisDirection.UP) {
                        axisTypes[i] = DimensionNameType.VERTICAL;
                    }
                }
                return axisTypes;
            }
        }
        return new DimensionNameType[displayDimension];
    }
}
