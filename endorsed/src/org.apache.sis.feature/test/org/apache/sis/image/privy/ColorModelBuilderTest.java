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

import java.util.List;
import java.util.AbstractMap.SimpleEntry;
import java.awt.Color;
import java.awt.image.IndexColorModel;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.image.DataType;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Units;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link ColorScaleBuilder}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ColorModelBuilderTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public ColorModelBuilderTest() {
    }

    /**
     * Tests the creation of an index color model using explicit range of sample values.
     *
     * @throws TransformException if a sample value cannot be converted.
     */
    @Test
    public void testRangeAndColors() throws TransformException {
        final var colorizer = new ColorScaleBuilder(List.of(
                new SimpleEntry<>(NumberRange.create(0, true,  0, true), new Color[] {Color.GRAY}),
                new SimpleEntry<>(NumberRange.create(1, true,  1, true), new Color[] {ColorModelFactory.TRANSPARENT}),
                new SimpleEntry<>(NumberRange.create(2, true, 15, true), new Color[] {Color.BLUE, Color.WHITE, Color.RED})), null);
        /*
         * No conversion of sample values should be necessary because the
         * above-given ranges already fit in a 4-bits IndexColormodel.
         */
        assertTrue(colorizer.getSampleToIndexValues().isIdentity());
        final var cm = (IndexColorModel) colorizer.createColorModel(DataType.BYTE, 1, 0);
        final int[] expected = {
            0xFF808080,     // Color.GRAY
            0x00000000,     // ColorModelFactory.TRANSPARENT
            0xFF0000FF,     // Color.BLUE
            0xFF2727FF,
            0xFF4E4EFF,
            0xFF7676FF,
            0xFF9D9DFF,
            0xFFC4C4FF,
            0xFFEBEBFF,
            0xFFFFEBEB,
            0xFFFFC4C4,
            0xFFFF9D9D,
            0xFFFF7676,
            0xFFFF4E4E,
            0xFFFF2727,
            0xFFFF0000      // Color.RED
        };
        assertEquals(expected.length, cm.getMapSize());
        assertEquals(1, cm.getTransparentPixel());
        for (int i=0; i<expected.length; i++) {
            assertEquals(expected[i], cm.getRGB(i));
        }
    }

    /**
     * Tests the creation of an index color model using sample dimensions.
     *
     * @throws TransformException if a sample value cannot be converted.
     */
    @Test
    public void testSampleDimension() throws TransformException {
        final SampleDimension sd = new SampleDimension.Builder()
                .addQualitative ("No data", Float.NaN)
                .addQuantitative("Low temperature", -5, 24, Units.CELSIUS)
                .addQuantitative("Hot temperature", 25, 40, Units.CELSIUS)
                .addQualitative ("Error", MathFunctions.toNanFloat(3))
                .setName("Temperature").build();

        final var colorizer = new ColorScaleBuilder(ColorScaleBuilder.GRAYSCALE, null, true);
        assertTrue(colorizer.initialize(null, sd));
        final var dt = DataType.forDataBufferType(ColorScaleBuilder.TYPE_COMPACT);
        final var cm = (IndexColorModel) colorizer.createColorModel(dt, 1, 0);      // Must be first.
        /*
         * Test conversion of a few sample values to packed values.
         */
        final MathTransform1D tr = colorizer.getSampleToIndexValues();
        assertFalse(tr.isIdentity());
        assertEquals(  0, tr.transform(Float.NaN));
        assertEquals(  1, tr.transform(MathFunctions.toNanFloat(3)));
        assertEquals(  2, tr.transform(-5), 1E-14);
        assertEquals(255, tr.transform(40), 1E-14);
        /*
         * Verifies a few values from the color map. We test about 1/16 of values.
         * The color map is a simple grayscale, except the two first colors which
         * are transparent.
         */
        assertEquals(256, cm.getMapSize());
        assertEquals(0, cm.getTransparentPixel());
        final int[] expected = {
              0, 0x00000000,
              1, 0x00000000,
              2, 0xFF000000,
             16, 0xFF161616,
             32, 0xFF2E2E2E,
             48, 0xFF474747,
             64, 0xFF5F5F5F,
             80, 0xFF787878,
             96, 0xFF909090,
            112, 0xFFA9A9A9,
            128, 0xFFC2C2C2,
            144, 0xFFDADADA,
            160, 0xFFF3F3F3,
            176, 0xFF151515,
            192, 0xFF444444,
            208, 0xFF747474,
            224, 0xFFA3A3A3,
            240, 0xFFD3D3D3,
            255, 0xFFFFFFFF
        };
        for (int k=0; k<expected.length;) {
            final int i = expected[k++];
            final int e = expected[k++];
            assertEquals(e, cm.getRGB(i));
        }
    }
}
