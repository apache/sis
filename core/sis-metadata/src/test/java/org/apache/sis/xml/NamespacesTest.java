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
package org.apache.sis.xml;

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link Namespaces} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.8
 * @module
 */
public final strictfp class NamespacesTest extends TestCase {
    /**
     * Tests {@link Namespaces#getPreferredPrefix(String, String)}.
     */
    @Test
    public void testGetPreferredPrefix() {
        assertEquals("gml",  Namespaces.getPreferredPrefix("http://www.opengis.net/gml/3.2", null));
        assertEquals("mdb",  Namespaces.getPreferredPrefix("http://standards.iso.org/iso/19115/-3/mdb/1.0", null));
        assertEquals("gmi",  Namespaces.getPreferredPrefix("http://standards.iso.org/iso/19115/-2/gmi/1.0", null));
        assertEquals("srv",  Namespaces.getPreferredPrefix("http://standards.iso.org/iso/19115/-3/srv/2.0", null));
        assertEquals("srv1", Namespaces.getPreferredPrefix("http://www.isotc211.org/2005/srv", null));
        assertEquals("gmd",  Namespaces.getPreferredPrefix("http://www.isotc211.org/2005/gmd", null));
    }

    /**
     * Tests {@link Namespaces#guessForType(String)}. This method uses {@code assertSame(…)} instead of
     * {@code assertEquals(…)} for verifying that {@link TransformingReader} invoked {@link String#intern()}.
     */
    @Test
    public void testGuessForType() {
        assertSame("CI_Citation",           Namespaces.CIT, Namespaces.guessForType("CI_Citation"));
        assertSame("EX_Extent",             Namespaces.GEX, Namespaces.guessForType("EX_Extent"));
        assertSame("MD_Metadata",           Namespaces.MDB, Namespaces.guessForType("MD_Metadata"));
        assertSame("MD_DataIdentification", Namespaces.MRI, Namespaces.guessForType("MD_DataIdentification"));
        assertSame("DS_InitiativeTypeCode", Namespaces.MRI, Namespaces.guessForType("DS_InitiativeTypeCode"));
        assertSame("DCPList",               Namespaces.SRV, Namespaces.guessForType("DCPList"));
    }
}
