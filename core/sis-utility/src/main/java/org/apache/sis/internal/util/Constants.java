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
package org.apache.sis.internal.util;

import org.apache.sis.util.Static;


/**
 * Hard coded values (typically identifiers).
 * The set of constants defined in this class may change in any SIS version - do not rely on them.
 *
 * {@section When to use}
 * Those constants should be used mostly for names, aliases or identifiers. They should generally
 * not be used for abbreviations for instance, even if the abbreviation result in the same string.
 *
 * Those constants do not need to be used systematically in tests neither, especially when the test
 * builds its tested object itself.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.6
 * @module
 */
public final class Constants extends Static {
    /**
     * The {@value} code space.
     */
    public static final String EPSG = "EPSG";

    /**
     * The {@value} code space.
     */
    public static final String OGC = "OGC";

    /**
     * The {@value} code space.
     */
    public static final String SIS = "SIS";

    /**
     * The {@value} code space.
     */
    public static final String CRS = "CRS";

    /**
     * The {@code CRS:27} identifier for a coordinate reference system.
     */
    public static final byte CRS27 = 27;

    /**
     * The {@code CRS:83} identifier for a coordinate reference system.
     */
    public static final byte CRS83 = 83;

    /**
     * The {@code CRS:84} identifier for a coordinate reference system.
     */
    public static final byte CRS84 = 84;

    /**
     * Name of the {@value} projection parameter, which is handled specially during WKT formatting.
     */
    public static final String SEMI_MAJOR = "semi_major", SEMI_MINOR = "semi_minor";

    /**
     * Name of the {@value} matrix parameters.
     */
    public static final String NUM_ROW = "num_row", NUM_COL = "num_col";

    /**
     * EPSG code of the {@code A0} coefficient used in affine (general parametric) and polynomial transformations.
     * Codes for parameters {@code A1} to {@code A8} inclusive follow, but the affine coefficients stop at {@code A2}.
     */
    public static final short A0 = 8623;

    /**
     * EPSG code of the {@code B0} coefficient used in affine (general parametric) and polynomial transformations.
     * Codes for parameters {@code B1} to {@code B3} inclusive follow, but the affine coefficients stop at {@code B2}.
     */
    public static final short B0 = 8639;

    /**
     * Do not allow instantiation of this class.
     */
    private Constants() {
    }
}
