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

import java.awt.image.ColorModel;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link ImageUtilities}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class ImageUtilitiesTest extends TestCase {
    /**
     * Verifies that {@link ImageUtilities#bandNames(RenderedImage)} returns expected band names.
     *
     * @param  nde    expected number of data elements. This number categorizes the tests in this class.
     * @param  type   one of the {@link BufferedImage} {@code TYPE_*} constants.
     * @param  names  vocabulary keys of expected names.
     */
    private static void assertBandNamesEqual(final int nde, final int type, final short... names) {
        final BufferedImage image = new BufferedImage(1, 1, type);
        assertEquals("numDataElements", nde, image.getSampleModel().getNumDataElements());
        assertArrayEquals("bandNames", names, ImageUtilities.bandNames(image));
        /*
         * The following is more for testing our understanding of the way BufferedImage works.
         * We want to verify that no matter which BufferedImage.TYPE_* constant we used, values
         * managed by the sample model are in RGBA order.
         */
        image.getRaster().setPixel(0, 0, new int[] {10, 20, 30, 40});       // Always RGBA order for this test.
        final Object data = image.getRaster().getDataElements(0, 0, null);
        final ColorModel cm = image.getColorModel();
        for (final short k : names) {
            final int expected, actual;
            switch (k) {
                case Vocabulary.Keys.Gray:         continue;
                case Vocabulary.Keys.Red:          expected = 10; actual = cm.getRed  (data); break;
                case Vocabulary.Keys.Green:        expected = 20; actual = cm.getGreen(data); break;
                case Vocabulary.Keys.Blue:         expected = 30; actual = cm.getBlue (data); break;
                case Vocabulary.Keys.Transparency: expected = 40; actual = cm.getAlpha(data); break;
                case Vocabulary.Keys.ColorIndex:   continue;
                default: throw new AssertionError(k);
            }
            assertEquals(expected, actual);
        }
    }

    /**
     * Tests {@link ImageUtilities#bandNames(RenderedImage)} with {@link BufferedImage} instances
     * created from the {@code BufferedImage.TYPE_*} constants.
     */
    @Test
    public void testBandNameOfBufferedImages() {
        /*
         * Images having a single data element.
         */
        assertBandNamesEqual(1, BufferedImage.TYPE_BYTE_GRAY,    Vocabulary.Keys.Gray);
        assertBandNamesEqual(1, BufferedImage.TYPE_USHORT_GRAY,  Vocabulary.Keys.Gray);
        assertBandNamesEqual(1, BufferedImage.TYPE_BYTE_BINARY,  Vocabulary.Keys.ColorIndex);
        assertBandNamesEqual(1, BufferedImage.TYPE_BYTE_INDEXED, Vocabulary.Keys.ColorIndex);
        /*
         * Image having a pixel packed in a single data element. The red color uses high
         * order bits (00FF0000 mask) and the blue color uses low order bits (000000FF).
         */
        assertBandNamesEqual(1, BufferedImage.TYPE_INT_RGB,
                Vocabulary.Keys.Red,
                Vocabulary.Keys.Green,
                Vocabulary.Keys.Blue);
        /*
         * Same as above, but with alpha channel. The alpha value uses highest order bits
         * (FF000000) mask but despite that fact, that value is ordered last by SampleModel.
         */
        assertBandNamesEqual(1, BufferedImage.TYPE_INT_ARGB,
                Vocabulary.Keys.Red,
                Vocabulary.Keys.Green,
                Vocabulary.Keys.Blue,
                Vocabulary.Keys.Transparency);
        /*
         * Same as above but with sample values packed in reverse order. Note that while values
         * are packed in BGR order, the sample model is still providing the values in RGB order.
         * For that reason, the band order below is the same than in above test.
         */
        assertBandNamesEqual(1, BufferedImage.TYPE_INT_BGR,
                Vocabulary.Keys.Red,
                Vocabulary.Keys.Green,
                Vocabulary.Keys.Blue);
        /*
         * Image having pixel stored in many data elements. BufferedImage still use a single bank,
         * but values are stored in 3 consecutive bytes with blue first, then green, then red.
         * Despite this storage order, SampleModel exposes those data in red, green, blue order.
         */
        assertBandNamesEqual(3, BufferedImage.TYPE_3BYTE_BGR,
                Vocabulary.Keys.Red,
                Vocabulary.Keys.Green,
                Vocabulary.Keys.Blue);
        /*
         * Add an alpha channel, which appears first in sequence of bytes. Despite this internal
         * order, SampleModel still exposes the alpha channel as the last sample value.
         */
        assertBandNamesEqual(4, BufferedImage.TYPE_4BYTE_ABGR,
                Vocabulary.Keys.Red,
                Vocabulary.Keys.Green,
                Vocabulary.Keys.Blue,
                Vocabulary.Keys.Transparency);
    }
}
