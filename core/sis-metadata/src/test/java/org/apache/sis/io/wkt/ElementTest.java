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

import java.util.Locale;
import java.text.ParsePosition;
import java.text.ParseException;
import org.apache.sis.util.CharSequences;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link Element} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final strictfp class ElementTest extends TestCase {
    /**
     * A dummy parser to be given to the {@link Element} constructor.
     */
    private final Parser parser = new Parser(Symbols.SQUARE_BRACKETS, Locale.ENGLISH) {
        @Override Object parse(Element element) throws ParseException {
            throw new UnsupportedOperationException();
        }
    };

    /**
     * Parses the given text and ensures that {@link ParsePosition} index is set at to the end of string.
     */
    private Element parse(final String text) throws ParseException {
        final ParsePosition position = new ParsePosition(0);
        final Element element = new Element(parser, text, position);
        assertEquals("errorIndex", -1, position.getErrorIndex());
        assertEquals("index", CharSequences.skipTrailingWhitespaces(text, 0, text.length()), position.getIndex());
        return element;
    }

    /**
     * Tests {@link Element#pullString(String)}.
     *
     * @throws ParseException if an error occurred during the parsing.
     */
    @Test
    public void testPullString() throws ParseException {
        Element element = parse("Datum[\"World Geodetic System 1984\"]");
        assertEquals("keyword", "Datum", element.keyword);
        assertEquals("value", "World Geodetic System 1984", element.pullString("value"));
        element.close();

        // Spaces inside quotes should be preserved.
        element = parse("  Datum [  \" World Geodetic System 1984  \"  ]  ");
        assertEquals("keyword", "Datum", element.keyword);
        assertEquals("value", " World Geodetic System 1984  ", element.pullString("value"));
        element.close();

        // Consecutive values.
        element = parse("A[\"B\", \"C\"]");
        assertEquals("keyword", "A", element.keyword);
        assertEquals("first",   "B", element.pullString("first"));
        assertEquals("second",  "C", element.pullString("second"));
        element.close();
    }

    /**
     * Tests {@link Element#pullString(String)} with enclosed quotes.
     * Also opportunistically tests different kinds of quotes.
     *
     * @throws ParseException if an error occurred during the parsing.
     */
    @Test
    @DependsOnMethod("testPullString")
    public void testEnclosedQuotes() throws ParseException {
        Element element = parse("A[“text.”]");
        assertEquals("keyword", "A", element.keyword);
        assertEquals("value", "text.", element.pullString("value"));
        element.close();

        // No need to double the enclosed quotes here.
        element = parse("A[“text with \"quotes\".”]");
        assertEquals("keyword", "A", element.keyword);
        assertEquals("value", "text with \"quotes\".", element.pullString("value"));
        element.close();

        // Those enclosed quotes need to be doubled.
        element = parse("A[\"text with \"\"double quotes\"\".\"]");
        assertEquals("keyword", "A", element.keyword);
        assertEquals("value", "text with \"double quotes\".", element.pullString("value"));
        element.close();
    }

    /**
     * Tests {@link Element#pullDouble(String)} for many consecutive values,
     * including usage of exponential notation.
     *
     * @throws ParseException if an error occurred during the parsing.
     */
    @Test
    public void testPullDouble() throws ParseException {
        Element element = parse("B[3.1, 4.2, 5.3E3, 6.4e3]");
        assertEquals("B", element.keyword);
        assertEquals("first",  3.1, element.pullDouble("first"),  STRICT);
        assertEquals("second", 4.2, element.pullDouble("second"), STRICT);
        assertEquals("third", 5300, element.pullDouble("third"),  STRICT);
        assertEquals("forth", 6400, element.pullDouble("forth"),  STRICT);
        element.close();
    }
}
