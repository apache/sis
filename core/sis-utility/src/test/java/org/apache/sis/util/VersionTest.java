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
package org.apache.sis.util;

import org.apache.sis.internal.jaxb.LegacyNamespaces;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link Version} class, especially the {@code compareTo} method.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.6
 * @module
 */
public final strictfp class VersionTest extends TestCase {
    /**
     * Tests a numeric-only version.
     */
    @Test
    public void testNumeric() {
        final Version version = new Version("6.11.2");
        assertEquals("6.11.2", version.toString());
        assertEquals( 6, version.getMajor());
        assertEquals(11, version.getMinor());
        assertEquals( 2, version.getRevision());
        assertSame(version.getRevision(), version.getComponent(2));
        assertNull(version.getComponent(3));

        assertTrue(version.compareTo(new Version("6.11.2")) == 0);
        assertTrue(version.compareTo(new Version("6.8"   )) >  0);
        assertTrue(version.compareTo(new Version("6.12.0")) <  0);
        assertTrue(version.compareTo(new Version("6.11"  )) >  0);
    }

    /**
     * Tests a alpha-numeric version.
     */
    @Test
    @DependsOnMethod("testNumeric")
    public void testAlphaNumeric() {
        final Version version = new Version("1.6.b2");
        assertEquals("1.6.b2", version.toString());
        assertEquals( 1, version.getMajor());
        assertEquals( 6, version.getMinor());
        assertEquals("b2", version.getRevision());
        assertSame(version.getRevision(), version.getComponent(2));
        assertNull(version.getComponent(3));

        assertTrue(version.compareTo(new Version("1.6.b2")) == 0);
        assertTrue(version.compareTo(new Version("1.6.b1"))  > 0);
        assertTrue(version.compareTo(new Version("1.07.b1")) < 0);
    }

    /**
     * Tests {@link Version#compareTo(Version, int)} with version numbers needed by our GML support.
     */
    @Test
    public void testGML() {
        assertTrue(LegacyNamespaces.VERSION_3_2.compareTo(LegacyNamespaces.VERSION_3_2_1, 2) == 0);
        assertTrue(LegacyNamespaces.VERSION_3_2.compareTo(LegacyNamespaces.VERSION_3_2_1   )  < 0);
        assertTrue(LegacyNamespaces.VERSION_3_0.compareTo(LegacyNamespaces.VERSION_3_2_1   )  < 0);
        assertTrue(LegacyNamespaces.VERSION_3_0.compareTo(LegacyNamespaces.VERSION_3_2_1, 2)  < 0);
        assertTrue(LegacyNamespaces.VERSION_3_0.compareTo(LegacyNamespaces.VERSION_3_2_1, 1) == 0);
        assertTrue(LegacyNamespaces.VERSION_3_0.compareTo(LegacyNamespaces.VERSION_3_2     )  < 0);
        assertTrue(LegacyNamespaces.VERSION_3_0.compareTo(LegacyNamespaces.VERSION_3_2,   2)  < 0);
        assertTrue(LegacyNamespaces.VERSION_3_0.compareTo(LegacyNamespaces.VERSION_3_2,   1) == 0);
    }

    /**
     * Tests the {@link Version#valueOf(int[])} method.
     */
    @Test
    public void testValueOf() {
        Version version = Version.valueOf(1);
        assertEquals("1", version.toString());
        assertEquals( 1,  version.getMajor());
        assertNull  (     version.getMinor());
        assertNull  (     version.getRevision());

        version = Version.valueOf(10);
        assertEquals("10", version.toString());
        assertEquals( 10,  version.getMajor());
        assertNull  (      version.getMinor());
        assertNull  (      version.getRevision());

        version = Version.valueOf(0, 4);
        assertEquals("0.4", version.toString());
        assertEquals( 0,    version.getMajor());
        assertEquals(   4,  version.getMinor());
        assertNull  (       version.getRevision());

        version = Version.valueOf(6, 11, 2);
        assertEquals("6.11.2", version.toString());
        assertEquals( 6,       version.getMajor());
        assertEquals(   11,    version.getMinor());
        assertEquals(      2,  version.getRevision());
    }

    /**
     * Tests the cached values of {@link Version#valueOf(int[])}.
     */
    @Test
    @DependsOnMethod("testValueOf")
    public void testCachedValueOf() {
        for (int major=1; major<=2; major++) {
            final Version version = Version.valueOf(major);
            assertSame(version.toString(), version, Version.valueOf(major));
        }
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        final Version version = new Version("1.6.b2");
        assertNotSame(version, assertSerializedEquals(version));
    }
}
