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
package org.apache.sis.storage.landsat;

import org.opengis.util.InternationalString;
import org.apache.sis.util.SimpleInternationalString;


/**
 * Names of Landsat bands.
 *
 * @todo Those names and the wavelength could be moved to the {@code SpatialMetadata} database,
 *       as described in <a href="https://issues.apache.org/jira/browse/SIS-338">SIS-338</a>.
 *       It would make easier to enrich the metadata with more information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
enum BandName {
    COASTAL_AEROSOL(BandGroupName.REFLECTIVE,   "Coastal Aerosol",                    (short)   433),
    BLUE           (BandGroupName.REFLECTIVE,   "Blue",                               (short)   482),
    GREEN          (BandGroupName.REFLECTIVE,   "Green",                              (short)   562),
    RED            (BandGroupName.REFLECTIVE,   "Red",                                (short)   655),
    NEAR_INFRARED  (BandGroupName.REFLECTIVE,   "Near-Infrared",                      (short)   865),
    SWIR1          (BandGroupName.REFLECTIVE,   "Short Wavelength Infrared (SWIR) 1", (short)  1610),
    SWIR2          (BandGroupName.REFLECTIVE,   "Short Wavelength Infrared (SWIR) 2", (short)  2200),
    PANCHROMATIC   (BandGroupName.PANCHROMATIC, "Panchromatic",                       (short)   590),
    CIRRUS         (BandGroupName.REFLECTIVE,   "Cirrus",                             (short)  1375),
    TIRS1          (BandGroupName.THERMAL,      "Thermal Infrared Sensor (TIRS) 1",   (short) 10800),
    TIRS2          (BandGroupName.THERMAL,      "Thermal Infrared Sensor (TIRS) 2",   (short) 12000);

    /**
     * Group in which this band belong.
     */
    final BandGroupName group;

    /**
     * Localized name of Landsat band.
     */
    final InternationalString title;

    /**
     * Peak response wavelength for the Landsat band, in nanometres.
     * If this band does not contains measurements in electromagnetic spectrum,
     * then this value is set to zero.
     */
    final short wavelength;

    /**
     * Creates a new enumeration value.
     */
    private BandName(final BandGroupName group, final String name, final short wavelength) {
        this.group      = group;
        this.title      = new SimpleInternationalString(name);
        this.wavelength = wavelength;
    }
}
