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

import static org.apache.sis.metadata.internal.shared.ImplementationHelper.ISO_NAMESPACE;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link Namespaces} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class NamespacesTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public NamespacesTest() {
    }

    /**
     * Tests {@link Namespaces#getPreferredPrefix(String, String)}.
     */
    @Test
    public void testGetPreferredPrefix() {
        assertEquals("gml",  Namespaces.getPreferredPrefix("http://www.opengis.net/gml/3.2", null));
        assertEquals("mdb",  Namespaces.getPreferredPrefix(ISO_NAMESPACE + "19115/-3/mdb/1.0", null));
        assertEquals("gmi",  Namespaces.getPreferredPrefix(ISO_NAMESPACE + "19115/-2/gmi/1.0", null));
        assertEquals("srv",  Namespaces.getPreferredPrefix(ISO_NAMESPACE + "19115/-3/srv/2.0", null));
        assertEquals("srv1", Namespaces.getPreferredPrefix("http://www.isotc211.org/2005/srv", null));
        assertEquals("gmd",  Namespaces.getPreferredPrefix("http://www.isotc211.org/2005/gmd", null));
    }

    /**
     * Tests {@link Namespaces#guessForType(String)}. This method uses {@code assertSame(…)} instead of
     * {@code assertEquals(…)} for verifying that {@link TransformingReader} invoked {@link String#intern()}.
     */
    @Test
    public void testGuessForType() {
        String type;
        assertSame(Namespaces.CIT, Namespaces.guessForType(type = "CI_Citation"),           type);
        assertSame(Namespaces.GEX, Namespaces.guessForType(type = "EX_Extent"),             type);
        assertSame(Namespaces.MDB, Namespaces.guessForType(type = "MD_Metadata"),           type);
        assertSame(Namespaces.MRI, Namespaces.guessForType(type = "MD_DataIdentification"), type);
        assertSame(Namespaces.MRI, Namespaces.guessForType(type = "DS_InitiativeTypeCode"), type);
        assertSame(Namespaces.SRV, Namespaces.guessForType(type = "DCPList"),               type);
    }
}
