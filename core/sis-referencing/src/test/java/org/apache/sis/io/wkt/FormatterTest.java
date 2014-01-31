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

import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.internal.util.X364;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.util.Collections.singletonMap;
import static org.apache.sis.referencing.Assert.*;


/**
 * Tests the {@link Formatter} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@DependsOn({ConventionTest.class, SymbolsTest.class})
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
     * Tests {@link Formatter#append(IdentifiedObject)} with a name that contains the quote character.
     * We test that the closing quote character is doubled.
     */
    @Test
    public void testAppendIdentifiedObject() {
        assertWktEquals("IdentifiedObject[“My “object””.”]",
                new AbstractIdentifiedObject(singletonMap(IdentifiedObject.NAME_KEY, "My “object”.")));
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
