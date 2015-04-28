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
import org.apache.sis.internal.simple.SimpleCharacterIterator;
import org.junit.Test;

import static java.lang.StrictMath.min;
import static java.lang.StrictMath.max;
import static java.text.NumberFormat.Field.INTEGER;
import static java.text.NumberFormat.Field.FRACTION;
import static java.text.NumberFormat.Field.DECIMAL_SEPARATOR;
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
        testAttributes(new LatitudeString().iterator(false), false);
    }

    /**
     * Tests an iteration with {@code INTEGER} attributes in addition to the
     * {@code DEGREES}/{@code MINUTES}/{@code SECONDS} ones. The {@code INTEGER}
     * attributes are defined on sub-range of the degrees/minutes/seconds ones.
     */
    @Test
    @DependsOnMethod("testNonOverlappingAttributes")
    public void testOverlappingAttributes() {
        testAttributes(new LatitudeString().iterator(true), true);
    }

    /**
     * Returns all expected attribute keys for the tests in this class.
     *
     * @param withNumberFields {@code true} for including the keys for {@code NumberFormat} fields.
     */
    private static Set<Attribute> getAllAttributeKeys(final boolean withNumberFields) {
        final Set<Attribute> keys = new HashSet<Attribute>(8);
        assertTrue(keys.add(DEGREES));
        assertTrue(keys.add(MINUTES));
        assertTrue(keys.add(SECONDS));
        assertTrue(keys.add(HEMISPHERE));
        if (withNumberFields) {
            assertTrue(keys.add(INTEGER));
            assertTrue(keys.add(FRACTION));
            assertTrue(keys.add(DECIMAL_SEPARATOR));
        }
        return keys;
    }

    /**
     * The {@value FormattedCharacterIteratorTest#LATITUDE_STRING} character string
     * with attributes. Built in a sub-class of {@link SimpleCharacterIterator} in
     * order to have access to the protected {@link #upper} field.
     */
    @SuppressWarnings("serial")
    private static class LatitudeString extends SimpleCharacterIterator {
        LatitudeString() {
            super(LATITUDE_STRING);
        }

        /**
         * Creates and returns an attributed {@link FormattedCharacterIterator}
         * with the attributes.
         */
        FormattedCharacterIterator iterator(final boolean o) {
            final FormattedCharacterIterator it = new FormattedCharacterIterator(this);
            int start=0; upper= 2; if (o) it.addFieldLimit(INTEGER,            45, start);
                         upper= 3;        it.addFieldLimit(DEGREES,            45, start);
            start=upper; upper= 5; if (o) it.addFieldLimit(INTEGER,            30, start);
                         upper= 6;        it.addFieldLimit(MINUTES,            30, start);
            start=upper; upper= 8; if (o) it.addFieldLimit(INTEGER,            15, start);
            start=upper; upper= 9; if (o) it.addFieldLimit(DECIMAL_SEPARATOR, '.', start);
            start=upper; upper=10; if (o) it.addFieldLimit(FRACTION,            0, start);
            start=6;     upper=11;        it.addFieldLimit(SECONDS,           15f, start);
            start=upper; upper=12;        it.addFieldLimit(HEMISPHERE,        'N', start);
            assertEquals(text.length(), upper);
            return it;
        }
    }

    /**
     * Tests an iteration with attributes, optionally having {@code INTEGER} attributes
     * overlapping the {@code DEGREES}/{@code MINUTES}/{@code SECONDS} ones. The given
     * iterator shall iterates over the {@code "45°30′15.0″N"} characters.
     *
     * <p>This test is leveraged by {@link AngleFormatTest#testFormatToCharacterIterator()}.</p>
     */
    @SuppressWarnings("fallthrough")
    static void testAttributes(final AttributedCharacterIterator it, final boolean withNumberFields) {
        assertEquals(getAllAttributeKeys(withNumberFields), it.getAllAttributeKeys());
        assertEquals(0, it.getIndex());
        assertEquals(3, it.getRunLimit(MINUTES));
        assertEquals(6, it.getRunLimit(SECONDS));

        for (char c=it.first(); c!=DONE; c=it.next()) {
            final int index = it.getIndex();
            final AngleFormat.Field key;
            final Comparable<?> value;
            final int start, limit;
                 if (index <  3) {key=DEGREES;    value=45 ; start= 0; limit= 3;}
            else if (index <  6) {key=MINUTES;    value=30 ; start= 3; limit= 6;}
            else if (index < 11) {key=SECONDS;    value=15f; start= 6; limit=11;}
            else                 {key=HEMISPHERE; value='N'; start=11; limit=12;}
            /*
             * Expected values when asking for a NumberFormat field.
             * Initialized to the values when no such field exists,
             * then updated if overlapping fields are allowed.
             */
            boolean isInteger      = false;
            boolean isSeparator    = false;
            boolean isFraction     = false;
            int     startInteger   =  0;
            int     limitInteger   = 12;
            int     startSeparator =  0;
            int     limitSeparator = 12;
            int     startFraction  =  0;
            int     limitFraction  = 12;
            int     numAttributes  =  1;
            if (withNumberFields) {
                /*
                 * Update the above expected values when the current position
                 * is inside a NumberFormat field.
                 */
                switch (index) {
                    case 0: case 1: // Degrees
                    case 3: case 4: // Minutes
                    case 6: case 7: isInteger   = true; numAttributes = 2; startInteger = start; limitInteger   =  2+start; break;
                    case 8:         isSeparator = true; numAttributes = 2; startSeparator = 8;   limitSeparator =  9; break;
                    case 9:         isFraction  = true; numAttributes = 2; startFraction  = 9;   limitFraction  = 10; break;
                }
                /*
                 * Update the expected values for fields which are not at the current position.
                 * A search for a field should give the position where the next field start, or
                 * where the previous field ended.
                 */
                if (!isInteger) {
                    if (index < 7) {
                        startInteger = index;   // End of previous integer field.
                        limitInteger = index+1; // Start of next integer field.
                    } else {
                        startInteger = 8;       // End of last integer field.
                    }
                }
                if (!isSeparator) {
                    if (index < 8) limitSeparator = 8; // Start of next separator field.
                    else           startSeparator = 9; // End of previous separator field.
                }
                if (!isFraction) {
                    if (index < 9) limitFraction =  9; // Start of next fraction field.
                    else           startFraction = 10; // End of previous fraction field.
                }
            }
            final Map<Attribute,Object> attributes = it.getAttributes();
            assertEquals("attributes.size", numAttributes, attributes.size());
            assertEquals("attributes.get",  value,         attributes.get(key));
            assertEquals("attributes.get",  isInteger,     attributes.get(INTEGER) != null);
            assertEquals("attributes.get",  isFraction,    attributes.get(FRACTION) != null);
            assertEquals("attributes.get",  isSeparator,   attributes.get(DECIMAL_SEPARATOR) != null);

            assertEquals("getRunStart", start,          it.getRunStart(key));
            assertEquals("getRunLimit", limit,          it.getRunLimit(key));
            assertEquals("getRunStart", startInteger,   it.getRunStart(INTEGER));
            assertEquals("getRunLimit", limitInteger,   it.getRunLimit(INTEGER));
            assertEquals("getRunStart", startFraction,  it.getRunStart(FRACTION));
            assertEquals("getRunLimit", limitFraction,  it.getRunLimit(FRACTION));
            assertEquals("getRunStart", startSeparator, it.getRunStart(DECIMAL_SEPARATOR));
            assertEquals("getRunLimit", limitSeparator, it.getRunLimit(DECIMAL_SEPARATOR));
            assertEquals("getRunStart", max(max(max(startInteger, startSeparator), startFraction), start), it.getRunStart());
            assertEquals("getRunLimit", min(min(min(limitInteger, limitSeparator), limitFraction), limit), it.getRunLimit());
        }
    }
}
