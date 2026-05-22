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
package org.apache.sis.referencing.datum;

import java.util.Map;
import java.util.List;
import org.opengis.referencing.datum.GeodeticDatum;
import org.apache.sis.metadata.iso.quality.DefaultAbsoluteExternalPositionalAccuracy;
import org.apache.sis.referencing.GeodeticException;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link DefaultDatumEnsemble} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public class DefaultDatumEnsembleTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultDatumEnsembleTest() {
    }

    /**
     * Tests the creation of a datum ensemble with some arbitrary geodetic datum.
     */
    @Test
    public void testGeodetic() {
        final DefaultDatumEnsemble<GeodeticDatum> ensemble = DefaultDatumEnsemble.create(
                Map.of(DefaultDatumEnsemble.NAME_KEY, "Any WGS"),
                GeodeticDatum.class,
                List.of(HardCodedDatum.WGS84, HardCodedDatum.WGS72),
                new DefaultAbsoluteExternalPositionalAccuracy());

        assertTrue(ensemble.getMembers().contains(HardCodedDatum.WGS84));
        assertTrue(ensemble.getMembers().contains(HardCodedDatum.WGS72));
        final GeodeticDatum geodetic = assertInstanceOf(GeodeticDatum.class, ensemble);
        assertEquals("DatumEnsemble<GeodeticDatum>", ensemble.getStandardType().getTypeName());
        assertEquals(0, geodetic.getPrimeMeridian().getGreenwichLongitude());
        GeodeticException e = assertThrows(GeodeticException.class, () -> geodetic.getEllipsoid());
        assertMessageContains(e, "WGS");
    }
}
