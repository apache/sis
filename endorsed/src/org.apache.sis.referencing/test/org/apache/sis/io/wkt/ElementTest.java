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

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.time.Instant;
import java.text.ParsePosition;
import java.text.ParseException;
import org.apache.sis.util.CharSequences;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link Element} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ElementTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public ElementTest() {
    }

    /**
     * A dummy parser to be given to the {@link Element} constructor.
     */
    private final AbstractParser parser = new AbstractParser(
            null, new HashMap<>(2), Symbols.SQUARE_BRACKETS, null, null, null, Locale.ENGLISH)
    {
        @Override String getPublicFacade() {
            throw new UnsupportedOperationException();
        }

        @Override Object buildFromTree(Element element) throws ParseException {
            throw new UnsupportedOperationException();
        }
    };

    /**
     * Parses the given text and ensures that {@link ParsePosition} index is set at to the end of string.
     */
    private Element parse(final String text) throws ParseException {
        final ParsePosition position = new ParsePosition(0);
        final Element element;
        try {
            element = new Element(parser, text, position);
        } catch (ParseException e) {
            assertEquals(0, position.getIndex(), "index should be unchanged.");
            assertTrue(position.getErrorIndex() > 0, "Error index should be set.");
            assertEquals(position.getErrorIndex(), e.getErrorOffset(), "Expected consistent error indices.");
            throw e;
        }
        assertEquals(-1, position.getErrorIndex(), "errorIndex");
        assertEquals(CharSequences.skipTrailingWhitespaces(text, 0, text.length()), position.getIndex(), "index");
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
        assertEquals("Datum", element.keyword);
        assertEquals("World Geodetic System 1984", element.pullString("value"));
        element.close(null);

        // Alternative bracket and quote characters.
        element = parse("Datum(“World Geodetic System 1984”)");
        assertEquals("Datum", element.keyword);
        assertEquals("World Geodetic System 1984", element.pullString("value"));
        element.close(null);

        // Leading and trailing spaces inside quotes should be ignored (ISO 19162 §B.4).
        element = parse("  Datum [  \" World Geodetic System 1984  \"  ]  ");
        assertEquals("Datum", element.keyword);
        assertEquals("World Geodetic System 1984", element.pullString("value"));
        element.close(null);

        // Consecutive values.
        element = parse("A[\"B\", \"C\"]");
        assertEquals("A", element.keyword);
        assertEquals("B", element.pullString("first"));
        assertEquals("C", element.pullString("second"));
        element.close(null);
    }

    /**
     * Tests {@link Element#pullString(String)} with enclosed quotes.
     * Also opportunistically tests different kinds of quotes.
     *
     * @throws ParseException if an error occurred during the parsing.
     */
    @Test
    public void testEnclosedQuotes() throws ParseException {
        Element element = parse("A[“text.”]");
        assertEquals("A", element.keyword);
        assertEquals("text.", element.pullString("value"));
        element.close(null);

        // No need to double the enclosed quotes here.
        element = parse("A[“text with \"quotes\".”]");
        assertEquals("A", element.keyword);
        assertEquals("text with \"quotes\".", element.pullString("value"));
        element.close(null);

        // Those enclosed quotes need to be doubled.
        element = parse("A[\"text with \"\"double quotes\"\".\"]");
        assertEquals("A", element.keyword);
        assertEquals("text with \"double quotes\".", element.pullString("value"));
        element.close(null);
    }

    /**
     * Tests {@link Element#pullDouble(String)} for many consecutive values,
     * including usage of exponential notation.
     *
     * @throws ParseException if an error occurred during the parsing.
     */
    @Test
    public void testPullDouble() throws ParseException {
        Element element = parse("C[3.1, 4.2, 5.3E3, 6.4e3]");
        assertEquals("C", element.keyword);
        assertEquals( 3.1, element.pullDouble("first"));
        assertEquals( 4.2, element.pullDouble("second"));
        assertEquals(5300, element.pullDouble("third"));
        assertEquals(6400, element.pullDouble("forth"));
        element.close(null);
    }

    /**
     * Tests {@link Element#pullInteger(String)} for many consecutive values.
     *
     * @throws ParseException if an error occurred during the parsing.
     */
    @Test
    public void testPullInteger() throws ParseException {
        final Element element = parse("B[3, 7, -5, 6]");
        assertEquals("B", element.keyword);
        assertEquals( 3, element.pullInteger("first"));
        assertEquals( 7, element.pullInteger("second"));
        assertEquals(-5, element.pullInteger("third"));
        assertEquals( 6, element.pullInteger("forth"));
        element.close(null);
        /*
         * Tests error message.
         */
        final Element next = parse("B[6.5]");
        var e = assertThrows(ParseException.class, () -> next.pullInteger("forth"));
        assertEquals("Text “6.5” cannot be parsed as an object of type ‘Integer’.", e.getLocalizedMessage());
    }

    /**
     * Tests {@link Element#pullDate(String)}.
     *
     * @throws ParseException if an error occurred during the parsing.
     */
    @Test
    public void testPullDate() throws ParseException {
        Element element = parse("TimeOrigin[1858-11-17T00:00:00.0Z]");
        assertEquals("TimeOrigin", element.keyword);
        assertEquals(Instant.parse("1858-11-17T00:00:00Z"), element.pullTime("date"));
        element.close(null);
    }

    /**
     * Tests {@link Element#pullBoolean(String)}.
     *
     * @throws ParseException if an error occurred during the parsing.
     */
    @Test
    public void testPullBoolean() throws ParseException {
        Element element = parse("ConformanceResult[true]");
        assertEquals("ConformanceResult", element.keyword);
        assertTrue(element.pullBoolean("pass"));
        element.close(null);

        element = parse("ConformanceResult[false]");
        assertEquals("ConformanceResult", element.keyword);
        assertFalse(element.pullBoolean("pass"));
        element.close(null);
        /*
         * Tests error message.
         */
        final Element next = parse("ConformanceResult[falseX]");
        var e = assertThrows(ParseException.class, () -> next.pullBoolean("pass"));
        assertEquals("Missing a “pass” component in “ConformanceResult”.", e.getLocalizedMessage());
    }

    /**
     * Tests {@link Element#pullElement(int, String...)}. This implies testing {@code Element} nesting.
     *
     * @throws ParseException if an error occurred during the parsing.
     */
    @Test
    public void testPullElement() throws ParseException {
        Element element = parse("TimeDatum[“Modified Julian”, TimeOrigin[1858-11-17T00:00:00.0Z]]");
        assertEquals("TimeDatum", element.keyword);
        assertEquals("Modified Julian", element.pullString("name"));
        Element inner = element.pullElement(AbstractParser.MANDATORY, "TimeOrigin");
        assertEquals("TimeOrigin", inner.keyword);
        assertEquals(Instant.parse("1858-11-17T00:00:00Z"), inner.pullTime("date"));
        inner.close(null);
        element.close(null);
    }

    /**
     * Tests {@link Element#close(Map)}.
     * This method should not accept to close an element when it still have unparsed child elements.
     *
     * @throws ParseException if an error occurred during the parsing.
     */
    @Test
    public void testClose() throws ParseException {
        final Element element = parse("A[\"B\", \"C\"]");
        var e = assertThrows(ParseException.class, () -> element.close(null));
        assertEquals("Unexpected value “B” in the “A” element.", e.getLocalizedMessage());
    }

    /**
     * Tests the production of error messages when a parsing fails.
     */
    @Test
    public void testParsingErrors() {
        ParseException e;

        // Missing closing quote or using the wrong quote character.
        e = assertThrows(ParseException.class, () -> parse("QuoteTest[“text\"]"));
        assertEquals("Missing a ‘”’ character in “QuoteTest” element.", e.getLocalizedMessage());

        // Missing closing bracket or using the wrong bracket character.
        e = assertThrows(ParseException.class, () -> parse("BracketTest(“text”]"));
        assertEquals("Cannot parse “]” in element “BracketTest”.", e.getLocalizedMessage());

        e = assertThrows(ParseException.class, () -> parse("BracketTest(“text”"));
        assertEquals("Missing a ‘)’ character in “BracketTest” element.", e.getLocalizedMessage());
    }

    /**
     * Tests the construction of {@link Element} tree from fragments.
     *
     * @throws ParseException if an error occurred during the parsing.
     */
    @Test
    public void testFragments() throws ParseException {
        parser.fragments.put("MyFrag", new StoredTree(parse("Frag[“A”,“B”,“A”]"), new HashMap<>()));
        final Element element = parse("Foo[“C”,$MyFrag,“D”]");
        assertEquals("C", element.pullString("C"));
        assertEquals("D", element.pullString("D"));
        Element frag = element.pullElement(AbstractParser.MANDATORY, "Frag");
        final String a = frag.pullString("A");
        assertEquals("A", a);
        assertEquals("B", frag.pullString("B"));
        assertEquals(a, frag.pullString("A"));
    }
}
