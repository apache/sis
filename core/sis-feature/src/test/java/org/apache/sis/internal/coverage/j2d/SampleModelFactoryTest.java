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

import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.BandedSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.MultiPixelPackedSampleModel;
import org.apache.sis.image.DataType;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertInstanceOf;


/**
 * Tests {@link SampleModelFactory}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class SampleModelFactoryTest extends TestCase {
    /**
     * Arbitrary width, height and number of bands for the sample models to create.
     * Size does not matter because we will not create data buffer.
     */
    private static final int WIDTH = 200, HEIGHT = 300, NUM_BANDS = 6;

    /**
     * Tests the creation and modification of a {@link BandedSampleModel}.
     */
    @Test
    public void testBanded() {
        final BandedSampleModel model = test(BandedSampleModel.class,
                new SampleModelFactory(DataType.FLOAT, WIDTH, HEIGHT, NUM_BANDS, Float.SIZE, true));

        assertArrayEquals("bankIndices", new int[] {1, 0, 2}, model.getBankIndices());
        assertArrayEquals("bandOffsets", new int[] {0, 0, 0}, model.getBandOffsets());
        assertEquals("pixelStride",        1, model.getPixelStride());
        assertEquals("scanlineStride", WIDTH, model.getScanlineStride());
        assertEquals("dataType", DataBuffer.TYPE_FLOAT, model.getDataType());
    }

    /**
     * Tests the creation and modification of a {@link PixelInterleavedSampleModel}.
     */
    @Test
    public void testPixelInterleaved() {
        final PixelInterleavedSampleModel model = test(PixelInterleavedSampleModel.class,
                new SampleModelFactory(DataType.BYTE, WIDTH, HEIGHT, NUM_BANDS, Byte.SIZE, false));

        assertArrayEquals("bankIndices", new int[] {0, 0, 0}, model.getBankIndices());
        assertArrayEquals("bandOffsets", new int[] {1, 0, 2}, model.getBandOffsets());
        assertEquals("pixelStride",          3, model.getPixelStride());
        assertEquals("scanlineStride", WIDTH*3, model.getScanlineStride());
        assertEquals("dataType", DataBuffer.TYPE_BYTE, model.getDataType());
    }

    /**
     * Tests the creation and modification of a {@link SinglePixelPackedSampleModel}.
     * Note that for this kind of sample model, the type changes from 32 bits to 16 bits integer
     * because the decrease in number of bands make possible to store a pixel in smaller integers.
     */
    @Test
    public void testSinglePixelPacked() {
        final SinglePixelPackedSampleModel model = test(SinglePixelPackedSampleModel.class,
                new SampleModelFactory(DataType.INT, WIDTH, HEIGHT, NUM_BANDS, 5, false));

        final int[] expected = {
            0b1111100000,           // Band 2 specified, 1 after compression.
            0b11111,                // Band 1 specified, 0 after compression.
            0b111110000000000       // Band 4 specified, 2 after compression.
        };
        assertArrayEquals("bitMasks", expected, model.getBitMasks());
        assertEquals("scanlineStride", WIDTH, model.getScanlineStride());
        assertEquals("dataType", DataBuffer.TYPE_USHORT, model.getDataType());
    }

    /**
     * Tests the creation and modification of a {@link MultiPixelPackedSampleModel}.
     * We can not test band sub-setting for this kind of sample model because it can
     * only have a single band.
     */
    @Test
    public void testPixelMultiPixelPacked() {
        final int bitsPerSample = 4;
        SampleModelFactory factory = new SampleModelFactory(DataType.INT, WIDTH, HEIGHT, 1, bitsPerSample, false);
        final MultiPixelPackedSampleModel model = (MultiPixelPackedSampleModel) factory.build();

        assertEquals("pixelBitStride", bitsPerSample, model.getPixelBitStride());
        assertEquals("scanlineStride", WIDTH / (Integer.SIZE / bitsPerSample), model.getScanlineStride());
        assertEquals("dataType", DataBuffer.TYPE_INT, model.getDataType());

        factory = new SampleModelFactory(model);
        assertEquals(model, factory.build());
    }

    /**
     * Builds a sample model using the given factory, tests basic properties, then applies a band subset.
     * The band subset is exactly: {2, 1, 4}.
     */
    private static <T extends SampleModel> T test(final Class<T> modelType, SampleModelFactory factory) {
        final SampleModel model = factory.build();
        assertEquals(WIDTH,     model.getWidth());
        assertEquals(HEIGHT,    model.getHeight());
        assertEquals(NUM_BANDS, model.getNumBands());
        assertInstanceOf("build", modelType, model);
        /*
         * Select a subset of the bands and verify again.
         * The subset is fixed by this method's contract.
         */
        final int[] bands = {2, 1, 4};
        factory.subsetAndCompress(bands);
        final SampleModel subset = factory.build();
        assertEquals(WIDTH,  subset.getWidth());
        assertEquals(HEIGHT, subset.getHeight());
        assertEquals(3,      subset.getNumBands());
        assertInstanceOf("subsetAndCompress", modelType, subset);
        /*
         * Repeat the same operations on a factory created using a sample
         * model as a template, and verify that we get the same results.
         */
        factory = new SampleModelFactory(model);
        assertEquals(model, factory.build());
        factory.subsetAndCompress(bands);
        assertEquals(subset, factory.build());
        return modelType.cast(subset);
    }
}
