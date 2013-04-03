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
package org.apache.sis.internal.metadata;

import java.util.Date;
import org.junit.Test;
import org.apache.sis.test.TestCase;

import static org.junit.Assert.*;
import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.FALSE;


/**
 * Tests the {@link MetadataUtilities} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class MetadataUtilitiesTest extends TestCase {
    /**
     * Tests {@link MetadataUtilities#toMilliseconds(Date)}.
     */
    @Test
    public void testToMilliseconds() {
        assertEquals(1000,           MetadataUtilities.toMilliseconds(new Date(1000)));
        assertEquals(Long.MIN_VALUE, MetadataUtilities.toMilliseconds(null));
    }

    /**
     * Tests {@link MetadataUtilities#toDate(long)}.
     */
    @Test
    public void testToDate() {
        assertEquals(new Date(1000), MetadataUtilities.toDate(1000));
        assertNull(MetadataUtilities.toDate(Long.MIN_VALUE));
    }

    /**
     * Tests {@link MetadataUtilities#setBoolean(int, byte, Boolean)}.
     * This will indirectly test the getter method through Java assertion.
     */
    @Test
    public void testSetBoolean() {
        final int mask0 =  3; // 0b000011;
        final int mask1 = 12; // 0b001100;
        final int mask2 = 48; // 0b110000;
        int flags = 0;
        flags = MetadataUtilities.setBoolean(flags, mask1, null ); assertEquals( 0 /*0b000000*/, flags);
        flags = MetadataUtilities.setBoolean(flags, mask1, TRUE ); assertEquals(12 /*0b001100*/, flags);
        flags = MetadataUtilities.setBoolean(flags, mask2, FALSE); assertEquals(44 /*0b101100*/, flags);
        flags = MetadataUtilities.setBoolean(flags, mask1, null ); assertEquals(32 /*0b100000*/, flags);
        flags = MetadataUtilities.setBoolean(flags, mask0, TRUE ); assertEquals(35 /*0b100011*/, flags);
        flags = MetadataUtilities.setBoolean(flags, mask0, FALSE); assertEquals(34 /*0b100010*/, flags);
    }
}
