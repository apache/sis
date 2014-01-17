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
package org.apache.sis.referencing.crs;

import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.util.Collections.singletonMap;
import static org.opengis.referencing.cs.CoordinateSystem.NAME_KEY;
import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link DefaultCompoundCRS}
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@DependsOn({SubTypesTest.class, AbstractCRSTest.class})
public final strictfp class DefaultCompoundCRSTest extends TestCase {
    /**
     * Tests construction and serialization of a {@link DefaultCompoundCRS}.
     */
    @Test
    public void testConstructionAndSerialization() {
        final DefaultCompoundCRS crs3 = new DefaultCompoundCRS(singletonMap(NAME_KEY, "3D"),
                HardCodedCRS.WGS84, HardCodedCRS.ELLIPSOIDAL_HEIGHT); // Not legal, but we just want to test.
        final DefaultCompoundCRS crs4 = new DefaultCompoundCRS(singletonMap(NAME_KEY, "4D"),
                crs3, HardCodedCRS.TIME);

        verify(crs3, crs4);
        verify(crs3, assertSerializedEquals(crs4));
    }

    /**
     * Verifies the components of the CRS created by {@link #testConstructionAndSerialization()}.
     */
    private static void verify(final DefaultCompoundCRS crs3, final DefaultCompoundCRS crs4) {
        assertArrayEquals("getComponents()", new AbstractCRS[] {
                crs3, HardCodedCRS.TIME
        }, crs4.getComponents().toArray());

        assertArrayEquals("getSingleComponents()", new AbstractCRS[] {
                HardCodedCRS.WGS84, HardCodedCRS.ELLIPSOIDAL_HEIGHT, HardCodedCRS.TIME
        }, crs4.getSingleComponents().toArray());
    }
}
