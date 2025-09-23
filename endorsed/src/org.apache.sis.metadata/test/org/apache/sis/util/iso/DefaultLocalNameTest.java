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
package org.apache.sis.util.iso;

import static org.apache.sis.util.internal.shared.Constants.EPSG;
import static org.apache.sis.util.iso.DefaultNameSpace.DEFAULT_SEPARATOR_STRING;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.opengis.test.Validators.validate;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSerializedEquals;


/**
 * Tests the {@link DefaultLocalName} implementation.
 * Those tests instantiate the objects directly, without using {@link DefaultNameFactory}.
 * For tests using the name factory, see {@link DefaultNameFactoryTest}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DefaultLocalNameTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultLocalNameTest() {
    }

    /**
     * Tests the creation of a local name in the global namespace.
     * The fully qualified name is {@code "EPSG"}.
     */
    @Test
    public void testGlobalNamespace() {
        final DefaultLocalName name = new DefaultLocalName(null, EPSG);
        assertSame(EPSG, name.toString());
        assertSame(EPSG, name.toInternationalString().toString());
        assertSame(GlobalNameSpace.GLOBAL, name.scope());
        assertNotSame(name, assertSerializedEquals(name));
        validate(name); // GeoAPI tests.
    }

    /**
     * Tests the creation of a local name in a new namespace.
     * The fully qualified name is {@code "EPSG:4326"}.
     * The tail and the tip are both local names.
     */
    @Test
    public void testEpsgNamespace() {
        final DefaultNameSpace ns = DefaultNameSpace.forName(new DefaultLocalName(null, EPSG),
                DEFAULT_SEPARATOR_STRING, DEFAULT_SEPARATOR_STRING);
        assertEquals(EPSG, ns.name().toString());
        validate(ns);       // GeoAPI tests.

        final String WGS84 = "4326";
        final DefaultLocalName name = new DefaultLocalName(ns, WGS84);
        assertSame(ns, name.scope());
        assertSame(WGS84, name.toString());
        assertEquals(EPSG + ':' + WGS84, name.toFullyQualifiedName().toString());
        assertNotSame(name, assertSerializedEquals(name));
        validate(name);     // GeoAPI tests.
    }
}
