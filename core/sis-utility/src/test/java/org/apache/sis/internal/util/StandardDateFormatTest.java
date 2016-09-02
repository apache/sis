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
package org.apache.sis.internal.util;

import java.text.ParseException;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link StandardDateFormat} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.8
 * @module
 */
public final strictfp class StandardDateFormatTest extends TestCase {
    /**
     * Verifies the condition documented in {@link StandardDateFormat#SHORT_PATTERN} javadoc.
     */
    @Test
    public void testDatePatterns() {
        assertTrue(StandardDateFormat.PATTERN.startsWith(StandardDateFormat.SHORT_PATTERN));
    }

    /**
     * Tests {@link StandardDateFormat.Fix} class.
     */
    @Test
    public void testAdaptText() {
        StandardDateFormat.Fix fix = StandardDateFormat.Fix.apply("2016-06-27T16:48:12Z", 0, 0);
        assertEquals("fix.input", "2016-06-27T16:48:12.000Z", fix.text);
        assertEquals("An index before", 18, fix.adjustIndex(18));
        assertEquals("An index after",  19, fix.adjustIndex(23));

        fix = StandardDateFormat.Fix.apply("2016-06-27T16:48:12.48Z", 0, 0);
        assertEquals("fix.input", "2016-06-27T16:48:12.480Z", fix.text);
        assertEquals("An index before", 18, fix.adjustIndex(18));
        assertEquals("An index after",  22, fix.adjustIndex(23));
    }

    /**
     * Tests parsing a date.
     * Since the implementation is completely different in JDK8 branch than in previous branch,
     * a key purpose of this test is to ensure that the parsing is consistent between the branches.
     *
     * @throws ParseException if an error occurred while parsing the date.
     */
    @Test
    public void testParse() throws ParseException {
        final long day = 1466985600000L;
        final StandardDateFormat f = new StandardDateFormat();
        assertEquals("millis", day + ((16*60 + 48)*60     )*1000,      f.parse("2016-06-27T16:48Z")      .getTime());
        assertEquals("millis", day + ((16*60 + 48)*60 + 12)*1000,      f.parse("2016-06-27T16:48:12Z")   .getTime());
        assertEquals("millis", day,                                    f.parse("2016-06-27")             .getTime());
        assertEquals("millis", day + (( 3*60 +  2)*60 +  1)*1000 + 90, f.parse("2016-06-27T03:02:01.09Z").getTime());
    }
}
