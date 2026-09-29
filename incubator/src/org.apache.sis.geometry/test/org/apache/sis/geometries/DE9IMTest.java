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
package org.apache.sis.geometries;

import org.apache.sis.geometries.DE9IM.Constraint;
import org.apache.sis.geometries.DE9IM.Element;

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link DE9IM}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DE9IMTest {
    /**
     * Creates a new test case.
     */
    public DE9IMTest() {
    }

    /**
     * Tests the creation of matrices with all cells untested.
     */
    @Test
    public void testDefault() {
        assertEquals(4, DE9IM.order4().getOrder());
        assertEquals(9, DE9IM.order9().getOrder());
        assertEquals("NNNN",      DE9IM.order4().toString());
        assertEquals("NNNNNNNNN", DE9IM.order9().toString());
        assertEquals(Constraint.ANY, DE9IM.order9().get(Element.BOUNDARY, Element.EXTERIOR));
    }

    /**
     * Tests the configuration of individual cells.
     */
    @Test
    public void testSet() {
        final DE9IM matrix = DE9IM.order9().set(Element.INTERIOR, Element.INTERIOR, Constraint.NON_EMPTY)
                                           .set(Element.EXTERIOR, Element.INTERIOR, Constraint.EMPTY)
                                           .set(Element.EXTERIOR, Element.BOUNDARY, Constraint.EMPTY);
        assertEquals("TNNNNNFFN", matrix.toString());
        assertEquals(Constraint.NON_EMPTY, matrix.get(Element.INTERIOR, Element.INTERIOR));
        assertEquals(Constraint.EMPTY,     matrix.get(Element.EXTERIOR, Element.BOUNDARY));
        assertEquals(Constraint.ANY,       matrix.get(Element.EXTERIOR, Element.EXTERIOR));

        final DE9IM order4 = DE9IM.order4().set(Element.CLOSURE, Element.CLOSURE, Constraint.NON_EMPTY);
        assertEquals("TNNN", order4.toString());
        /*
         * The interior and the boundary exist only in a matrix of order 9,
         * and the closure only in a matrix of order 4.
         */
        assertThrows(IllegalArgumentException.class, () -> order4.get(Element.INTERIOR, Element.EXTERIOR));
        assertThrows(IllegalArgumentException.class, () -> matrix.get(Element.CLOSURE,  Element.EXTERIOR));
    }

    /**
     * Tests the conversions between a matrix and its string form.
     */
    @Test
    public void testString() {
        assertEquals("FFNFFNNNN", DE9IM.valueOf("FFNFFNNNN").toString());
        assertEquals("FNNN",      DE9IM.valueOf("FNNN")     .toString());
        assertEquals("0123TFNNN", DE9IM.valueOf("0123TFNNN").toString());
        /*
         * The `*` character is synonymous of `N` and lower cases are accepted,
         * but the canonical form uses `N` and upper cases.
         */
        assertEquals("FTNNNNNNN", DE9IM.valueOf("FT*******").toString());
        assertEquals("FTNNNNNNN", DE9IM.valueOf("ft*******").toString());
        assertEquals(DE9IM.valueOf("FT*******"), DE9IM.valueOf("FTNNNNNNN"));

        assertThrows(IllegalArgumentException.class, () -> DE9IM.valueOf("TNN"));       // Invalid length.
        assertThrows(IllegalArgumentException.class, () -> DE9IM.valueOf("TNNNNNNNNN"));
        assertThrows(IllegalArgumentException.class, () -> DE9IM.valueOf("TNN4"));      // Invalid symbol.
    }

    /**
     * Tests the conversions between a matrix and its integer form.
     */
    @Test
    public void testInt() {
        for (final String pattern : new String[] {"NNNN", "FNNN", "TNNNNNFFN", "0123TFNNN", "NNNNNNNNN"}) {
            final DE9IM matrix = DE9IM.valueOf(pattern);
            assertEquals(matrix, DE9IM.valueOf(matrix.toInt()), pattern);
        }
        /*
         * A matrix of order 4 and a matrix of order 9 with the same cells in their
         * first 4 positions shall nevertheless have different integer forms.
         */
        assertNotEquals(DE9IM.valueOf("FNNN").toInt(), DE9IM.valueOf("FNNNNNNNN").toInt());
        assertEquals(0, DE9IM.order4().toInt());

        assertThrows(IllegalArgumentException.class, () -> DE9IM.valueOf(7));            // No such constraint.
        assertThrows(IllegalArgumentException.class, () -> DE9IM.valueOf(1 << 12));      // Bit outside the cells.
    }

    /**
     * Tests the evaluation of a matrix of order 9 against computed intersection dimensions.
     */
    @Test
    public void testMatchesOrder9() {
        // A polygon containing a point. The point has no boundary, hence the empty column in the middle.
        final int[] dimensions = {
             0, -1,  2,
            -1, -1,  1,
            -1, -1,  2
        };
        assertTrue (DE9IM.valueOf("TNNNNNFFN").matches(dimensions));     // contains
        assertTrue (DE9IM.valueOf("T********").matches(dimensions));
        assertTrue (DE9IM.valueOf("0********").matches(dimensions));     // Exactly dimension 0.
        assertFalse(DE9IM.valueOf("1********").matches(dimensions));
        assertFalse(DE9IM.valueOf("F********").matches(dimensions));
        assertFalse(DE9IM.valueOf("NFFFNFNNF").matches(dimensions));     // equals
        assertTrue (DE9IM.valueOf("NNNNNNNNN").matches(dimensions));     // Everything untested.
        /*
         * A digit requires the intersection to be of exactly that dimension,
         * as in OGC Simple Feature Access rather than in ISO 19107.
         */
        assertTrue (DE9IM.valueOf("NNNNNNNN2").matches(dimensions));
        assertFalse(DE9IM.valueOf("NNNNNNNN3").matches(dimensions));
        assertFalse(DE9IM.valueOf("NNNNNNNN1").matches(dimensions));
        assertFalse(DE9IM.valueOf("NNNNNN0NN").matches(dimensions));     // Empty intersection.

        assertThrows(IllegalArgumentException.class, () -> DE9IM.valueOf("TNNNNNFFN").matches(0, 1, 2, 3));
    }

    /**
     * Tests the evaluation of a matrix of order 4, where the closure merges
     * the interior and the boundary of the order 9 matrix.
     */
    @Test
    public void testMatchesOrder4() {
        // Two disjoint polygons: only the intersections with the exteriors are non-empty.
        final int[] disjoint = {
            -1, -1,  2,
            -1, -1,  1,
             2,  1,  2
        };
        assertTrue (DE9IM.valueOf("FNNN").matches(disjoint));            // disjoint
        assertFalse(DE9IM.valueOf("TNNN").matches(disjoint));            // intersects
        /*
         * The dimension of the intersection of a closure with the exterior is the greatest
         * dimension among the interior and the boundary intersections with that exterior.
         */
        assertTrue (DE9IM.valueOf("F2NN").matches(disjoint));
        assertFalse(DE9IM.valueOf("F1NN").matches(disjoint));

        // Two polygons sharing a boundary line: only the boundaries intersect.
        final int[] touches = {
            -1, -1,  2,
            -1,  1,  1,
             2,  1,  2
        };
        assertTrue (DE9IM.valueOf("TNNN").matches(touches));             // intersects
        assertFalse(DE9IM.valueOf("FNNN").matches(touches));             // disjoint
        assertTrue (DE9IM.valueOf("1NNN").matches(touches));             // Exactly dimension 1.
        assertFalse(DE9IM.valueOf("0NNN").matches(touches));
    }
}
