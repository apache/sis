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

import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.util.FactoryException;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.IdentifiedObject;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.test.mock.IdentifiedObjectMock;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.referencing.IdentifiedObjects.*;


/**
 * Tests the {@link IdentifiedObjects} static methods.
 *
 * <p><b>Note:</b> this test class intentionally declares {@link #testLookupEPSG()} and {@link #testLookupWMS()}
 * methods without {@link Test} annotation because those tests should be executed only after the EPSG tests in
 * {@link org.apache.sis.test.suite.ReferencingTestSuite}. Those tests will be executed by {@link CRSTest} instead.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
public final strictfp class IdentifiedObjectsTest extends TestCase {
    /**
     * Tests {@link IdentifiedObjects#isHeuristicMatchForName(IdentifiedObject, String)}.
     */
    @Test
    public void testIsHeuristicMatchForName() {
        final NameFactory factory = DefaultFactories.forBuildin(NameFactory.class);
        final GenericName name = factory.createGenericName(null, "myScope", "myName");
        IdentifiedObjectMock object = new IdentifiedObjectMock("myCode ", name); // Intentional trailing space.

        // Test the code.
        assertFalse(isHeuristicMatchForName(object, "other"));
        assertTrue (isHeuristicMatchForName(object, "myCode"));
        assertTrue (isHeuristicMatchForName(object, " my_code "));
        assertFalse(isHeuristicMatchForName(object, "testmyCode"));
        assertFalse(isHeuristicMatchForName(object, "other:myCode"));
        assertFalse(isHeuristicMatchForName(object, "test"));
        assertFalse(isHeuristicMatchForName(null,   "myCode"));

        // Test the alias.
        assertTrue (isHeuristicMatchForName(object, "myName"));
        assertTrue (isHeuristicMatchForName(object, " My_name "));
        assertFalse(isHeuristicMatchForName(object, "myScope"));
        assertFalse(isHeuristicMatchForName(object, "other:myName"));
        assertFalse(isHeuristicMatchForName(object, "myScope:other"));
        assertFalse(isHeuristicMatchForName(object, "other:myScope:myName"));

        // Test non-letter and non-digits characters.
        object = new IdentifiedObjectMock("Mercator (1SP)", name);
        assertTrue (isHeuristicMatchForName(object, "Mercator (1SP)"));
        assertTrue (isHeuristicMatchForName(object, "Mercator_1SP"));
        assertFalse(isHeuristicMatchForName(object, "Mercator_2SP"));

        // Test diacritical signs
        object = new IdentifiedObjectMock("Réunion", name);
        assertTrue (isHeuristicMatchForName(object, "Réunion"));
        assertTrue (isHeuristicMatchForName(object, "Reunion"));
    }

    /**
     * Tests {@link IdentifiedObjects#lookupEPSG(IdentifiedObject)} and
     * {@link IdentifiedObjects#lookupURN(IdentifiedObject, Citation)}
     * with a CRS which is expected to be found in the EPSG dataset.
     *
     * <p>This method is intentionally not annotated with {@link Test}.
     * See class javadoc for explanation.</p>
     *
     * @throws FactoryException if an error occurred during the lookup.
     *
     * @see CRSTest#testIdentifiedObjectLookup()
     */
    public static void testLookupEPSG() throws FactoryException {
        assertEquals("EPSG:4326", Integer.valueOf(4326), IdentifiedObjects.lookupEPSG(HardCodedCRS.WGS84_φλ));
        String urn = IdentifiedObjects.lookupURN(HardCodedCRS.WGS84_φλ, null);
        assertTrue(urn, urn.matches("urn\\:ogc\\:def\\:crs\\:EPSG\\:.*\\:4326"));
    }

    /**
     * Tests {@link IdentifiedObjects#lookupEPSG(IdentifiedObject)} and
     * {@link IdentifiedObjects#lookupURN(IdentifiedObject, Citation)}.
     * with a CRS which is expected to be found in the WMS definitions.
     *
     * <p>This method is intentionally not annotated with {@link Test}.
     * See class javadoc for explanation.</p>
     *
     * @throws FactoryException if an error occurred during the lookup.
     *
     * @see CRSTest#testIdentifiedObjectLookup()
     */
    public static void testLookupWMS() throws FactoryException {
        assertNull("CRS:84", IdentifiedObjects.lookupEPSG(HardCodedCRS.WGS84));
        String urn = IdentifiedObjects.lookupURN(HardCodedCRS.WGS84, null);
        assertTrue(urn, urn.matches("urn\\:ogc\\:def\\:crs\\:OGC\\:.*\\:CRS84"));
    }
}
