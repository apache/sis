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
import java.awt.image.ComponentSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.WritableRaster;
import org.opengis.geometry.MismatchedDimensionException;
import org.apache.sis.coverage.SubspaceNotSpecifiedException;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Static;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.ArgumentChecks;
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
     * Wraps a sub-region of the given data buffer in a raster.
     * The raster width, raster height, pixel stride and scanline stride are inferred from the grid extents.
     * The sample model type is selected according the number of bands and the pixel stride.
     *
     * @param  buffer          buffer that contains the sample values.
     * @param  bankIndices     bank indices for each band, or {@code null} for 0, 1, 2, 3….
     * @param  bandOffsets     number of data elements from the first element of the bank to the first sample of the band, or {@code null} for all 0.
     * @param  source          extent of the data wrapped by the given buffer. May have any number of dimensions.
     * @param  target          extent of the subspace to wrap in a raster.
     * @param  startAtZero     whether to force the raster to start at (0,0) instead than the target extent low coordinates.
     * @return a raster built from given properties.
     * @throws ArithmeticException if a stride calculation overflows the 32 bits integer capacity.
     * @throws SubspaceNotSpecifiedException if this method can not infer a two-dimensional slice from {@code target}.
     */
    public static WritableRaster createRaster(final DataBuffer buffer, final int[] bankIndices, final int[] bandOffsets,
            final GridExtent source, final GridExtent target, final boolean startAtZero)
    {
        int dimension = target.getDimension();
        if (source.getDimension() != dimension) {
            throw new MismatchedDimensionException(Errors.format(
                    Errors.Keys.MismatchedDimension_3, "target", source.getDimension(), dimension));
        }
        final int[] dimensions = target.getSubspaceDimensions(2);
        int xd = dimensions[0];
        int yd = dimensions[1];
        final Point location;
        if (startAtZero) {
            location = null;
        } else {
            location = new Point(Math.toIntExact(target.getLow(xd)),
                                 Math.toIntExact(target.getLow(yd)));
        }
        final int width  = Math.toIntExact(target.getSize(xd));
        final int height = Math.toIntExact(target.getSize(yd));
        /*
         * After this point, xd and yd should be indices relative to source extent.
         * For now we keep them unchanged on the assumption that the two grid extents have the same dimensions.
         */
        long pixelStride = 1;
        for (int i=0; i<xd; i++) {
            pixelStride = Math.multiplyExact(pixelStride, target.getSize(i));
        }
        long scanlineStride = pixelStride;
        for (int i=xd; i<yd; i++) {
            scanlineStride = Math.multiplyExact(scanlineStride, target.getSize(i));
        }
        return createRaster(buffer, width, height,
                Math.toIntExact(pixelStride), Math.toIntExact(scanlineStride), bankIndices, bandOffsets, location);
    }

    /**
     * Wraps the given data buffer in a raster.
     * The sample model type is selected according the number of bands and the pixel stride.
     *
     * @param  buffer          buffer that contains the sample values.
     * @param  width           raster width in pixels.
     * @param  height          raster height in pixels.
     * @param  pixelStride     number of data elements between two samples for the same band on the same line.
     * @param  scanlineStride  number of data elements between a given sample and the corresponding sample in the same column of the next line.
     * @param  bankIndices     bank indices for each band, or {@code null} for 0, 1, 2, 3….
     * @param  bandOffsets     number of data elements from the first element of the bank to the first sample of the band, or {@code null} for all 0.
     * @param  location        the upper-left corner of the raster, or {@code null} for (0,0).
     * @return a raster built from given properties.
     *
     * @see WritableRaster#createInterleavedRaster(DataBuffer, int, int, int, int, int[], Point)
     * @see WritableRaster#createBandedRaster(DataBuffer, int, int, int, int[], int[], Point)
     */
    @SuppressWarnings("fallthrough")
    public static WritableRaster createRaster(final DataBuffer buffer,
            final int width, final int height, final int pixelStride, final int scanlineStride,
            int[] bankIndices, int[] bandOffsets, final Point location)
    {
        ArgumentChecks.ensureStrictlyPositive("width",          width);
        ArgumentChecks.ensureStrictlyPositive("height",         height);
        ArgumentChecks.ensureStrictlyPositive("pixelStride",    pixelStride);
        ArgumentChecks.ensureStrictlyPositive("scanlineStride", scanlineStride);
        if (bandOffsets == null) {
            bandOffsets = new int[buffer.getNumBanks()];
        }
        final int dataType = buffer.getDataType();
        /*
         * This SampleModel variable is a workaround for WritableRaster static methods not supporting all data types.
         * If 'dataType' is unsupported, then we create a SampleModel ourselves in the 'switch' statements below and
         * use it for creating a WritableRaster at the end of this method. This variable, together with the 'switch'
         * statements, may be removed in a future SIS version if all types become supported by the JDK.
         */
        @Workaround(library = "JDK", version = "10")
        final SampleModel model;
        if (buffer.getNumBanks() == 1 && (bankIndices == null || bankIndices[0] == 0)) {
            /*
             * Sample data are stored for all bands in a single bank of the DataBuffer.
             * Each sample of a pixel occupies one data element of the DataBuffer.
             * The number of bands is inferred from bandOffsets.length.
             */
            switch (dataType) {
                case DataBuffer.TYPE_BYTE:
                case DataBuffer.TYPE_USHORT: {
                    // 'scanlineStride' and 'pixelStride' really interchanged in that method signature.
                    return WritableRaster.createInterleavedRaster(buffer, width, height, scanlineStride, pixelStride, bandOffsets, location);
                }
                case DataBuffer.TYPE_INT: {
                    if (bandOffsets.length == 1) {
                        // From JDK javadoc: "To create a 1-band Raster of type TYPE_INT, use createPackedRaster()".
                        return WritableRaster.createPackedRaster(buffer, width, height, Integer.SIZE, location);
                    }
                    // else fallthrough.
                }
                default: {
                    model = new PixelInterleavedSampleModel(dataType, width, height, pixelStride, scanlineStride, bandOffsets);
                    break;
                }
            }
        } else {
            if (bankIndices == null) {
                bankIndices = ArraysExt.sequence(0, bandOffsets.length);
            }
            if (pixelStride == 1) {
                /*
                 * All pixels are consecutive (pixelStride = 1) but may be on many bands.
                 */
                switch (dataType) {
                    case DataBuffer.TYPE_BYTE:
                    case DataBuffer.TYPE_USHORT:
                    case DataBuffer.TYPE_INT: {
                        // This constructor supports only above-cited types.
                        return WritableRaster.createBandedRaster(buffer, width, width, scanlineStride, bankIndices, bandOffsets, location);
                    }
                    default: {
                        model = new BandedSampleModel(dataType, width, height, scanlineStride, bankIndices, bandOffsets);
                        break;
                    }
                }
            } else {
                model = new ComponentSampleModel(dataType, width, height, pixelStride, scanlineStride, bankIndices, bandOffsets);
            }
        }
        return WritableRaster.createWritableRaster(model, buffer, location);
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
