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
package org.apache.sis.referencing.gazetteer;

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertInstanceOf;


/**
 * Tests {@link GazetteerFactory}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 */
public final class GazetteerFactoryTest extends TestCase {
    /**
     * Tests {@link ReferencingByIdentifiers#getInstance(String)}.
     *
     * @throws GazetteerException if a reference system cannot be created.
     */
    @Test
    public void testGetInstance() throws GazetteerException {
        final GazetteerFactory factory = new GazetteerFactory();
        final ReferencingByIdentifiers r1 = factory.forName("MGRS");
        final ReferencingByIdentifiers r2 = factory.forName("Geohash");
        assertInstanceOf("MGRS", MilitaryGridReferenceSystem.class, r1);
        assertInstanceOf("Geohash", GeohashReferenceSystem.class, r2);
        assertSame(r1, factory.forName("MGRS"));
        assertSame(r2, factory.forName("Geohash"));
        try {
            factory.forName("Dummy");
            fail("Should have thrown an exception");
        } catch (GazetteerException e) {
            assertTrue(e.getMessage().contains("Dummy"));
        }
    }
}
