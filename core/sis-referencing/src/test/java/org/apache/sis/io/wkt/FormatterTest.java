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
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultVerticalExtent;
import org.apache.sis.metadata.iso.extent.DefaultTemporalExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.citation.HardCodedCitations;
import org.apache.sis.referencing.AbstractReferenceSystem;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.internal.util.X364;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.opengis.referencing.ReferenceSystem.*;
import static org.apache.sis.referencing.Assert.*;


/**
 * Tests the {@link Formatter} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
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
     * Tests {@link Formatter#append(IdentifiedObject)} with a name that contains the quote character
     * and optional information. We test that the closing quote character is doubled and the optional
     * information properly formatted.
     */
    @Test
    public void testAppendIdentifiedObject() {
        final Map<String,Object> properties = new HashMap<>(8);
        assertNull(properties.put(NAME_KEY, "My “object”."));
        assertNull(properties.put(SCOPE_KEY, "Large scale topographic mapping and cadastre."));
        assertNull(properties.put(REMARKS_KEY, "注です。"));
        assertNull(properties.put(IDENTIFIERS_KEY, new ImmutableIdentifier(
                HardCodedCitations.OGP, "EPSG", "4326", "8.2", null)));
        assertNull(properties.put(DOMAIN_OF_VALIDITY_KEY, new DefaultExtent("Netherlands offshore.",
                new DefaultGeographicBoundingBox(2.54, 6.40, 51.43, 55.77),
                new DefaultVerticalExtent(10, 1000, HardCodedCRS.DEPTH),
                new DefaultTemporalExtent()))); // TODO: needs sis-temporal module for testing that one.
        final IdentifiedObject object = new AbstractReferenceSystem(properties);

        assertWktEquals(Convention.WKT1,
                // Closing quote conservatively omitted for WKT 1.
                "ReferenceSystem[“My “object.”, AUTHORITY[“EPSG”, “4326”]]",
                object);

        assertWktEquals(Convention.WKT2,
                "ReferenceSystem[“My “object””.”,\n" +
                "  SCOPE[“Large scale topographic mapping and cadastre.”],\n" +
                "  AREA[“Netherlands offshore.”],\n" +
                "  BBOX[51.43, 2.54, 55.77, 6.40],\n" +
                "  VERTICALEXTENT[-1000, -10, LENGTHUNIT[“metre”, 1.0]],\n" +
                "  ID[“EPSG”, 4326, “8.2”, “OGP”],\n" +
                "  REMARKS[“注です。”]]",
                object);

        assertWktEquals(Convention.WKT2_SIMPLIFIED,
                "ReferenceSystem[“My “object””.”,\n" +
                "  SCOPE[“Large scale topographic mapping and cadastre.”],\n" +
                "  AREA[“Netherlands offshore.”],\n" +
                "  BBOX[51.43, 2.54, 55.77, 6.40],\n" +
                "  VERTICALEXTENT[-1000, -10],\n" +
                "  ID[“EPSG”, 4326, “8.2”, “OGP”],\n" +
                "  REMARKS[“注です。”]]",
                object);
    }

    /**
     * Tests {@link Formatter#append(Matrix)}.
     */
    @Test
    public void testAppendMatrix() {
        final Matrix m = new Matrix4(
                1, 0, 4, 0,
               -2, 1, 0, 0,
                0, 0, 1, 7,
                0, 0, 0, 1);
        assertWktEquals(
                "PARAMETER[“num_row”, 4],\n"    +
                "PARAMETER[“num_col”, 4],\n"    +
                "PARAMETER[“elt_0_2”, 4.0],\n"  +
                "PARAMETER[“elt_1_0”, -2.0],\n"  +
                "PARAMETER[“elt_2_3”, 7.0]", m);
    }
}
