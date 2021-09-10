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
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import org.opengis.coverage.grid.SequenceType;

import static org.junit.Assert.*;


/**
 * Tests {@link BandedIterator} on floating point values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 */
public final strictfp class BandedIteratorTest extends PixelIteratorTest {
    /**
     * Creates a new test case.
     */
    public BandedIteratorTest() {
        super(DataBuffer.TYPE_FLOAT, null);
        useBandedSampleModel = true;
    }

    /**
     * Creates a {@code PixelIterator} for a sub-area of given raster.
     */
    @Override
    void createPixelIterator(final WritableRaster raster, final Rectangle subArea) {
        final int scanlineStride = PixelIterator.Builder.getScanlineStride(raster.getSampleModel());
        assertTrue(scanlineStride >= raster.getWidth());
        iterator = new BandedIterator(raster, isWritable ? raster : null, subArea, null, null, scanlineStride);
        assertEquals("getIterationOrder()", SequenceType.LINEAR, iterator.getIterationOrder().get());
        assertEquals("isWritable", isWritable, iterator.isWritable());
    }

    /**
     * Creates a {@code PixelIterator} for a sub-area of given image.
     */
    @Override
    void createPixelIterator(final WritableRenderedImage image, final Rectangle subArea) {
        final int scanlineStride = PixelIterator.Builder.getScanlineStride(image.getSampleModel());
        assertTrue(scanlineStride >= image.getTileWidth());
        iterator = new BandedIterator(image, isWritable ? image : null, subArea, null, null, scanlineStride);
        assertEquals("isWritable", isWritable, iterator.isWritable());
    }

    /**
     * Creates a {@code PixelIterator} for a window in the given image.
     * The iterator shall be assigned to the {@link #iterator} field.
     */
    @Override
    void createWindowIterator(final WritableRenderedImage image, final Dimension window) {
        final int scanlineStride = PixelIterator.Builder.getScanlineStride(image.getSampleModel());
        assertTrue(scanlineStride >= image.getTileWidth());
        iterator = new BandedIterator(image, isWritable ? image : null, null, window, null, scanlineStride);
        assertEquals("isWritable", isWritable, iterator.isWritable());
    }
}
