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
package org.apache.sis.internal.geoapi.temporal;

import org.opengis.util.InternationalString;


/**
 * Placeholder for a GeoAPI interfaces which is still incomplete in GeoAPI 3.0.0.
 * We reproduce here the GeoAPI 3.1-pending API. Note that at the time of writing,
 * this is a bad API (values shall not be instances of {@link InternationalString}).
 * This will be fixed in a future GeoAPI version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public interface PeriodDuration extends org.opengis.temporal.PeriodDuration {
    /**
     * A positive integer, followed by the character "Y",
     * which indicated the number of years in the period.
     */
    InternationalString getYears();

    /**
     * A positive integer, followed by the character "M",
     * which indicated the number of months in the period.
     */
    InternationalString getMonths();

    /**
     * A positive integer, followed by the character "D",
     * which indicated the number of days in the period.
     */
    InternationalString getDays();

    /**
     * A positive integer, followed by the character "H",
     * which indicated the number of hours in the period.
     */
    InternationalString getHours();

    /**
     * A positive integer, followed by the character "M",
     * which indicated the number of minutes in the period.
     */
    InternationalString getMinutes();

    /**
     * A positive integer, followed by the character "S",
     * which indicated the number of seconds in the period.
     */
    InternationalString getSeconds();
}
