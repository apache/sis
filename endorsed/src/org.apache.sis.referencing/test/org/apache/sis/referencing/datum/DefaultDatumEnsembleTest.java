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
import org.opengis.metadata.Identifier;
import org.opengis.referencing.datum.GeodeticDatum;
import org.apache.sis.referencing.GeodeticException;
import org.apache.sis.metadata.KeyNamePolicy;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.ValueExistencePolicy;
import org.apache.sis.metadata.iso.quality.DefaultAbsoluteExternalPositionalAccuracy;

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
     * Creates a dummy ensemble with <abbr>WGS</abbr> datum.
     */
    private static DefaultDatumEnsemble<GeodeticDatum> WGS() {
        return DefaultDatumEnsemble.create(
                Map.of(DefaultDatumEnsemble.NAME_KEY, "Various WGS"),
                GeodeticDatum.class,
                List.of(HardCodedDatum.WGS84, HardCodedDatum.WGS72),
                new DefaultAbsoluteExternalPositionalAccuracy());
    }

    /**
     * Tests the creation of a datum ensemble with some arbitrary geodetic datum.
     */
    @Test
    public void testGeodetic() {
        final DefaultDatumEnsemble<GeodeticDatum> ensemble = WGS();
        assertEquals("Various WGS", ensemble.getName().getCode());
        assertTrue(ensemble.getMembers().contains(HardCodedDatum.WGS84));
        assertTrue(ensemble.getMembers().contains(HardCodedDatum.WGS72));
        final GeodeticDatum geodetic = assertInstanceOf(GeodeticDatum.class, ensemble);
        assertEquals("DefaultDatumEnsemble<GeodeticDatum>", ensemble.getStandardType().getTypeName());
        assertEquals(0, geodetic.getPrimeMeridian().getGreenwichLongitude());
        GeodeticException e = assertThrows(GeodeticException.class, () -> geodetic.getEllipsoid());
        assertMessageContains(e, "WGS");
    }

    /**
     * Tests the view as a map. This test depends on {@link DefaultDatumEnsemble#getStandardType()},
     * which is invoked by the metadata module for determining which interface is the main one.
     */
    @Test
    public void testValueMap() {
        final Map<String, Object> values = MetadataStandard.ISO_19111.asValueMap(
                WGS(), null, KeyNamePolicy.UML_IDENTIFIER, ValueExistencePolicy.ALL);
        assertEquals("Various WGS", assertInstanceOf(Identifier.class, values.get("name")).getCode());
    }

    /**
     * Tests another view as a map where the object is specified as a class instead of as an instance.
     */
    @Test
    public void testNameMap() {
        final Map<String, String> names = MetadataStandard.ISO_19111.asNameMap(
                DefaultDatumEnsemble.class, KeyNamePolicy.UML_IDENTIFIER, KeyNamePolicy.SENTENCE);
        assertEquals("Name", names.get("name"));
    }
}
