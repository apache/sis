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
import static org.apache.sis.internal.referencing.provider.TransverseMercator.Zoner.UTM;


/**
 * Tests {@link MGRSEncoder}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public final strictfp class MGRSEncoderTest extends TestCase {
    /**
     * Tests {@link MGRSEncoder#latitudeBand(double)}.
     */
    @Test
    public void testLatitudeBand() {
        assertEquals("80°S", 'C', MGRSEncoder.latitudeBand(-80));
        assertEquals("55°N", 'U', MGRSEncoder.latitudeBand( 55));
        assertEquals("56°N", 'V', MGRSEncoder.latitudeBand( 56));
        assertEquals("63°N", 'V', MGRSEncoder.latitudeBand( 63));
        assertEquals("64°N", 'W', MGRSEncoder.latitudeBand( 64));
        assertEquals("71°N", 'W', MGRSEncoder.latitudeBand( 71));
        assertEquals("72°N", 'X', MGRSEncoder.latitudeBand( 72));
        assertEquals("84°N", 'X', MGRSEncoder.latitudeBand( 84));
    }

    /**
     * Tests {@link MGRSEncoder#zone(double, char)}.
     */
    @Test
    public void testZone() {
        assertEquals( "4°E band T", 31, MGRSEncoder.zone(UTM.zone( 4), 4, 'T'));
        assertEquals( "4°E band V", 32, MGRSEncoder.zone(UTM.zone( 4),  4, 'V'));
        assertEquals("20°E band W", 34, MGRSEncoder.zone(UTM.zone(20), 20, 'W'));
        assertEquals("20°E band X", 33, MGRSEncoder.zone(UTM.zone(20), 20, 'X'));
    }
}
