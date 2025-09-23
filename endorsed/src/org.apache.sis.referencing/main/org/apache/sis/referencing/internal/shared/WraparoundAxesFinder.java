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
package org.apache.sis.referencing.internal.shared;

import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;


/**
 * Finds the axes where wraparound may happen in a CRS. The search may be indirect.
 * For example if the given CRS is projected, this class will search in geographic CRS.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class WraparoundAxesFinder {
    /**
     * The CRS that may contain wraparound axes. Geographic CRS are preferred,
     * but will be the CRS specified at construction time if we found nothing better.
     */
    public final CoordinateReferenceSystem preferredCRS;

    /**
     * The transform from {@link #preferredCRS} to the CRS specified at construction time.
     * Never null but may be the identity transform.
     */
    public final MathTransform preferredToSpecified;

    /**
     * Searches wraparound axes in the specified CRS or its base CRS (if any).
     *
     * @param  crs  the CRS where to search for wraparound axes.
     */
    public WraparoundAxesFinder(CoordinateReferenceSystem crs) {
        if (crs instanceof ProjectedCRS) {
            final ProjectedCRS p = (ProjectedCRS) crs;
            crs = p.getBaseCRS();       // Geographic, so a wraparound axis certainly exists.
            preferredToSpecified = p.getConversionFromBase().getMathTransform();
        } else {
            // TODO: we should handle the case of CompoundCRS before to fallback on identity.
            preferredToSpecified = MathTransforms.identity(ReferencingUtilities.getDimension(crs));
        }
        preferredCRS = crs;
    }

    /**
     * Returns the range (maximum - minimum) of wraparound axes. For non-wraparound axes, the value is set to 0.
     * The length of this array is the smallest length necessary for handing all wraparound axes.
     * It may be smaller than the CRS dimension.
     *
     * @return periods of axes (0 for non-wraparound axes), or {@code null} if none.
     */
    public double[] periods() {
        double[] periods = null;
        final CoordinateSystem cs = preferredCRS.getCoordinateSystem();
        for (int i = cs.getDimension(); --i >= 0;) {
            final double period = WraparoundApplicator.range(cs, i);
            if (period > 0) {
                if (periods == null) {
                    periods = new double[i + 1];
                }
                periods[i] = period;
            }
        }
        return periods;
    }
}
