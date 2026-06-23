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

import java.awt.Point;
import java.awt.image.RenderedImage;
import java.util.Random;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coverage.grid.SequenceType;


/**
 * Tests {@link OverviewImage}.
 *
 * @author  Estelle Idée (Geomatys)
 */
@SuppressWarnings("exports")
public final class OverviewImageTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public OverviewImageTest() {
    }

    /**
     * Tests on an image filled with integer values.
     */
    @Test
    public void testOnIntegers() {
        testForType(DataType.INT);
    }

    /**
     * Tests on an image filled with floating point values.
     * Some random values are set to NaN.
     */
    @Test
    public void testOnFloats() {
        testForType(DataType.DOUBLE);
    }

    /**
     * Runs the test on an image of the specified type.
     *
     * @param  type  type of data stored in the image.
     */
    private static void testForType(final DataType type) {
        final Random r = TestUtilities.createRandomNumberGenerator();
        final var source = new TiledImageMock(
                type.toDataBufferType(),
                r.nextInt( 2) +  1,     // num bands
                r.nextInt( 9) -  4,     // min X
                r.nextInt( 9) -  4,     // min Y
                r.nextInt(20) + 10,     // width
                r.nextInt(20) + 10,     // height
                r.nextInt( 5) +  5,     // tile width
                r.nextInt( 5) +  5,     // tile height
                r.nextInt( 9) -  4,     // min tile X
                r.nextInt( 9) -  4,     // min tile Y
                true);                  // banded

        source.initializeAllTiles();
        if (!type.isInteger()) {
            source.setRandomNaN(r);
        }
        verify(source, new OverviewImage(source), type.isInteger());
    }

    /**
     * Verifies an image which is expected to be the result of an image overview operation.
     *
     * @param  source     the image used for computing the overview.
     * @param  target     the result of the image overview operation.
     * @param  isInteger  whether the images use an integer type.
     */
    public static void verify(final RenderedImage source, final RenderedImage target, final boolean isInteger) {
        assertEquals(source.getWidth()  / 2, target.getWidth());
        assertEquals(source.getHeight() / 2, target.getHeight());
        final int offsetX = source.getMinX() & 1;
        final int offsetY = source.getMinY() & 1;

        double[] p00 = null, p01 = null, p10 = null, p11 = null;
        double[] actual = null;

        final PixelIterator itSource = new PixelIterator.Builder().setIteratorOrder(SequenceType.LINEAR).create(source);
        final PixelIterator itTarget = PixelIterator.create(target);
        int count = 0;
        while (itTarget.next()) {
            final Point p = itTarget.getPosition();
            final int sx = p.x * 2 + offsetX;
            final int sy = p.y * 2 + offsetY;

            // Read 2×2 block from source.
            itSource.moveTo(sx, sy);      p00 = itSource.getPixel(p00);
            assertTrue(itSource.next());  p01 = itSource.getPixel(p01);
            itSource.moveTo(sx, sy + 1);  p10 = itSource.getPixel(p10);
            assertTrue(itSource.next());  p11 = itSource.getPixel(p11);

            actual = itTarget.getPixel(actual);
            for (int b = 0; b < actual.length; b++) {
                int n = 0;
                double sum = 0, v;
                if (!Double.isNaN(v = p00[b])) {sum += v; n++;}
                if (!Double.isNaN(v = p01[b])) {sum += v; n++;}
                if (!Double.isNaN(v = p10[b])) {sum += v; n++;}
                if (!Double.isNaN(v = p11[b])) {sum += v; n++;}
                double expected = (n != 0) ? sum / n : Double.NaN;
                if (isInteger) {
                    expected = (int) expected;
                }
                assertEquals(expected, actual[b], 1E-10, () -> "Mismatch at (" + p.x + ", " + p.y + ')');
            }
            count++;
        }
        assertEquals(target.getWidth() * target.getHeight(), count);
    }
}
