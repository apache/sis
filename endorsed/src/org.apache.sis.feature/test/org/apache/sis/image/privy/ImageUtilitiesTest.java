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

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;
import java.awt.image.SampleModel;
import java.awt.image.BandedSampleModel;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.resources.Vocabulary;
import static org.apache.sis.util.privy.Numerics.COMPARISON_THRESHOLD;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link ImageUtilities}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ImageUtilitiesTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public ImageUtilitiesTest() {
    }

    /**
     * Tests {@link ImageUtilities#getBounds(RenderedImage)} and {@link ImageUtilities#clipBounds(RenderedImage, Rectangle)}.
     */
    @Test
    public void testClipBounds() {
        final BufferedImage image = new BufferedImage(5, 4, BufferedImage.TYPE_BYTE_GRAY);
        final Rectangle bounds = ImageUtilities.getBounds(image);
        assertEquals(0, bounds.x);
        assertEquals(0, bounds.y);
        assertEquals(5, bounds.width);
        assertEquals(4, bounds.height);

        bounds.x      = -4;
        bounds.y      =  1;
        bounds.width  =  8;
        bounds.height =  7;
        ImageUtilities.clipBounds(image, bounds);
        assertEquals(0, bounds.x);
        assertEquals(1, bounds.y);
        assertEquals(4, bounds.width);
        assertEquals(3, bounds.height);
    }

    /**
     * Tests {@link ImageUtilities#getDataTypeName(SampleModel)}.
     */
    @Test
    public void testGetDataTypeName() {
        assertEquals("byte",   ImageUtilities.getDataTypeName(new BandedSampleModel(DataBuffer.TYPE_BYTE,   1, 1, 1)));
        assertEquals("short",  ImageUtilities.getDataTypeName(new BandedSampleModel(DataBuffer.TYPE_SHORT,  1, 1, 1)));
        assertEquals("ushort", ImageUtilities.getDataTypeName(new BandedSampleModel(DataBuffer.TYPE_USHORT, 1, 1, 1)));
        assertEquals("int",    ImageUtilities.getDataTypeName(new BandedSampleModel(DataBuffer.TYPE_INT,    1, 1, 1)));
        assertEquals("float",  ImageUtilities.getDataTypeName(new BandedSampleModel(DataBuffer.TYPE_FLOAT,  1, 1, 1)));
        assertEquals("double", ImageUtilities.getDataTypeName(new BandedSampleModel(DataBuffer.TYPE_DOUBLE, 1, 1, 1)));
    }

    /**
     * Verifies that {@link ImageUtilities#bandNames(ColorModel, SampleModel)} returns expected band names.
     *
     * @param  nde    expected number of data elements. This number categorizes the tests in this class.
     * @param  type   one of the {@link BufferedImage} {@code TYPE_*} constants.
     * @param  names  vocabulary keys of expected names.
     */
    private static void assertBandNamesEqual(final int nde, final int type, final short... names) {
        final BufferedImage image = new BufferedImage(1, 1, type);
        final ColorModel cm = image.getColorModel();
        assertEquals(nde, image.getSampleModel().getNumDataElements());
        assertArrayEquals(names, ImageUtilities.bandNames(cm, image.getSampleModel()));
        /*
         * The following is more for testing our understanding of the way BufferedImage works.
         * We want to verify that no matter which BufferedImage.TYPE_* constant we used, values
         * managed by the sample model are in RGBA order.
         */
        image.getRaster().setPixel(0, 0, new int[] {10, 20, 30, 40});       // Always RGBA order for this test.
        final Object data = image.getRaster().getDataElements(0, 0, null);
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
         * For that reason, the band order below is the same as in above test.
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

    /**
     * Tests {@link ImageUtilities#toNumberEnum(int)}.
     */
    @Test
    public void testToNumberEnum() {
        assertEquals(Numbers.BYTE,    ImageUtilities.toNumberEnum(DataBuffer.TYPE_BYTE));
        assertEquals(Numbers.SHORT,   ImageUtilities.toNumberEnum(DataBuffer.TYPE_SHORT));
        assertEquals(Numbers.SHORT,   ImageUtilities.toNumberEnum(DataBuffer.TYPE_USHORT));
        assertEquals(Numbers.INTEGER, ImageUtilities.toNumberEnum(DataBuffer.TYPE_INT));
        assertEquals(Numbers.FLOAT,   ImageUtilities.toNumberEnum(DataBuffer.TYPE_FLOAT));
        assertEquals(Numbers.DOUBLE,  ImageUtilities.toNumberEnum(DataBuffer.TYPE_DOUBLE));
        assertEquals(Numbers.OTHER,   ImageUtilities.toNumberEnum(DataBuffer.TYPE_UNDEFINED));
    }

    /**
     * Tests the {@link ImageUtilities#roundIfAlmostInteger(AffineTransform)} method.
     */
    @Test
    public void testRoundIfAlmostInteger() {
        final double tolerance = COMPARISON_THRESHOLD;
        final AffineTransform test = new AffineTransform(4, 0, 0, 4, -400, -1186);
        final AffineTransform copy = new AffineTransform(test);
        assertTrue(ImageUtilities.roundIfAlmostInteger(test));
        assertEquals(copy, test);       // Coefficients were already integers, so the transform should not have been modified.

        test.scale(1 + tolerance/8, 1 - tolerance/8);
        assertTrue(ImageUtilities.roundIfAlmostInteger(test));
        assertEquals(copy, test);       // Coefficients should have been rounded.

        test.scale(1 + tolerance*2, 1 - tolerance*2);
        assertFalse(ImageUtilities.roundIfAlmostInteger(test));
        assertNotEquals(copy, test);    // Change was larger than threshold, so the transform should not have been modified.
    }
}
