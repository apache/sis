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
package org.apache.sis.internal.referencing;

import org.apache.sis.util.Static;
import org.apache.sis.measure.Latitude;

import static java.lang.Math.*;


/**
 * Miscellaneous utilities which should not be put in public API.
 * This class contains methods that depend on hard-coded arbitrary tolerance threshold, and we
 * do not want to expose publicly those arbitrary values (or at least not in a too direct way).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
public final class Utilities extends Static {
    /**
     * Default tolerance threshold for comparing ordinate values in a projected CRS,
     * assuming that the unit of measurement is metre. This is not a tolerance for
     * testing map projection accuracy.
     *
     * @see #ANGULAR_TOLERANCE
     * @see org.apache.sis.internal.util.Utilities#COMPARISON_THRESHOLD
     */
    public static final double LINEAR_TOLERANCE = 1.0;

    /**
     * Default tolerance threshold for comparing ordinate values in a geographic CRS,
     * assuming that the unit of measurement is decimal degrees and using the standard
     * nautical mile length.
     *
     * @see #LINEAR_TOLERANCE
     * @see org.apache.sis.internal.util.Utilities#COMPARISON_THRESHOLD
     */
    public static final double ANGULAR_TOLERANCE = LINEAR_TOLERANCE / (1852 * 60);

    /**
     * Do not allow instantiation of this class.
     */
    private Utilities() {
    }

    /**
     * Returns {@code true} if {@code ymin} is the south pole and {@code ymax} is the north pole.
     *
     * @param ymin The minimal latitude to test.
     * @param ymax The maximal latitude to test.
     * @return {@code true} if the given latitudes are south pole to north pole respectively.
     */
    public static boolean isPoleToPole(final double ymin, final double ymax) {
        return abs(ymin - Latitude.MIN_VALUE) <= ANGULAR_TOLERANCE &&
               abs(ymax - Latitude.MAX_VALUE) <= ANGULAR_TOLERANCE;
    }
}
