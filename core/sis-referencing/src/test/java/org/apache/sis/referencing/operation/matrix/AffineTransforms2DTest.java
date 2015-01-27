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
package org.apache.sis.referencing.operation.matrix;

import java.awt.geom.AffineTransform;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static java.lang.StrictMath.*;


/**
 * Tests the {@link AffineTransforms2D} static methods.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final strictfp class AffineTransforms2DTest extends TestCase {
    /**
     * Tolerance value for comparisons.
     */
    private static final double EPS = 1E-10;

    /**
     * Tests {@link AffineTransforms2D} in the unflipped case.
     */
    @Test
    public void testUnflipped() {
        runTest(+1);
    }

    /**
     * Tests {@link AffineTransforms2D} in the flipped case.
     */
    @Test
    public void testFlipped() {
        runTest(-1);
    }

    /**
     * Gets the flip state using standard {@code AffineTransform} API.
     */
    private static int getFlipFromType(final AffineTransform tr) {
        return (tr.getType() & AffineTransform.TYPE_FLIP) != 0 ? -1 : +1;
    }

    /**
     * Run the test in the flipped or unflipped case.
     *
     * @param f -1 for the flipped case, or +1 for the unflipped case.
     */
    private static void runTest(final int f) {
        // Test identity
        final AffineTransform tr = new AffineTransform();
        tr.setToScale(1, f);
        assertEquals( 1, AffineTransforms2D.getScaleX0 (tr), EPS);
        assertEquals( 1, AffineTransforms2D.getScaleY0 (tr), EPS);
        assertEquals( 0, AffineTransforms2D.getRotation(tr), EPS);
        assertEquals( 1, AffineTransforms2D.getSwapXY  (tr));
        assertEquals( f, AffineTransforms2D.getFlip    (tr));
        assertEquals( f, getFlipFromType               (tr));

        // Tests rotation (< 45°)
        double r = toRadians(25);
        tr.rotate(r);
        assertEquals( 1, AffineTransforms2D.getScaleX0 (tr), EPS);
        assertEquals( 1, AffineTransforms2D.getScaleY0 (tr), EPS);
        assertEquals( r, AffineTransforms2D.getRotation(tr), EPS);
        assertEquals( 1, AffineTransforms2D.getSwapXY  (tr));
        assertEquals( f, AffineTransforms2D.getFlip    (tr));
        assertEquals( f, getFlipFromType               (tr));

        // Tests more rotation (> 45°)
        r = toRadians(65);
        tr.rotate(toRadians(40));
        assertEquals( 1, AffineTransforms2D.getScaleX0 (tr), EPS);
        assertEquals( 1, AffineTransforms2D.getScaleY0 (tr), EPS);
        assertEquals( r, AffineTransforms2D.getRotation(tr), EPS);
        assertEquals(-1, AffineTransforms2D.getSwapXY  (tr));
        assertEquals( f, AffineTransforms2D.getFlip    (tr));
        assertEquals( f, getFlipFromType               (tr));

        // Tests scale
        tr.setToScale(2, 3*f);
        assertEquals( 2, AffineTransforms2D.getScaleX0 (tr), EPS);
        assertEquals( 3, AffineTransforms2D.getScaleY0 (tr), EPS);
        assertEquals( 0, AffineTransforms2D.getRotation(tr), EPS);
        assertEquals( 1, AffineTransforms2D.getSwapXY  (tr));
        assertEquals( f, AffineTransforms2D.getFlip    (tr));
        assertEquals( f, getFlipFromType               (tr));

        // Tests rotation + scale
        tr.rotate(r);
        assertEquals( 2, AffineTransforms2D.getScaleX0 (tr), EPS);
        assertEquals( 3, AffineTransforms2D.getScaleY0 (tr), EPS);
        assertEquals( r, AffineTransforms2D.getRotation(tr), EPS);
        assertEquals(-1, AffineTransforms2D.getSwapXY  (tr));
        assertEquals( f, AffineTransforms2D.getFlip    (tr));
        assertEquals( 1, getFlipFromType(tr)); // Always unflipped according Java 1.5.0_09...

        // Tests axis swapping
        r = toRadians(-90 * f);
        tr.setTransform(0, 1, f, 0, 0, 0);
        assertEquals( 1, AffineTransforms2D.getScaleX0 (tr), EPS);
        assertEquals( 1, AffineTransforms2D.getScaleY0 (tr), EPS);
        assertEquals( r, AffineTransforms2D.getRotation(tr), EPS);
        assertEquals(-1, AffineTransforms2D.getSwapXY  (tr));
        assertEquals(-f, AffineTransforms2D.getFlip    (tr));
        assertEquals(-f, getFlipFromType               (tr));

        // Tests axis swapping + scale
        tr.scale(2, 3);
        assertEquals( 3, AffineTransforms2D.getScaleX0 (tr), EPS);
        assertEquals( 2, AffineTransforms2D.getScaleY0 (tr), EPS);
        assertEquals( r, AffineTransforms2D.getRotation(tr), EPS);
        assertEquals(-1, AffineTransforms2D.getSwapXY  (tr));
        assertEquals(-f, AffineTransforms2D.getFlip    (tr));
        assertEquals(-f, getFlipFromType               (tr));
    }
}
