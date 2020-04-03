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
package org.apache.sis.image;

import java.util.Random;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link ResamplingGrid}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class ResamplingGridTest extends TestCase {
    /**
     * Tests the {@link ResamplingGrid#roundIfAlmostInteger(AffineTransform)} method.
     */
    @Test
    public void testRoundIfAlmostInteger() {
        final AffineTransform test = new AffineTransform(4, 0, 0, 4, -400, -1186);
        final AffineTransform copy = new AffineTransform(test);
        ResamplingGrid.roundIfAlmostInteger(test);
        assertEquals("Translation terms were already integers, so the " +
                "transform should not have been modified.", copy, test);

        test.translate(ResamplingGrid.EPS/8, -ResamplingGrid.EPS/8);
        ResamplingGrid.roundIfAlmostInteger(test);
        assertEquals("Translation terms should have been rounded.", copy, test);

        test.translate(ResamplingGrid.EPS*2, -ResamplingGrid.EPS*2);
        ResamplingGrid.roundIfAlmostInteger(test);
        assertFalse("Treshold was smaller than the translation, so the " +
                "transform should not have been modified.", copy.equals(test));
    }

    /**
     * Tests the {@link ResamplingGrid} class using an affine transform.
     * Because the transform is linear, results should be identical ignoring rounding errors.
     *
     * @throws TransformException if an error occurred while transforming a coordinate.
     */
    @Test
    public void compareWithAffine() throws TransformException {
        final AffineTransform2D reference = new AffineTransform2D(0.25, 0, 0, 2.5, 4, 2);
        final Rectangle         bounds    = new Rectangle(-7, 3, 12, 8);
        final ResamplingGrid    grid      = new ResamplingGrid(reference, bounds, new Dimension(4,3));
        final Random            random    = TestUtilities.createRandomNumberGenerator(-854734760285695284L);
        final double[]          source    = new double[2];
        final double[]          actual    = new double[2];
        final double[]          expected  = new double[2];
        for (int i=0; i<100; i++) {
            source[0] = bounds.x + bounds.width  * random.nextDouble();
            source[1] = bounds.y + bounds.height * random.nextDouble();
            grid.transform(source, 0, actual, 0, 1);

            source[0] += 0.5;       // Was relative to pixel center, make it relative to pixel corner.
            source[1] += 0.5;
            reference.transform(source, 0, expected, 0, 1);
            assertArrayEquals(expected, actual, Numerics.COMPARISON_THRESHOLD);
        }
    }
}
