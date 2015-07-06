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
package org.apache.sis.referencing.operation;

import java.util.List;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.Datum;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.Utilities;


/**
 * Infers a conversion of transformation path from a source CRS to a target CRS.
 *
 * This is currently only a placeholder for future SIS development (code to be ported from Geotk).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
final class OperationPathFinder {
    private OperationPathFinder() {
    }

    /**
     * Returns {@code true} if the given CRS are using equivalent (ignoring metadata) datum.
     * If the CRS are {@link CompoundCRS}, then this method verifies that all datum in the
     * target CRS exists in the source CRS, but not necessarily in the same order.
     * The target CRS may have less datum than the source CRS.
     *
     * @param sourceCRS The target CRS.
     * @param targetCRS The source CRS.
     * @return {@code true} if all datum in the {@code targetCRS} exists in the {@code sourceCRS}.
     */
    static boolean isConversion(final CoordinateReferenceSystem sourceCRS,
                                final CoordinateReferenceSystem targetCRS)
    {
        List<SingleCRS> components = CRS.getSingleComponents(sourceCRS);
        int n = components.size();   // Number of remaining datum from sourceCRS to verify.
        final Datum[] datum = new Datum[n];
        for (int i=0; i<n; i++) {
            datum[i] = components.get(i).getDatum();
        }
        components = CRS.getSingleComponents(targetCRS);
next:   for (int i=components.size(); --i >= 0;) {
            final Datum d = components.get(i).getDatum();
            for (int j=n; --j >= 0;) {
                if (Utilities.equalsIgnoreMetadata(d, datum[j])) {
                    System.arraycopy(datum, j+1, datum, j, --n - j);  // Remove the datum from the list.
                    continue next;
                }
            }
            return false;  // Datum from 'targetCRS' not found in 'sourceCRS'.
        }
        return true;
    }
}
