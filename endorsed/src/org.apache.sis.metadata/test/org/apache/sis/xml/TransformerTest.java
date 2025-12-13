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
import org.apache.sis.xml.internal.shared.LegacyNamespaces;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link Transformer}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class TransformerTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public TransformerTest() {
    }

    /**
     * Tests {@link Transformer#load(boolean, String, Set, int)}.
     */
    @Test
    public void testLoad() {
        final Set<String> targets = new HashSet<>(12);
        final Map<String, Map<String,String>> namespaces = Transformer.load(false, TransformingReader.FILENAME, targets, 260);
        assertTrue(targets.contains(LegacyNamespaces.GMI_ALIAS), LegacyNamespaces.GMI_ALIAS);
        assertTrue(targets.contains(LegacyNamespaces.GMI),       LegacyNamespaces.GMI      );
        assertTrue(targets.contains(LegacyNamespaces.GMD),       LegacyNamespaces.GMD      );
        assertTrue(targets.contains(LegacyNamespaces.SRV),       LegacyNamespaces.SRV      );
        assertTrue(targets.contains(LegacyNamespaces.GCO),       LegacyNamespaces.GCO      );
        assertTrue(targets.contains(LegacyNamespaces.GMX),       LegacyNamespaces.GMX      );
        assertTrue(targets.contains(LegacyNamespaces.GML),       LegacyNamespaces.GML      );

        Map<String, String> m = namespaces.get("CI_Citation");
        assertNotNull(m, "CI_Citation");
        assertEquals(Namespaces.CIT, m.get("title"), "title");
        assertEquals(Namespaces.CIT, m.get("edition"), "edition");

        m = namespaces.get("MD_Metadata");
        assertNotNull(m, "MD_Metadata");
        assertEquals(Namespaces.MDB, m.get("identificationInfo"), "identificationInfo");
        assertEquals(Namespaces.MDB, m.get("spatialRepresentationInfo"), "spatialRepresentationInfo");
    }
}
