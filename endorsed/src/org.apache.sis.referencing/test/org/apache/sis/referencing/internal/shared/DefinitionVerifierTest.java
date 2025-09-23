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

import java.util.Map;
import java.util.HashMap;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import org.opengis.util.FactoryException;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.metadata.iso.citation.HardCodedCitations;


/**
 * Tests {@link DefinitionVerifier}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DefinitionVerifierTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefinitionVerifierTest() {
    }

    /**
     * Tests with a CRS which is conform to the authoritative definition.
     *
     * @throws FactoryException if an error occurred while querying the authority factory.
     */
    @Test
    public void testConformCRS() throws FactoryException {
        final DefaultGeographicCRS crs = HardCodedCRS.WGS84_LATITUDE_FIRST;
        final DefinitionVerifier ver = DefinitionVerifier.withAuthority(crs, null, false, null);
        assertNotNull(ver, "Should replace by EPSG:4326");
        assertNotSame(crs, ver.recommendation, "Should replace by EPSG:4326");
        assertSame(CommonCRS.WGS84.geographic(), ver.recommendation, "Should replace by EPSG:4326");
        assertNull(ver.warning(true), "Should be silent.");
    }

    /**
     * Tests with a CRS which is conform to the authoritative definition.
     *
     * @throws FactoryException if an error occurred while querying the authority factory.
     */
    @Test
    public void testNormalizedCRS() throws FactoryException {
        final DefaultGeographicCRS crs = HardCodedCRS.WGS84;
        assertNull(DefinitionVerifier.withAuthority(crs, null, false, null), "No replacement without EPSG code.");
        final DefinitionVerifier ver = DefinitionVerifier.withAuthority(crs, null, true, null);
        assertNotNull(ver, "Should replace by normalized CRS");
        assertNotSame(crs, ver.recommendation, "Should replace by normalized CRS");
        assertSame(CommonCRS.WGS84.normalizedGeographic(), ver.recommendation, "Should replace by normalized CRS");
        assertNull(ver.warning(true), "Should be silent.");
    }

    /**
     * Tests with a CRS having wrong axis order.
     *
     * @throws FactoryException if an error occurred while querying the authority factory.
     */
    @Test
    public void testDifferentAxisOrder() throws FactoryException {
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(DefaultGeographicCRS.NAME_KEY, "WGS 84");
        properties.put(DefaultGeographicCRS.IDENTIFIERS_KEY, new NamedIdentifier(HardCodedCitations.EPSG, "4326"));
        DefaultGeographicCRS crs = HardCodedCRS.WGS84;
        crs = new DefaultGeographicCRS(properties, crs.getDatum(), crs.getDatumEnsemble(), crs.getCoordinateSystem());

        final DefinitionVerifier ver = DefinitionVerifier.withAuthority(crs, null, false, null);
        assertNotNull(ver, "Should replace by normalized CRS");
        assertNotSame(crs, ver.recommendation, "Should replace by normalized CRS");
        assertSame(CommonCRS.WGS84.normalizedGeographic(), ver.recommendation, "Should replace by normalized CRS");

        final LogRecord warning = ver.warning(true);
        assertNotNull(warning, "Should emit a warning.");
        final String message = new SimpleFormatter().formatMessage(warning);
        assertTrue(message.contains("WGS 84"), message);
        assertTrue(message.contains("EPSG:4326"), message);
    }
}
