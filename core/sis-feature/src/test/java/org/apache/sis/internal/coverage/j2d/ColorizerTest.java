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
package org.apache.sis.internal.coverage.j2d;

import java.util.Arrays;
import java.util.Collection;
import java.util.AbstractMap.SimpleEntry;
import java.awt.Color;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Units;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link Colorizer}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
public final strictfp class ColorizerTest extends TestCase {
    /**
     * Tests the creation of an index color model using {@link Colorizer#Colorizer(Collection)}.
     *
     * @throws TransformException if a sample value can not be converted.
     */
    @Test
    public void testRangeAndColors() throws TransformException {
        final Colorizer colorizer = new Colorizer(Arrays.asList(
                new SimpleEntry<>(NumberRange.create(0, true,  0, true), new Color[] {Color.GRAY}),
                new SimpleEntry<>(NumberRange.create(1, true,  1, true), new Color[] {ColorModelFactory.TRANSPARENT}),
                new SimpleEntry<>(NumberRange.create(2, true, 15, true), new Color[] {Color.BLUE, Color.WHITE, Color.RED})));
        /*
         * No conversion of sample values should be necessary because the
         * above-given ranges already fit in a 4-bits IndexColormodel.
         */
        assertTrue("isIdentity", colorizer.getSampleToIndexValues().isIdentity());
        final IndexColorModel cm = (IndexColorModel) colorizer.createColorModel(DataBuffer.TYPE_BYTE, 1, 0);
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
        assertEquals("mapSize", expected.length, cm.getMapSize());
        assertEquals("transparentPixel", 1, cm.getTransparentPixel());
        for (int i=0; i<expected.length; i++) {
            assertEquals(expected[i], cm.getRGB(i));
        }
    }

    /**
     * Tests the creation of an index color model using {@link Colorizer#Colorizer(Function)}
     * and an initialization with a {@link SampleDimension}.
     *
     * @throws TransformException if a sample value can not be converted.
     */
    @Test
    public void testSampleDimension() throws TransformException {
        final SampleDimension sd = new SampleDimension.Builder()
                .addQualitative ("No data", Float.NaN)
                .addQuantitative("Low temperature", -5, 24, Units.CELSIUS)
                .addQuantitative("Hot temperature", 25, 40, Units.CELSIUS)
                .addQualitative ("Error", MathFunctions.toNanFloat(3))
                .setName("Temperature").build();

        final Colorizer colorizer = new Colorizer(Colorizer.GRAYSCALE);
        assertTrue("initialize", colorizer.initialize(null, sd));
        final IndexColorModel cm = (IndexColorModel) colorizer.compactColorModel(1, 0);     // Must be first.
        /*
         * Test conversion of a few sample values to packed values.
         */
        final MathTransform1D tr = colorizer.getSampleToIndexValues();
        assertFalse("isIdentity", tr.isIdentity());
        assertEquals(  0, tr.transform(Float.NaN), STRICT);
        assertEquals(  1, tr.transform(MathFunctions.toNanFloat(3)), STRICT);
        assertEquals(  2, tr.transform(-5), 1E-14);
        assertEquals(255, tr.transform(40), 1E-14);
        /*
         * Verifies a few values from the color map. We test about 1/16 of values.
         * The color map is a simple grayscale, except the two first colors which
         * are transparent.
         */
        assertEquals("mapSize", 256, cm.getMapSize());
        assertEquals("transparentPixel", 0, cm.getTransparentPixel());
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
