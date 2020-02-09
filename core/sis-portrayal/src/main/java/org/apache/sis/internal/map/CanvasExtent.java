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
package org.apache.sis.internal.map;

import java.util.List;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.internal.util.Numerics;


/**
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class CanvasExtent {
    private CanvasExtent() {
    }

    /**
     * Finds the dimensions in the given CRS that are not included in the objective CRS.
     * Those dimensions are discovered by inspection of the derivative of the transform
     * from the given CRS to the objective CRS.  In addition, this method also adds the
     * CRS component of those supplemental dimensions in the given list. If a component
     * can not be separated from the CRS, then current implementation excludes it from
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
         * Now we know the source dimensions of the CRS components to add in the specified list.
         * We must ask for CRS components using ranges as much as possible. For example if some
         * supplemental dimensions are 1,2,3 then we must as the component in range 1 inclusive
         * to 4 exclusive. This is done easily we bits arithmetic. If we can get all components,
         * the `supplementalDimensions` final value will be the `mask` initial value. If we had
         * to discard some components, then those long values will differ.
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
     * @param  pointOfInterest         value of {@link Canvas#getPointOfInterest()}.
     * @param  supplementalDimensions  value of {@link #findSupplementalDimensions(CoordinateReferenceSystem, Matrix, List)}.
     * @return the "grid to CRS" transform of a grid geometry for a {@link Canvas}.
     */
    static LinearTransform createGridToCRS(final Matrix displayToObjective,
            final DirectPosition pointOfInterest, long supplementalDimensions)
    {
        final int srcDim = displayToObjective.getNumCol();
        final int tgtDim = displayToObjective.getNumRow() + Long.bitCount(supplementalDimensions);
        final MatrixSIS gridToCRS = Matrices.createIdentity(tgtDim + 1);
        while (supplementalDimensions != 0) {
            final int n = Long.numberOfTrailingZeros(supplementalDimensions);
            gridToCRS.setElement(n, tgtDim, pointOfInterest.getOrdinate(n));
            supplementalDimensions &= ~(1L << n);
        }
        for (int j=0; j<tgtDim; j++) {
            for (int i=0; i<srcDim; i++) {
                gridToCRS.setElement(j,i, displayToObjective.getElement(j,i));
            }
        }
        return MathTransforms.linear(gridToCRS);
    }
}
