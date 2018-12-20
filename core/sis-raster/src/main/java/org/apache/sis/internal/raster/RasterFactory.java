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
package org.apache.sis.internal.raster;

import java.nio.Buffer;
import java.nio.ReadOnlyBufferException;
import java.awt.Point;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferDouble;
import java.awt.image.SampleModel;
import java.awt.image.BandedSampleModel;
import java.awt.image.WritableRaster;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Static;
import org.apache.sis.util.resources.Errors;


/**
 * Creates rasters from given properties.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class RasterFactory extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private RasterFactory() {
    }

    /**
     * Wraps the given data buffer in a raster.
     *
     * @param  buffer          buffer that contains the sample values.
     * @param  width           raster width in pixels.
     * @param  height          raster height in pixels.
     * @param  scanlineStride  line stride of raster data.
     * @param  bankIndices     bank indices for each band, or {@code null} for 0, 1, 2, 3….
     * @param  bandOffsets     offsets of all bands, or {@code null} for all 0.
     * @param  location        the upper-left corner of the raster, or {@code null} for (0,0).
     * @return a raster built from given properties.
     *
     * @see WritableRaster#createBandedRaster(DataBuffer, int, int, int, int[], int[], Point)
     */
    public static WritableRaster createBandedRaster​(final DataBuffer buffer,
            final int width, final int height, final int scanlineStride,
            int[] bankIndices, int[] bandOffsets, final Point location)
    {
        if (bankIndices == null) {
            bankIndices = ArraysExt.sequence(0, buffer.getNumBanks());
        }
        if (bandOffsets == null) {
            bandOffsets = new int[bankIndices.length];
        }
        final int dataType = buffer.getDataType();
        switch (dataType) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_INT: {
                // This constructor supports only above-cited types.
                return WritableRaster.createBandedRaster(buffer, width, width, scanlineStride, bankIndices, bandOffsets, location);
            }
            default: {
                SampleModel model = new BandedSampleModel(dataType, width, height, scanlineStride, bankIndices, bandOffsets);
                return WritableRaster.createWritableRaster(model, buffer, location);
            }
        }
    }

    /**
     * Wraps the backing arrays of given NIO buffers into Java2D buffers.
     * This method wraps the underlying array of primitive types; data are not copied.
     * For each buffer, the data starts at {@linkplain Buffer#position() buffer position}
     * and ends at the position + {@linkplain Buffer#remaining() remaining}.
     *
     * @param  dataType  type of buffer to create as one of {@link DataBuffer} constants.
     * @param  data      the data, one for each band.
     * @return buffer of the given type, or {@code null} if {@code dataType} is unrecognized.
     * @throws UnsupportedOperationException if a buffer is not backed by an accessible array.
     * @throws ReadOnlyBufferException if a buffer is backed by an array but is read-only.
     * @throws ArrayStoreException if the type of a backing array is not {@code dataType}.
     * @throws ArithmeticException if the position of a buffer is too high.
     * @throws IllegalArgumentException if buffers do not have the same amount of remaining values.
     */
    public static DataBuffer wrap(final int dataType, final Buffer... data) {
        final int numBands = data.length;
        final Object[] arrays;
        switch (dataType) {
            case DataBuffer.TYPE_USHORT: // fall through
            case DataBuffer.TYPE_SHORT:  arrays = new short [numBands][]; break;
            case DataBuffer.TYPE_INT:    arrays = new int   [numBands][]; break;
            case DataBuffer.TYPE_BYTE:   arrays = new byte  [numBands][]; break;
            case DataBuffer.TYPE_FLOAT:  arrays = new float [numBands][]; break;
            case DataBuffer.TYPE_DOUBLE: arrays = new double[numBands][]; break;
            default: return null;
        }
        final int[] offsets = new int[numBands];
        int length = 0;
        for (int i=0; i<numBands; i++) {
            final Buffer buffer = data[i];
            arrays [i] = buffer.array();
            offsets[i] = Math.addExact(buffer.arrayOffset(), buffer.position());
            final int r = buffer.remaining();
            if (i == 0) length = r;
            else if (length != r) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.MismatchedArrayLengths));
            }
        }
        switch (dataType) {
            case DataBuffer.TYPE_BYTE:   return new DataBufferByte  (  (byte[][]) arrays, length, offsets);
            case DataBuffer.TYPE_SHORT:  return new DataBufferShort ( (short[][]) arrays, length, offsets);
            case DataBuffer.TYPE_USHORT: return new DataBufferUShort( (short[][]) arrays, length, offsets);
            case DataBuffer.TYPE_INT:    return new DataBufferInt   (   (int[][]) arrays, length, offsets);
            case DataBuffer.TYPE_FLOAT:  return new DataBufferFloat ( (float[][]) arrays, length, offsets);
            case DataBuffer.TYPE_DOUBLE: return new DataBufferDouble((double[][]) arrays, length, offsets);
            default: return null;
        }
    }
}
