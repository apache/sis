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
package org.apache.sis.measure;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.text.AttributedCharacterIterator;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOnMethod;
import org.junit.Test;

import static java.text.NumberFormat.Field.INTEGER;
import static java.text.AttributedCharacterIterator.DONE;
import static java.text.AttributedCharacterIterator.Attribute;
import static org.apache.sis.measure.AngleFormat.Field.*;
import static org.junit.Assert.*;


/**
 * Tests the {@link FormattedCharacterIterator} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class FormattedCharacterIteratorTest extends TestCase {
    /**
     * The string used for testing purpose in this class.
     */
    static final String LATITUDE_STRING = "45°30′15.0″N";

    /**
     * The numerical value corresponding to {@link #ANGLE_STRING}.
     * This information is used by {@link AngleFormatTest}.
     */
    static final double LATITUDE_VALUE = 45.50416666666667;

    /**
     * Tests an iteration without attribute.
     */
    @Test
    public void testWithoutAttribute() {
        final String text = LATITUDE_STRING;
        final FormattedCharacterIterator it = new FormattedCharacterIterator(text);
        assertTrue(it.getAllAttributeKeys().isEmpty());
        assertEquals(text, it.toString());

        int i = 0;
        for (char c=it.first(); c!=DONE; c=it.next()) {
            assertEquals(text.charAt(i), c);
            assertEquals(it  .charAt(i), c);
            assertEquals("getIndex",    i++,           it.getIndex());
            assertEquals("getRunStart", 0,             it.getRunStart());
            assertEquals("getRunLimit", text.length(), it.getRunLimit());
            assertTrue(it.getAttributes().isEmpty());
        }
        assertEquals(text.length(), i);
        for (char c=it.last(); c!=DONE; c=it.previous()) {
            assertEquals(text.charAt(--i), c);
            assertEquals(it  .charAt(  i), c);
            assertEquals("getIndex",    i,             it.getIndex());
            assertEquals("getRunStart", 0,             it.getRunStart());
            assertEquals("getRunLimit", text.length(), it.getRunLimit());
            assertTrue(it.getAttributes().isEmpty());
        }
        assertEquals(0, i);
    }

    /**
     * Tests an iteration with {@code DEGREES}/{@code MINUTES}/{@code SECONDS} attributes.
     */
    @Test
    public void testNonOverlappingAttributes() {
        testAttributes(false);
    }

    /**
     * Tests an iteration with {@code INTEGER} attributes in addition to the
     * {@code DEGREES}/{@code MINUTES}/{@code SECONDS} ones. The {@code INTEGER}
     * attributes are defined on sub-range of the degrees/minutes/seconds ones.
     */
    @Test
    @DependsOnMethod("testNonOverlappingAttributes")
    public void testOverlappingAttributes() {
        testAttributes(true);
    }

    /**
     * Returns all expected attribute keys for the tests in this class.
     *
     * @param overlapping {@code true} for including the keys for overlapping attributes.
     */
    private static Set<Attribute> getAllAttributeKeys(final boolean overlapping) {
        final Set<Attribute> keys = new HashSet<>(8);
        assertTrue(keys.add(DEGREES));
        assertTrue(keys.add(MINUTES));
        assertTrue(keys.add(SECONDS));
        assertTrue(keys.add(HEMISPHERE));
        if (overlapping) {
            assertTrue(keys.add(INTEGER));
        }
        return keys;
    }

    /**
     * Tests an iteration with attributes, optionally having {@code INTEGER} attributes
     * overlapping the {@code DEGREES}/{@code MINUTES}/{@code SECONDS} ones.
     */
    private static void testAttributes(final boolean overlapping) {
        final FormattedCharacterIterator it = new FormattedCharacterIterator(LATITUDE_STRING);
        it.addField(DEGREES,     45,  0,  3);
        it.addField(MINUTES,     30,  3,  6);
        it.addField(SECONDS,     15,  6, 11);
        it.addField(HEMISPHERE, 'N', 11, 12);
        if (overlapping) {
            it.addField(INTEGER, 45,  0,  2);
            it.addField(INTEGER, 30,  3,  5);
            it.addField(INTEGER, 15,  6,  8);
        }
        testAttributes(it, overlapping);
    }

    /**
     * Tests an iteration with attributes, optionally having {@code INTEGER} attributes
     * overlapping the {@code DEGREES}/{@code MINUTES}/{@code SECONDS} ones. The given
     * iterator shall iterates over the {@code "45°30′15.0″N"} characters.
     *
     * <p>This test is leveraged by {@link AngleFormatTest#testFormatToCharacterIterator()}.</p>
     */
    @SuppressWarnings("fallthrough")
    static void testAttributes(final AttributedCharacterIterator it, final boolean overlapping) {
        assertEquals(getAllAttributeKeys(overlapping), it.getAllAttributeKeys());
        for (char c=it.first(); c!=DONE; c=it.next()) {
            final AngleFormat.Field key;
            final Comparable<?> value;
            final int start, limit;
            int oi = 0; // How many characters to remove for having the limit of the integer field.
            switch (it.getIndex()) {
                case  0: assertEquals('4', c); // Beginning of 45°
                case  1: oi = overlapping ? 1 : 0;
                case  2: key=DEGREES;    value= 45; start=0; limit= 3; break;
                case  3: assertEquals('3', c); // Beginning of 30′
                case  4: oi = overlapping ? 1 : 0;
                case  5: key=MINUTES;    value= 30; start=3; limit= 6; break;
                case  6: assertEquals('1', c); // Beginning of 15.0″
                case  7: oi = overlapping ? 3 : 0;
                case  8:
                case  9:
                case 10: key=SECONDS;    value= 15; start= 6; limit=11; break;
                case 11: key=HEMISPHERE; value='N'; start=11; limit=12; break;
                default: throw new AssertionError();
            }
            final Map<Attribute,Object> attributes = it.getAttributes();
            assertEquals("attributes.size", (oi!=0) ? 2     : 1,    attributes.size());
            assertEquals("attributes.get",            value,        attributes.get(key));
            assertEquals("attributes.get",  (oi!=0) ? value : null, attributes.get(INTEGER));

            assertEquals("getRunStart",           start,         it.getRunStart());
            assertEquals("getRunLimit",           limit-oi,      it.getRunLimit());
            assertEquals("getRunStart",           start,         it.getRunStart(key));
            assertEquals("getRunLimit",           limit,         it.getRunLimit(key));
            assertEquals("getRunStart", (oi!=0) ? start    :  0, it.getRunStart(INTEGER));
            assertEquals("getRunLimit", (oi!=0) ? limit-oi : 12, it.getRunLimit(INTEGER));
        }
    }
}
