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
package org.apache.sis.referencing;

import java.util.Date;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.TemporalDatum;
import org.opengis.test.Validators;
import org.apache.sis.test.mock.GeodeticDatumMock;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.TestUtilities.*;


/**
 * Tests the {@link GeodeticObjects} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.2)
 * @version 0.4
 * @module
 */
@DependsOn({
  org.apache.sis.referencing.datum.DefaultGeodeticDatumTest.class,
  org.apache.sis.referencing.datum.DefaultVerticalDatumTest.class
})
public final strictfp class GeodeticObjectsTest extends TestCase {
    /**
     * Length of a day in milliseconds.
     */
    private static final double DAY_LENGTH = 24 * 60 * 60 * 1000;

    /**
     * Compares the {@link GeodeticDatumMock} constants with the {@link GeodeticObjects} ones.
     * This is more a {@code GeodeticDatumMock} test than a {@code GeodeticObjects} one, but can
     * hardly be defined elsewhere since we need some reference objects for comparing the values.
     */
    @Test
    public void testGeodeticDatumMocks() {
        final GeodeticDatum[] mocks = new GeodeticDatum[] {
            GeodeticDatumMock.WGS84,
            GeodeticDatumMock.WGS72,
            GeodeticDatumMock.NAD83,
            GeodeticDatumMock.NAD27,
            GeodeticDatumMock.SPHERE
        };
        final GeodeticObjects[] enums = new GeodeticObjects[] {
            GeodeticObjects.WGS84,
            GeodeticObjects.WGS72,
            GeodeticObjects.NAD83,
            GeodeticObjects.NAD27,
            GeodeticObjects.SPHERE
        };
        assertEquals(mocks.length, enums.length);
        for (int i=0; i<mocks.length; i++) {
            final Ellipsoid mock = mocks[i].getEllipsoid();
            final Ellipsoid ref  = enums[i].ellipsoid();
            assertEquals("semiMajorAxis",     ref.getSemiMajorAxis(),     mock.getSemiMajorAxis(), 0);
            assertEquals("semiMinorAxis",     ref.getSemiMinorAxis(),     mock.getSemiMinorAxis(), 0);
            assertEquals("inverseFlattening", ref.getInverseFlattening(), mock.getInverseFlattening(), 1E-11);
            assertEquals("isIvfDefinitive",   ref.isIvfDefinitive(),      mock.isIvfDefinitive());
            assertEquals("isSphere",          ref.isSphere(),             mock.isSphere());
        }
    }

    /**
     * Verifies the epoch values of temporal enumeration compared to the Julian epoch.
     *
     * @see <a href="http://en.wikipedia.org/wiki/Julian_day">Wikipedia: Julian day</a>
     */
    @Test
    public void testTemporal() {
        final double epoch = GeodeticObjects.Temporal.JULIAN.datum().getOrigin().getTime() / DAY_LENGTH;
        assertTrue(epoch < 0);
        assertEquals(2400000.5, julian("1858-11-17 00:00:00", GeodeticObjects.Temporal.MODIFIED_JULIAN)  - epoch, 0);
        assertEquals(2440000.5, julian("1968-05-24 00:00:00", GeodeticObjects.Temporal.TRUNCATED_JULIAN) - epoch, 0);
        assertEquals(2415020.0, julian("1899-12-31 12:00:00", GeodeticObjects.Temporal.DUBLIN_JULIAN)    - epoch, 0);
        assertEquals(2440587.5, julian("1970-01-01 00:00:00", GeodeticObjects.Temporal.UNIX)             - epoch, 0);
    }

    /**
     * Validates the datum of the given definition, compares its epoch with the given ISO date
     * and returns its Julian day.
     */
    private static double julian(final String epoch, final GeodeticObjects.Temporal def) {
        final TemporalDatum datum = def.datum();
        Validators.validate(datum);
        final Date origin = datum.getOrigin();
        assertEquals(def.name(), epoch, format(origin));
        return origin.getTime() / DAY_LENGTH;
    }
}
