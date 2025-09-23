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
import java.util.Hashtable;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import org.apache.sis.image.internal.shared.ColorModelFactory;
import org.apache.sis.image.internal.shared.ImageUtilities;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import static org.apache.sis.feature.Assertions.assertValuesEqual;


/**
 * Tests {@link BandSelectImage}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class BandSelectImageTest extends TestCase {
    /**
     * Arbitrary size for the test image.
     */
    private static final int WIDTH = 3, HEIGHT = 4;

    /**
     * Random number generator used for the test.
     */
    private Random random;

    /**
     * The source image as an instance of custom implementation.
     */
    private TiledImageMock image;

    /**
     * The source image as a {@link BufferedImage} instance.
     */
    private BufferedImage bufferedImage;

    /**
     * Creates a new test case.
     */
    public BandSelectImageTest() {
    }

    /**
     * Creates a dummy image for testing purpose. This image will contain the given number of bands.
     * One band contains deterministic values and all other bands contain random values.
     * The image is assigned to {@link #bufferedImage} and {@link #image} fields.
     *
     * @param  numBands     number of bands in the image to create.
     * @param  checkedBand  band in which to put deterministic values.
     * @param  icm          {@code true} for using index color model, or {@code false} for scaled color model.
     */
    private void createImage(final int numBands, final int checkedBand, final boolean icm) {
        image = new TiledImageMock(DataBuffer.TYPE_BYTE, numBands, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT, 0, 0, false);
        image.initializeAllTiles(checkedBand);
        random = TestUtilities.createRandomNumberGenerator();
        for (int i=0; i<numBands; i++) {
            if (i != checkedBand) {
                image.setRandomValues(i, random, 100);
            }
        }
        image.validate();
        final ColorModel cm;
        if (icm) {
            final int[] ARGB = new int[256];
            ColorModelFactory.expand(new int[] {0xFF000000, 0xFFFFFFFF}, ARGB, 0, ARGB.length);
            cm = ColorModelFactory.createIndexColorModel(null, 0, numBands, checkedBand, ARGB, true, -1);
        } else {
            cm = ColorModelFactory.createGrayScale(DataBuffer.TYPE_BYTE, numBands, checkedBand, Byte.MIN_VALUE, Byte.MAX_VALUE);
        }
        @SuppressWarnings("UseOfObsoleteCollectionType")
        final var properties = new Hashtable<String,Object>();
        final var resolutions = new double[numBands];
        for (int i=0; i<numBands; i++) resolutions[i] = resolution(i);
        properties.put(PlanarImage.SAMPLE_RESOLUTIONS_KEY, resolutions);
        bufferedImage = new BufferedImage(cm, (WritableRaster) image.getTile(0, 0), false, properties);
    }

    /**
     * The expected sample values in the determinist band initialized by {@link #createImage(int, int, boolean)}.
     */
    private static int[][] expectedSampleValues() {
        return new int[][] {
            {100, 101, 102},
            {110, 111, 112},
            {120, 121, 122},
            {130, 131, 132}
        };
    }

    /**
     * Computes a dummy resolution for the given band.
     */
    private static double resolution(final int band) {
        return (band+1) * 10;
    }

    /**
     * Verifies that the given image contains the data expected for the {@code checkedBand}.
     *
     * @param  image        the image resulting from a call to {@link ImageProcessor#selectBands(RenderedImage, int...)}.
     * @param  numBands     expected number of bands.
     * @param  checkedBand  band in which to check values.
     */
    private static void verifySamples(final RenderedImage image, final int numBands, final int checkedBand) {
        final Raster tile = image.getTile(0,0);
        assertEquals(numBands, tile.getNumBands());
        assertEquals(numBands, ImageUtilities.getNumBands(image));
        assertEquals(image.getSampleModel(), tile.getSampleModel());
        assertValuesEqual(tile, checkedBand, expectedSampleValues());
    }

    /**
     * Verifies the property values.
     *
     * @param  image  the image resulting from a call to {@link ImageProcessor#selectBands(RenderedImage, int...)}.
     * @param  bands  selected bands.
     */
    private static void verifyProperties(final RenderedImage image, final int... bands) {
        assertArrayEquals(new String[] {PlanarImage.SAMPLE_RESOLUTIONS_KEY}, image.getPropertyNames());
        final double[] resolutions = (double[]) image.getProperty(PlanarImage.SAMPLE_RESOLUTIONS_KEY);
        final double[] expected = new double[bands.length];
        for (int i=0; i<bands.length; i++) {
            expected[i] = resolution(bands[i]);
        }
        assertArrayEquals(expected, resolutions);
    }

    /**
     * Tests bands selection in an image using index color model.
     */
    @Test
    public void testIndexColorModel() {
        createImage(3, 1, true);
        final ImageProcessor processor = new ImageProcessor();
        assertSame(image,          processor.selectBands(image,         0, 1, 2));
        assertSame(bufferedImage,  processor.selectBands(bufferedImage, 0, 1, 2));

        RenderedImage test = processor.selectBands(image, 1);
        verifySamples(test, 1, 0);

        test = processor.selectBands(image, 0, 1);
        verifySamples(test, 2, 1);

        test = processor.selectBands(bufferedImage, 1);
        assertInstanceOf(BufferedImage.class, test);
        assertEquals(IndexColorModel.class, test.getColorModel().getClass());
        verifySamples(test, 1, 0);
        verifyProperties(test, 1);

        test = processor.selectBands(bufferedImage, 0, 1);
        assertInstanceOf(BufferedImage.class, test);
        assertInstanceOf(IndexColorModel.class, test.getColorModel());
        verifySamples(test, 2, 1);
        verifyProperties(test, 0, 1);
    }

    /**
     * Tests bands selection in an image using scaled color model.
     */
    @Test
    public void testScaledColorModel() {
        createImage(4, 2, true);
        final ImageProcessor processor = new ImageProcessor();
        assertSame(image,         processor.selectBands(image,         0, 1, 2, 3));
        assertSame(bufferedImage, processor.selectBands(bufferedImage, 0, 1, 2, 3));

        RenderedImage test = processor.selectBands(image, 3, 0, 2);
        verifySamples(test, 3, 2);

        test = processor.selectBands(bufferedImage, 3, 0, 2);
        assertInstanceOf(BufferedImage.class, test);
        assertNotNull(test.getColorModel());
        verifySamples(test, 3, 2);
        verifyProperties(test, 3, 0, 2);
    }

    /**
     * Tests write operation.
     */
    @Test
    public void testWritable() {
        createImage(2, 1, true);
        final ImageProcessor processor = new ImageProcessor();
        RenderedImage test = processor.selectBands(image, 1);
        final int[][] expected = expectedSampleValues();
        final Raster data = test.getData();
        assertValuesEqual(data, 0, expected);
        /*
         * Above code where read operations for making sure that we initialized the test correctly.
         * Code below is the actual test for write operations.
         */
        final WritableRenderedImage writable = (WritableRenderedImage) test;
        final int tileX = writable.getMinTileX();
        final int tileY = writable.getMinTileY();
        final WritableRaster tile = writable.getWritableTile(tileX, tileY);
        for (int i=0; i<3; i++) {
            final int x = random.nextInt(tile.getWidth());
            final int y = random.nextInt(tile.getHeight());
            final int s = random.nextInt(10);
            tile.setSample(x, y, 0, s);
            expected[y][x] = s;
        }
        writable.releaseWritableTile(tileX, tileY);
        assertValuesEqual(writable.getData(), 0, expected);
        /*
         * Try to restore orginal values.
         */
        writable.setData(data);
        assertValuesEqual(writable.getData(), 0, expectedSampleValues());
    }

    /**
     * Tests a band select on an image which is already a band select.
     * The nested operations should be simplified to a single band select operation.
     */
    @Test
    public void testNestedBandSelect() {
        createImage(3, 2, true);
        final ImageProcessor processor = new ImageProcessor();
        RenderedImage test = processor.selectBands(image, 1, 2);
        test = processor.selectBands(test, 1);
        assertSame(image, ((BandSelectImage) test).getSource());
        assertValuesEqual(test.getData(), 0, expectedSampleValues());
    }
}
