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
package org.apache.sis.image.privy;

import java.util.Random;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import static java.lang.StrictMath.*;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.image.ImageTestCase;
import org.apache.sis.test.TestUtilities;


/**
 * Tests the {@link ScaledColorSpace} implementation.
 * This class contains a visual test which can be run by a main method.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public final class ScaledColorSpaceTest extends ImageTestCase {
    /**
     * The minimal and maximal values to renderer.
     */
    private final double minimum, maximum;

    /**
     * The scaled color space to test.
     */
    private final ScaledColorSpace colors;

    /**
     * Sets up common objects used for all tests.
     */
    public ScaledColorSpaceTest() {
        final Random random = TestUtilities.createRandomNumberGenerator();
        minimum = random.nextDouble()*25;
        maximum = random.nextDouble()*80 + minimum + 10;          // Shall be less than 256 for compliance with SAMPLE_TOLERANCE.
        colors  = new ScaledColorSpace(1, 0, minimum, maximum);
    }

    /**
     * Tests the color space.
     */
    @Test
    public void testColorSpace() {
        assertEquals(minimum, colors.getMinValue(0), SAMPLE_TOLERANCE);
        assertEquals(maximum, colors.getMaxValue(0), SAMPLE_TOLERANCE);

        final float[] array = new float[1];
        final double step = (maximum - minimum) / 256;
        for (double x=minimum; x<maximum; x+=step) {
            array[0] = (float) x;
            assertEquals(x, colors.fromRGB(colors.toRGB(array))[0], 2*SAMPLE_TOLERANCE);
        }
    }

    /**
     * Shows an image using the scaled color model.
     * The image appears only if {@link #viewEnabled} is {@code true}.
     */
    @Test
    public void view() {
        if (viewEnabled) {
            final int transparency = Transparency.OPAQUE;
            final int datatype     = DataBuffer.TYPE_FLOAT;
            final ColorModel model = new ComponentColorModel(colors, false, false, transparency, datatype);
            final WritableRaster data = model.createCompatibleWritableRaster(200, 200);
            image = new BufferedImage(model, data, false, null);
            final int width  = data.getWidth();
            final int height = data.getHeight();
            for (int x=width; --x>=0;) {
                for (int y=height; --y>=0;) {
                    double v = hypot(((double) x) / width - 0.5, ((double) y) / height - 0.5);
                    v = v*(maximum - minimum) + minimum;
                    data.setSample(x, y, 0, v);
                }
            }
            showCurrentImage("ScaledColorSpace");
        }
    }
}
