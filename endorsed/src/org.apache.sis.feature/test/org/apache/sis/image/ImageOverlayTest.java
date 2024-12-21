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

import java.util.Hashtable;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ComponentColorModel;
import java.awt.image.BandedSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import org.apache.sis.coverage.privy.ImageUtilities;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.feature.Assertions.assertValuesEqual;


/**
 * Tests {@link ImageOverlay}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class ImageOverlayTest extends TestCase {
    /**
     * The image to use at the sources for the test.
     * Should not be modified.
     */
    private final BufferedImage[] sources;

    /**
     * Creates a new test case.
     */
    public ImageOverlayTest() {
        final var properties = new Hashtable<String,Object>();
        final var cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        sources = new BufferedImage[3];

        properties.put("ShouldBeIgnored", "Dummy");
        properties.put(PlanarImage.SAMPLE_RESOLUTIONS_KEY, new double[] {3, 6, 1});
        sources[0] = new BufferedImage(cm, data(7, 3, 100), false, properties);

        properties.put(PlanarImage.SAMPLE_RESOLUTIONS_KEY, new double[] {2, 5, 3});
        sources[2] = new BufferedImage(cm, data(3, 5, 200), false, properties);
    }

    /**
     * Creates a raster for a source image.
     */
    private static WritableRaster data(final int width, final int height, int value) {
        final var sm = new BandedSampleModel(DataBuffer.TYPE_BYTE, width, height, 1);
        final WritableRaster raster = WritableRaster.createWritableRaster(sm, null);
        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                raster.setSample(x, y, 0, value++);
            }
        }
        return raster;
    }

    /**
     * Tests an image created with the default argument values.
     */
    @Test
    public void testDefault() {
        final RenderedImage image = ImageOverlay.create(sources, null, null, null, true, false);
        assertEquals(2, image.getSources().size());
        assertEquals(7, image.getWidth());
        assertEquals(5, image.getHeight());
        assertEquals(7, image.getTileWidth());
        assertEquals(5, image.getTileHeight());
        assertEquals(1, image.getNumXTiles());
        assertEquals(1, image.getNumYTiles());
        assertEquals(new Rectangle(7, 5), ImageUtilities.getValidArea(image).getBounds());
        assertArrayEquals(new String[] {PlanarImage.SAMPLE_RESOLUTIONS_KEY}, image.getPropertyNames());
        assertArrayEquals(new double[] {2, 5, 1}, (double[]) image.getProperty(PlanarImage.SAMPLE_RESOLUTIONS_KEY));
        assertValuesEqual(image.getData(), 0, new int[][] {
            {100, 101, 102, 103, 104, 105, 106},
            {107, 108, 109, 110, 111, 112, 113},
            {114, 115, 116, 117, 118, 119, 120},
            {209, 210, 211,   0,   0,   0,   0},
            {212, 213, 214,   0,   0,   0,   0}
        });
    }

    /**
     * Tests with a subregion fully covered by the first image.
     * The code should return the first image directly.
     */
    @Test
    public void testSubRegion() {
        RenderedImage image = ImageOverlay.create(sources, new Rectangle(7, 3), null, null, true, false);
        assertSame(sources[0], image);
    }
}
