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
package org.apache.sis.internal.referencing.provider;


/**
 * The method to use for applying a datum shift.
 * Values are ordered from most accurate method to less accurate.
 * This enumeration does not include datum shift based on grid files.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public enum DatumShiftMethod {
    /**
     * Transformation in geocentric domain (translation or position vector 7-parameters).
     */
    GEOCENTRIC_DOMAIN,

    /**
     * Approximation of geocentric translations.
     */
    MOLODENSKY,

    /**
     * Approximation of Molodensky transformations,
     * which is itself an approximation of geocentric translations.
     */
    ABRIDGED_MOLODENSKY,

    /**
     * No datum shift applied (not even a change of ellipsoid).
     * This method should be used only when a coarse accuracy (at least 3 km) is sufficient.
     */
    NONE;

    /**
     * Suggests an operation method for the given accuracy. This method contains accuracy threshold (in metres)
     * for allowing the use of Molodensky approximation instead of the Geocentric Translation method.
     * The accuracy of datum shifts with Molodensky approximation is about 5 or 10 metres.
     * However for this method, we are not interested in absolute accuracy but rather in the difference
     * between Molodensky and Geocentric Translation methods, which is much lower (less than 1 m).
     * We nevertheless use a relatively high threshold as a conservative approach.
     *
     * @param  desired  desired accuracy on Earth (metres).
     * @return suggested datum shift method for an accuracy equals or better than the given value.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-369">SIS-369</a>
     */
    public static DatumShiftMethod forAccuracy(final double desired) {
        if (desired >= 5) {
            return MOLODENSKY;
        } else {
            return GEOCENTRIC_DOMAIN;
        }
    }
}
