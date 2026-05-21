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

import java.awt.Dimension;
import java.awt.image.SampleModel;
import java.util.Map;
import java.util.stream.IntStream;
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import org.opengis.referencing.operation.MathTransform;

// Test dependencies
import org.junit.jupiter.api.Test;

import static org.apache.sis.feature.Assertions.assertPixelsEqual;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.image.processing.isoline.IsolinesTest;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSingleton;


/**
 * Tests {@link ImageProcessor}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class ImageProcessorTest extends TestCase {
    /**
     * The processor to test.
     */
    private final ImageProcessor processor;

    /**
     * Creates a new test case.
     */
    public ImageProcessorTest() {
        processor = new ImageProcessor();
    }

    /**
     * Tests {@link ImageProcessor#aggregateBands(RenderedImage...)}.
     *
     * @see BandAggregateImageTest
     */
    @Test
    public void testBandAggregate() {
        final int width  = 3;
        final int height = 4;
        final BufferedImage im1 = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        final BufferedImage im2 = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        im1.getRaster().setSamples(0, 0, width, height, 0, IntStream.range(0, width*height).map(s -> s +  10).toArray());
        im2.getRaster().setSamples(0, 0, width, height, 0, IntStream.range(0, width*height).map(s -> s + 100).toArray());

        final Raster data = processor.aggregateBands(im1, im2).getData();
        assertEquals(new Rectangle(0, 0, width, height), data.getBounds());
        assertEquals(2, data.getNumBands());
        assertArrayEquals(
            new int[] {
                10, 100,  11, 101,  12, 102,
                13, 103,  14, 104,  15, 105,
                16, 106,  17, 107,  18, 108,
                19, 109,  20, 110,  21, 111
            },
            data.getPixels(0, 0, width, height, (int[]) null)
        );
    }

    /**
     * Tests {@link ImageProcessor#addUserProperties(RenderedImage, Map)}.
     */
    @Test
    public void testAddUserProperties() {
        final String key = "my-property";
        final String value = "my-value";
        final RenderedImage source = new BufferedImage(2, 2, BufferedImage.TYPE_BYTE_BINARY);
        final RenderedImage image  = processor.addUserProperties(source, Map.of(key, value));
        assertSame(BufferedImage.UndefinedProperty, source.getProperty(key));
        assertSame(BufferedImage.UndefinedProperty,  image.getProperty("another-property"));
        assertSame(value, image.getProperty(key));
        assertArrayEquals(new String[] {key}, image.getPropertyNames());
    }

    /**
     * Tests {@link ImageProcessor#isolines(RenderedImage, double[][], MathTransform)}.
     */
    @Test
    public void testIsolines() {
        final BufferedImage image = new BufferedImage(3, 3, BufferedImage.TYPE_BYTE_BINARY);
        image.getRaster().setSample(1, 1, 0, 1);
        boolean parallel = false;
        do {
            processor.setExecutionMode(parallel ? ImageProcessor.Mode.SEQUENTIAL : ImageProcessor.Mode.PARALLEL);
            final Map<Double,Shape> r = assertSingleton(processor.isolines(image, new double[][] {{0.5}}, null));
            assertEquals(0.5, assertSingleton(r.keySet()));
            IsolinesTest.verifyIsolineFromMultiCells(assertSingleton(r.values()));
        } while ((parallel = !parallel) == true);
    }

    /**
     * Verify that {@link ImageProcessor#reformat(RenderedImage, SampleModel) reformat} properly adapt tile size
     * according to given parameters.
     */
    @Test
    public void changeTileSize() {
        changeTileSize(12, 12, 4, 2);
        changeTileSize(64, 64, 32, 32);
        changeTileSize(50, 50, 5, 5);
    }

    private void changeTileSize(int sourceImageWidth, int sourceImageHeight, int targetTileWidth, int targetTileHeight) {
        // Fill source image
        final var image = new BufferedImage(sourceImageWidth, sourceImageHeight, BufferedImage.TYPE_BYTE_GRAY);
        final var canvas = image.getRaster();
        for (int y = 0 ; y < image.getHeight() ; y++) {
            for (int x = 0 ; x < image.getWidth() ; x++) {
                canvas.setSample(x, y, 0, x*y);
            }
        }

        // Prepare target image layout
        final var tileModel = image.getSampleModel().createCompatibleSampleModel(targetTileWidth, targetTileHeight);
        final var preferredTileSize = new Dimension(tileModel.getWidth(), tileModel.getHeight());
        processor.setImageLayout(new ImageLayout(tileModel, preferredTileSize, true, false, true, null));

        // Execute and verify twice: sequential then parallel
        boolean parallel = false;
        final var imageBounds = new Rectangle(0, 0, image.getWidth(), image.getHeight());
        do {
            processor.setExecutionMode(parallel ? ImageProcessor.Mode.SEQUENTIAL : ImageProcessor.Mode.PARALLEL);
            final RenderedImage reformatted = processor.reformat(image, null);
            assertPixelsEqual(image, imageBounds, reformatted, imageBounds);
            assertEquals(tileModel.getWidth(), reformatted.getTileWidth(), "Reformatted image tile width");
            assertEquals(tileModel.getHeight(), reformatted.getTileHeight(), "Reformatted image tile height");
        } while ((parallel = !parallel) == true);
    }
}
