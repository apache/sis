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

import java.util.List;
import java.util.Arrays;
import org.opengis.util.GenericName;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.test.Validators.validate;
import static org.apache.sis.test.Assertions.assertSerializedEquals;


/**
 * Tests the {@link DefaultScopedName} implementations.
 * Those tests instantiate the objects directly, without using {@link DefaultNameFactory}.
 * For tests using the name factory, see {@link DefaultNameFactoryTest}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.3
 */
@DependsOn(DefaultLocalNameTest.class)
public final class DefaultScopedNameTest extends TestCase {
    /**
     * Tests the creation of a scoped name in a new namespace.
     * The fully qualified name is {@code "urn:ogc:def:crs:epsg:4326"}.
     */
    @Test
    public void testUrnNamespace() {
        final String[] parsed = new String[] {
            "urn","ogc","def","crs","epsg","4326"
        };
        GenericName name = new DefaultScopedName(null, Arrays.asList(parsed));
        assertSame(name, name.toFullyQualifiedName());
        assertEquals("urn:ogc:def:crs:epsg:4326", name.toString());
        assertNotSame(name, assertSerializedEquals(name));
        validate(name);                     // GeoAPI tests.
        for (int i=parsed.length; --i>=0;) {
            name = name.tip();
            validate(name);
            assertEquals(parsed[i], name.toString());
            name = name.scope().name();
        }
    }

    /**
     * Tests the creation of scoped names where different parts of the name are {@link SimpleInternationalString}
     * instances. The implementation should be able to detect that the names and their hash codes are equal.
     *
     * @see DefaultNameFactoryTest#testSimpleInternationalString()
     */
    @Test
    public void testSimpleInternationalString() {
        GenericName n1 = new DefaultScopedName(null, List.of("ns1", "Route"));
        GenericName n2 = new DefaultScopedName(null, List.of(new SimpleInternationalString("ns1"), "Route"));
        GenericName n3 = new DefaultScopedName(null, List.of("ns1", new SimpleInternationalString("Route")));
        assertNameEqual(n1, n2);
        assertNameEqual(n1, n3);
        assertNameEqual(n2, n3);
    }

    /**
     * Verifies that the following names are equal and have the same hash code.
     */
    private static void assertNameEqual(final GenericName n1, final GenericName n2) {
        assertEquals("equals(Object)", n1, n2);
        assertEquals("equals(Object)", n2, n1);
        assertEquals("hashCode()", n1.hashCode(), n2.hashCode());
    }
}
