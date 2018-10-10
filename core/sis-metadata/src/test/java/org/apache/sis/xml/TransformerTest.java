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

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import org.apache.sis.internal.xml.LegacyNamespaces;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link Transformer}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class TransformerTest extends TestCase {
    /**
     * Tests {@link Transformer#load(boolean, String, Set, int)}.
     */
    @Test
    public void testLoad() {
        final Set<String> targets = new HashSet<>(12);
        final Map<String, Map<String,String>> namespaces = Transformer.load(false, TransformingReader.FILENAME, targets, 260);
        assertTrue(LegacyNamespaces.GMI_ALIAS, targets.contains(LegacyNamespaces.GMI_ALIAS));
        assertTrue(LegacyNamespaces.GMI, targets.contains(LegacyNamespaces.GMI));
        assertTrue(LegacyNamespaces.GMD, targets.contains(LegacyNamespaces.GMD));
        assertTrue(LegacyNamespaces.SRV, targets.contains(LegacyNamespaces.SRV));
        assertTrue(LegacyNamespaces.GCO, targets.contains(LegacyNamespaces.GCO));
        assertTrue(LegacyNamespaces.GMX, targets.contains(LegacyNamespaces.GMX));
        assertTrue(LegacyNamespaces.GML, targets.contains(LegacyNamespaces.GML));

        Map<String, String> m = namespaces.get("CI_Citation");
        assertNotNull("CI_Citation", m);
        assertEquals("title",   Namespaces.CIT, m.get("title"));
        assertEquals("edition", Namespaces.CIT, m.get("edition"));

        m = namespaces.get("MD_Metadata");
        assertNotNull("MD_Metadata", m);
        assertEquals("identificationInfo",        Namespaces.MDB, m.get("identificationInfo"));
        assertEquals("spatialRepresentationInfo", Namespaces.MDB, m.get("spatialRepresentationInfo"));
    }
}
