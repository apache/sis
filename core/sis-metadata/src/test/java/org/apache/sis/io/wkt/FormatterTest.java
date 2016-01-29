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

import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import org.opengis.util.CodeList;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.DefaultVerticalExtent;
import org.apache.sis.measure.Units;
import org.apache.sis.internal.util.X364;

// Test imports
import org.apache.sis.test.mock.VerticalCRSMock;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests the {@link Formatter} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.5
 * @module
 */
@DependsOn({ConventionTest.class, SymbolsTest.class, ColorsTest.class})
public final strictfp class FormatterTest extends TestCase {
    /**
     * Verifies the ANSI escape sequences hard-coded in {@link Formatter}.
     */
    @Test
    public void testAnsiEscapeSequences() {
        assertEquals("FOREGROUND_DEFAULT", X364.FOREGROUND_DEFAULT.sequence(), Formatter.FOREGROUND_DEFAULT);
        assertEquals("BACKGROUND_DEFAULT", X364.BACKGROUND_DEFAULT.sequence(), Formatter.BACKGROUND_DEFAULT);
    }

    /**
     * Tests (indirectly) {@link Formatter#quote(String)}.
     */
    @Test
    public void testQuote() {
        assertWktEquals(Convention.WKT2,
                "“A \"quote\" to replace”",             // Expect replacement of non-latin characters.
                 "A “quote” to replace");
        assertWktEquals(Convention.INTERNAL,
                "“A “quote”” to double”",               // Expect doubling quotes.
                 "A “quote” to double");
        assertWktEquals(Convention.WKT2,
                "“Nouvelle Triangulation Francaise”",   // Expect replacement of non-latin characters.
                 "Nouvelle Triangulation Française");
        assertWktEquals(Convention.INTERNAL,
                "“Nouvelle Triangulation Française”",   // Non-latin characters shall be preserved in internal mode.
                 "Nouvelle Triangulation Française");
    }

    /**
     * Tests (indirectly) {@link Formatter#append(GeographicBoundingBox, int)}.
     */
    @Test
    public void testAppendGeographicBoundingBox() {
        assertWktEquals(Convention.WKT2_SIMPLIFIED, "BBox[51.43, 2.54, 55.77, 6.40]",
                new DefaultGeographicBoundingBox(2.54, 6.40, 51.43, 55.77));
        assertWktEquals(Convention.WKT1, "BBOX[51.43, 2.54, 55.77, 6.40]",
                new DefaultGeographicBoundingBox(2.54, 6.40, 51.43, 55.77));
    }

    /**
     * Tests (indirectly) formatting of a vertical extent.
     */
    @Test
    public void testAppendVerticalExtent() {
        final DefaultVerticalExtent extent = new DefaultVerticalExtent(102, 108, VerticalCRSMock.HEIGHT_ft);
        assertWktEquals(Convention.WKT2_SIMPLIFIED, "VerticalExtent[102, 108, Unit[“ft”, 0.3048]]", extent);
        assertWktEquals(Convention.WKT2, "VERTICALEXTENT[102, 108, LENGTHUNIT[“ft”, 0.3048]]", extent);

        extent.setMinimumValue(100.2);
        extent.setMaximumValue(100.8);
        assertWktEquals(Convention.WKT2, "VERTICALEXTENT[100.2, 100.8, LENGTHUNIT[“ft”, 0.3048]]", extent);
    }

    /**
     * Tests (indirectly) {@link Formatter#append(Unit)}.
     */
    @Test
    public void testAppendUnit() {
        assertWktEquals(Convention.WKT2, "LENGTHUNIT[“metre”, 1]", SI.METRE);
        assertWktEquals(Convention.WKT2, "ANGLEUNIT[“degree”, 0.017453292519943295]", NonSI.DEGREE_ANGLE);
        assertWktEquals(Convention.WKT2, "SCALEUNIT[“parts per million”, 1.0E-6]", Units.PPM);

        assertWktEquals(Convention.WKT1, "UNIT[“metre”, 1]", SI.METRE);
        assertWktEquals(Convention.WKT1, "UNIT[“degree”, 0.017453292519943295]", NonSI.DEGREE_ANGLE);
        assertWktEquals(Convention.WKT1, "UNIT[“parts per million”, 1.0E-6]", Units.PPM);
    }

    /**
     * Tests (indirectly) {@link Formatter#append(CodeList)}.
     */
    @Test
    public void testAppendCodeList() {
        assertWktEquals(Convention.WKT2, "northEast",  AxisDirection.NORTH_EAST);
        assertWktEquals(Convention.WKT1, "NORTH_EAST", AxisDirection.NORTH_EAST);
    }
}
