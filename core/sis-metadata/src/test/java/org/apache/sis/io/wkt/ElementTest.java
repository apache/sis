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
import java.text.ParsePosition;
import java.text.ParseException;
import org.apache.sis.util.CharSequences;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestUtilities;
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
    private final AbstractParser parser = new AbstractParser(Symbols.SQUARE_BRACKETS, new HashMap<String,Element>(2),
            null, null, null, Locale.ENGLISH)
    {
        @Override String getPublicFacade() {
            throw new UnsupportedOperationException();
        }

        @Override Object parseObject(Element element) throws ParseException {
            throw new UnsupportedOperationException();
        }
    };

    /**
     * The map of shared values to gives to the {@link Element} constructor.
     * This is usually null, except for the test of WKT fragments.
     */
    private Map<Object,Object> sharedValues;

    /**
     * Parses the given text and ensures that {@link ParsePosition} index is set at to the end of string.
     */
    private Element parse(final String text) throws ParseException {
        final ParsePosition position = new ParsePosition(0);
        final Element element;
        try {
            element = new Element(parser, text, position, sharedValues);
        } catch (ParseException e) {
            assertEquals("index should be unchanged.", 0, position.getIndex());
            assertTrue("Error index should be set.", position.getErrorIndex() > 0);
            assertEquals("Expected consistent error indices.", position.getErrorIndex(), e.getErrorOffset());
            throw e;
        }
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
        element.close(null);

        // Alternative bracket and quote characters.
        element = parse("Datum(“World Geodetic System 1984”)");
        assertEquals("keyword", "Datum", element.keyword);
        assertEquals("value", "World Geodetic System 1984", element.pullString("value"));
        element.close(null);

        // Leading and trailing spaces inside quotes should be ignored (ISO 19162 §B.4).
        element = parse("  Datum [  \" World Geodetic System 1984  \"  ]  ");
        assertEquals("keyword", "Datum", element.keyword);
        assertEquals("value", "World Geodetic System 1984", element.pullString("value"));
        element.close(null);

        // Consecutive values.
        element = parse("A[\"B\", \"C\"]");
        assertEquals("keyword", "A", element.keyword);
        assertEquals("first",   "B", element.pullString("first"));
        assertEquals("second",  "C", element.pullString("second"));
        element.close(null);
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
        element.close(null);

        // No need to double the enclosed quotes here.
        element = parse("A[“text with \"quotes\".”]");
        assertEquals("keyword", "A", element.keyword);
        assertEquals("value", "text with \"quotes\".", element.pullString("value"));
        element.close(null);

        // Those enclosed quotes need to be doubled.
        element = parse("A[\"text with \"\"double quotes\"\".\"]");
        assertEquals("keyword", "A", element.keyword);
        assertEquals("value", "text with \"double quotes\".", element.pullString("value"));
        element.close(null);
    }

    /**
     * Tests {@link Element#pullDouble(String)} for many consecutive values,
     * including usage of exponential notation.
     *
     * @throws ParseException if an error occurred during the parsing.
     */
    @Test
    @DependsOnMethod("testPullString")      // Because there is some common code in the Element class.
    public void testPullDouble() throws ParseException {
        Element element = parse("C[3.1, 4.2, 5.3E3, 6.4e3]");
        assertEquals("keyword", "C", element.keyword);
        assertEquals("first",  3.1, element.pullDouble("first"),  STRICT);
        assertEquals("second", 4.2, element.pullDouble("second"), STRICT);
        assertEquals("third", 5300, element.pullDouble("third"),  STRICT);
        assertEquals("forth", 6400, element.pullDouble("forth"),  STRICT);
        element.close(null);
    }

    /**
     * Tests {@link Element#pullInteger(String)} for many consecutive values.
     *
     * @throws ParseException if an error occurred during the parsing.
     */
    @Test
    @DependsOnMethod("testPullDouble")      // Because there is lot of common code in the Element class.
    public void testPullInteger() throws ParseException {
        Element element = parse("B[3, 7, -5, 6]");
        assertEquals("keyword", "B", element.keyword);
        assertEquals("first",  3, element.pullInteger("first"));
        assertEquals("second", 7, element.pullInteger("second"));
        assertEquals("third", -5, element.pullInteger("third"));
        assertEquals("forth",  6, element.pullInteger("forth"));
        element.close(null);
        /*
         * Tests error message.
         */
        element = parse("B[6.5]");
        try {
            element.pullInteger("forth");
            fail("Double value can not be parsed as an integer.");
        } catch (ParseException e) {
            assertEquals("Text “6.5” can not be parsed as an object of type ‘Integer’.", e.getLocalizedMessage());
        }
    }

    /**
     * Tests {@link Element#pullDate(String)}.
     *
     * @throws ParseException if an error occurred during the parsing.
     */
    @Test
    @DependsOnMethod("testPullDouble")      // Because there is lot of common code in the Element class.
    public void testPullDate() throws ParseException {
        Element element = parse("TimeOrigin[1858-11-17T00:00:00.0Z]");
        assertEquals("keyword", "TimeOrigin", element.keyword);
        assertEquals("date", TestUtilities.date("1858-11-17 00:00:00"), element.pullDate("date"));
        element.close(null);
    }

    /**
     * Tests {@link Element#pullBoolean(String)}.
     *
     * @throws ParseException if an error occurred during the parsing.
     */
    @Test
    @DependsOnMethod("testPullString")      // Because there is some common code in the Element class.
    public void testPullBoolean() throws ParseException {
        Element element = parse("ConformanceResult[true]");
        assertEquals("keyword", "ConformanceResult", element.keyword);
        assertTrue("pass", element.pullBoolean("pass"));
        element.close(null);

        element = parse("ConformanceResult[false]");
        assertEquals("keyword", "ConformanceResult", element.keyword);
        assertFalse("pass", element.pullBoolean("pass"));
        element.close(null);
        /*
         * Tests error message.
         */
        element = parse("ConformanceResult[falseX]");
        try {
            element.pullBoolean("pass");
            fail("Should not accept “falseX” as a boolean.");
        } catch (ParseException e) {
            assertEquals("Missing a “pass” component in “ConformanceResult”.", e.getLocalizedMessage());
        }
    }

    /**
     * Tests {@link Element#pullElement(String)}. This implies testing {@code Element} nesting.
     *
     * @throws ParseException if an error occurred during the parsing.
     */
    @Test
    @DependsOnMethod("testPullDate")
    public void testPullElement() throws ParseException {
        Element element = parse("TimeDatum[“Modified Julian”, TimeOrigin[1858-11-17T00:00:00.0Z]]");
        assertEquals("keyword", "TimeDatum", element.keyword);
        assertEquals("name", "Modified Julian", element.pullString("name"));
        Element inner = element.pullElement(AbstractParser.MANDATORY, "TimeOrigin");
        assertEquals("keyword", "TimeOrigin", inner.keyword);
        assertEquals("date", TestUtilities.date("1858-11-17 00:00:00"), inner.pullDate("date"));
        inner.close(null);
        element.close(null);
    }

    /**
     * Tests {@link Element#close()}.
     *
     * @throws ParseException if an error occurred during the parsing.
     */
    @Test
    public void testClose() throws ParseException {
        final Element element = parse("A[\"B\", \"C\"]");
        try {
            element.close(null);
            fail("Should not close will we still have unparsed elements.");
        } catch (ParseException e) {
            assertEquals("Unexpected value “B” in “A” element.", e.getLocalizedMessage());
        }
    }

    /**
     * Tests the production of error messages when a parsing fails.
     */
    @Test
    public void testParsingErrors() {
        // Missing closing quote or using the wrong quote character.
        try {
            parse("QuoteTest[“text\"]");
            fail("Should complain about missing quote.");
        } catch (ParseException e) {
            assertEquals("Missing a ‘”’ character in “QuoteTest” element.", e.getLocalizedMessage());
        }

        // Missing closing bracket or using the wrong bracket character.
        try {
            parse("BracketTest(“text”]");
            fail("Should complain about missing bracket.");
        } catch (ParseException e) {
            assertEquals("Can not parse “]” in element “BracketTest”.", e.getLocalizedMessage());
        }
        try {
            parse("BracketTest(“text”");
            fail("Should complain about missing bracket.");
        } catch (ParseException e) {
            assertEquals("Missing a ‘)’ character in “BracketTest” element.", e.getLocalizedMessage());
        }
    }

    /**
     * Tests the construction of {@link Element} tree from fragments.
     *
     * @throws ParseException if an error occurred during the parsing.
     */
    @Test
    @DependsOnMethod({"testPullString", "testPullElement"})
    public void testFragments() throws ParseException {
        sharedValues = new HashMap<Object,Object>();
        Element frag = parse("Frag[“A”,“B”,“A”]");
        parser.fragments.put("MyFrag", frag);
        try {
            frag.pullString("A");
            fail("Element shall be unmodifiable.");
        } catch (UnsupportedOperationException e) {
            // This is the expected exception.
        }
        /*
         * Parse a normal value. Since this is not a fragment,
         * we should be able to pull a copy of the components.
         */
        sharedValues = null;
        final Element element = parse("Foo[“C”,$MyFrag,“D”]");
        assertEquals("C", element.pullString("C"));
        assertEquals("D", element.pullString("D"));
        frag = element.pullElement(AbstractParser.MANDATORY, "Frag");
        final String a = frag.pullString("A");
        assertEquals("A", a);
        assertEquals("B", frag.pullString("B"));
        assertSame(a, frag.pullString("A"));    // 'sharedValues' should have allowed to share the same instance.
    }
}
