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

import java.util.Map;
import java.util.HashMap;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import org.opengis.util.FactoryException;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.metadata.iso.citation.HardCodedCitations;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link DefinitionVerifier}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.8
 * @module
 */
@DependsOn(org.apache.sis.referencing.CRSTest.class)
public final strictfp class DefinitionVerifierTest extends TestCase {
    /**
     * Tests with a CRS which is conform to the authoritative definition.
     *
     * @throws FactoryException if an error occurred while querying the authority factory.
     */
    @Test
    public void testConformCRS() throws FactoryException {
        final DefaultGeographicCRS crs = HardCodedCRS.WGS84_LATITUDE_FIRST;
        final DefinitionVerifier ver = DefinitionVerifier.withAuthority(crs, null, false, null);
        assertNotNull("Should replace by EPSG:4326", ver);
        assertNotSame("Should replace by EPSG:4326", crs, ver.recommendation);
        assertSame   ("Should replace by EPSG:4326", CommonCRS.WGS84.geographic(), ver.recommendation);
        assertNull   ("Should be silent.", ver.warning(true));
    }

    /**
     * Tests with a CRS which is conform to the authoritative definition.
     *
     * @throws FactoryException if an error occurred while querying the authority factory.
     */
    @Test
    @DependsOnMethod("testConformCRS")
    public void testNormalizedCRS() throws FactoryException {
        final DefaultGeographicCRS crs = HardCodedCRS.WGS84;
        assertNull("No replacement without EPSG code.", DefinitionVerifier.withAuthority(crs, null, false, null));
        final DefinitionVerifier ver = DefinitionVerifier.withAuthority(crs, null, true, null);
        assertNotNull("Should replace by normalized CRS", ver);
        assertNotSame("Should replace by normalized CRS", crs, ver.recommendation);
        assertSame   ("Should replace by normalized CRS", CommonCRS.WGS84.normalizedGeographic(), ver.recommendation);
        assertNull   ("Should be silent.", ver.warning(true));
    }

    /**
     * Tests with a CRS having wrong axis order.
     *
     * @throws FactoryException if an error occurred while querying the authority factory.
     */
    @Test
    @DependsOnMethod("testNormalizedCRS")
    public void testDifferentAxisOrder() throws FactoryException {
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(DefaultGeographicCRS.NAME_KEY, "WGS 84");
        properties.put(DefaultGeographicCRS.IDENTIFIERS_KEY, new NamedIdentifier(HardCodedCitations.EPSG, "4326"));
        DefaultGeographicCRS crs = HardCodedCRS.WGS84;
        crs = new DefaultGeographicCRS(properties, crs.getDatum(), crs.getCoordinateSystem());

        final DefinitionVerifier ver = DefinitionVerifier.withAuthority(crs, null, false, null);
        assertNotNull("Should replace by normalized CRS", ver);
        assertNotSame("Should replace by normalized CRS", crs, ver.recommendation);
        assertSame   ("Should replace by normalized CRS", CommonCRS.WGS84.normalizedGeographic(), ver.recommendation);

        final LogRecord warning = ver.warning(true);
        assertNotNull("Should emit a warning.", warning);
        final String message = new SimpleFormatter().formatMessage(warning);
        assertTrue(message, message.contains("WGS 84"));
        assertTrue(message, message.contains("EPSG:4326"));
    }
}
