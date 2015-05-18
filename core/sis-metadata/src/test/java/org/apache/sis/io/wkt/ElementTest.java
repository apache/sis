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
     * Tests a {@link Element#pullString(String)}.
     *
     * @throws ParseException if an error occurred during the parsing.
     */
    @Test
    public void testString() throws ParseException {
        final ParsePosition position = new ParsePosition(0);
        final Element element = new Element(new ParserMock(), "Datum[\"World Geodetic System 1984\"]", position);
        assertEquals("Datum", element.keyword);
        assertEquals("World Geodetic System 1984", element.pullString("name"));
    }

    /**
     * A dummy parser for testing purpose.
     */
    private static final class ParserMock extends Parser {
        ParserMock() {
            super(Symbols.SQUARE_BRACKETS, Locale.ENGLISH);
        }

        @Override
        Object parse(Element element) throws ParseException {
            throw new UnsupportedOperationException();
        }
    }
}
