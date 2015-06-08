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
package org.apache.sis.io.wkt;

import java.text.ParseException;
import org.opengis.referencing.crs.VerticalCRS;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link WKTFormat}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.6
 * @module
 */
@DependsOn(GeodeticObjectParserTest.class)
public final strictfp class WKTFormatTest extends TestCase {
    /**
     * Verifies the condition documented in {@link WKTFormat#SHORT_DATE_PATTERN} javadoc.
     */
    @Test
    public void testDatePatterns() {
        assertTrue(WKTFormat.DATE_PATTERN.startsWith(WKTFormat.SHORT_DATE_PATTERN));
    }

    /**
     * Tests integration in {@link WKTFormat#parse(CharSequence, ParsePosition)}.
     * This method tests only a simple WKT because it is not the purpose of this
     * method to test the parser itself. We only want to tests its integration in
     * the {@link WKTFormat} class.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    public void testParse() throws ParseException {
        final WKTFormat format = new WKTFormat(null, null);
        final VerticalCRS crs = (VerticalCRS) format.parseObject(
                "VERT_CS[“Gravity-related height”,\n" +
                "  VERT_DATUM[“Mean Sea Level”, 2005],\n" +
                "  UNIT[“metre”, 1],\n" +
                "  AXIS[“Gravity-related height”, UP]]");

        GeodeticObjectParserTest.assertNameAndIdentifierEqual("Gravity-related height", 0, crs);
        GeodeticObjectParserTest.assertNameAndIdentifierEqual("Mean Sea Level", 0, crs.getDatum());
    }
}
