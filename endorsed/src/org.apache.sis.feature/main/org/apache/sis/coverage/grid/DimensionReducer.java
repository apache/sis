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
package org.apache.sis.coverage.grid;

import java.util.Arrays;
import org.opengis.util.FactoryException;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.internal.shared.AxisDirections;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.geometry.GeneralEnvelope;


/**
 * An helper class for reducing the number of dimensions in a grid geometry, an envelope or a position.
 * This is used when the Area Of Interest has more dimensions than the grid geometry, in which case the
 * transform from grid to AOI will fail if we do not discard the extra dimensions.
 *
 * <p>This class works on the CRS dimensions.
 * This is different than {@link DimensionalityReduction}, which works on grid dimensions.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class DimensionReducer {
    /**
     * The dimensions to keep, or {@code null} for keeping them all.
     */
    private int[] dimensions;

    /**
     * The CRS with only the {@linkplain #dimensions} to keep, or {@code null} if no reduction.
     */
    private CoordinateReferenceSystem reducedCRS;

    /**
     * Creates a helper which will retain only the {@code targetCRS} dimensions that are found in the base grid.
     * This will be used by caller for creating a valid {@code sourceCRS} to {@code targetCRS} transform.
     *
     * @param  base       the grid geometry which will be derived. Cannot be null.
     * @param  targetCRS  CRS of the area or point of interest. Cannot be null.
     */
    DimensionReducer(final GridGeometry base, final CoordinateReferenceSystem targetCRS) throws FactoryException {
        if (base != null && base.envelope != null) {
            final CoordinateReferenceSystem sourceCRS = base.envelope.getCoordinateReferenceSystem();
            if (sourceCRS != null) {
                final CoordinateSystem sourceCS = sourceCRS.getCoordinateSystem();
                final CoordinateSystem targetCS = targetCRS.getCoordinateSystem();
                if (sourceCS.getDimension() < targetCS.getDimension()) {
                    dimensions = AxisDirections.indicesOfLenientMapping(targetCS, sourceCS);
                    if (dimensions != null) {
                        Arrays.sort(dimensions);
                        reducedCRS = CRS.selectDimensions(targetCRS, dimensions);
                    }
                }
            }
        }
    }

    /**
     * Applies reduction on the given position.
     * If the position cannot be reduced, then it is returned as-is.
     */
    final DirectPosition apply(final DirectPosition target) {
        if (dimensions == null) {
            return target;
        }
        final GeneralDirectPosition position = new GeneralDirectPosition(reducedCRS);
        for (int i=0; i < dimensions.length; i++) {
            position.coordinates[i] = target.getOrdinate(dimensions[i]);
        }
        return position;
    }

    /**
     * Applies reduction on the given envelope.
     * If the envelope cannot be reduced, then it is returned as-is.
     */
    final Envelope apply(final Envelope target) {
        if (dimensions == null) {
            return target;
        }
        final DirectPosition lowerCorner = target.getLowerCorner();
        final DirectPosition upperCorner = target.getUpperCorner();
        final GeneralEnvelope envelope = new GeneralEnvelope(reducedCRS);
        for (int i=0; i < dimensions.length; i++) {
            final int s = dimensions[i];
            envelope.setRange(i, lowerCorner.getOrdinate(s), upperCorner.getOrdinate(s));
        }
        return envelope;
    }
}
