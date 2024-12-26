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

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.SampleModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferDouble;
import java.awt.image.RenderedImage;
import java.awt.image.RasterFormatException;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.coverage.privy.ImageUtilities;
import org.apache.sis.system.Configuration;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.util.privy.Numerics;


/**
 * Strategy for reading and writing data between two rasters, with a computation between them.
 * Some strategies are to write directly in the destination raster, other strategies are to use
 * an intermediate buffer. This class has the following constraints:
 *
 * <ul>
 *   <li>Source values cannot be modified. Calculations must be done either directly in the
 *       target raster, or in a temporary buffer.</li>
 *   <li>Direct access to the {@link DataBuffer} arrays may disable video card acceleration.
 *       This class assumes that it is acceptable for {@code float} and {@code double} types,
 *       and to be avoided for integer types.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class Transferer {
    /**
     * Approximate size of the buffer to use for copying data from/to a raster, in bits.
     * The actual buffer size may be smaller or larger, depending on the actual tile size.
     * This value does not need to be very large.
     *
     * @see #prepareTransferRegion(Rectangle, int)
     */
    @Configuration
    private static final int BUFFER_SIZE = 32 * ImageLayout.DEFAULT_TILE_SIZE * Byte.SIZE;

    /**
     * The image tile from which to read sample values.
     */
    protected final Raster source;

    /**
     * The image tile where to write sample values after processing.
     */
    protected final WritableRaster target;

    /**
     * Coordinates of the region to read and write. This class assumes that
     * {@link #source} and {@link #target} share the same coordinate system.
     */
    protected final Rectangle region;

    /**
     * The band to read and write. This class assumes that {@link #source} and
     * {@link #target} have the same number of bands and bands in same order.
     */
    protected int band;

    /**
     * Creates a new instance for transferring data between the two specified rasters.
     * It is caller responsibility to ensure that {@code aoi} is contained in the bounds
     * of both {@code source} and {@code target} rasters.
     *
     * @param  source  image tile from which to read sample values.
     * @param  target  image tile where to write sample values after processing.
     * @param  aoi     the area of interest, often {@code target.getBounds()}.
     */
    protected Transferer(final Raster source, final WritableRaster target, final Rectangle aoi) {
        this.source = source;
        this.target = target;
        this.region = aoi;
    }

    /**
     * Sets {@code region.height} to the height of the buffer where values are stored
     * during data transfer and where {@link MathTransform1D} operations are applied.
     * If this {@code Transferer} does not use an intermediate buffer (i.e. if it
     * copies values directly in the target buffer and processes them in-place),
     * then this method should leave {@link #region} unchanged.
     *
     * <p>The default implementation does nothing. This is the most conservative approach
     * since it does not require {@code Transferer} to split data processing in strips,
     * at the cost of more memory consumption if this {@code Transferer} does not write
     * data directly in the target tile.</p>
     *
     * @return {@code region.y + region.height}.
     *
     * @see #prepareTransferRegion(Rectangle, int)
     */
    int prepareTransferRegion() {
        return Math.addExact(region.y, region.height);
    }

    /**
     * Suggests the height of a transfer region for a tile of the given size. The given region should be
     * contained inside {@link Raster#getBounds()}. This method modifies {@link Rectangle#height} in-place.
     * The {@link Rectangle#width} value is never modified, so caller can iterate on all raster rows without
     * the need to check if the row is incomplete.
     *
     * @param  bounds    on input, the region of interest. On output, the suggested transfer region bounds.
     * @param  dataType  one of {@link DataBuffer} constant. It is okay if an unknown constant is used since
     *                   this information is used only as a hint for adjusting the {@link #BUFFER_SIZE} value.
     * @return the maximum <var>y</var> value plus 1. This can be used as stop condition for iterating over rows.
     * @throws ArithmeticException if the maximum <var>y</var> value overflows 32 bits integer capacity.
     * @throws RasterFormatException if the given bounds is empty.
     */
    static int prepareTransferRegion(final Rectangle bounds, final int dataType) {
        if (bounds.isEmpty()) {
            throw new RasterFormatException(Resources.format(Resources.Keys.EmptyTileOrImageRegion));
        }
        final int afterLastRow = Math.addExact(bounds.y, bounds.height);
        int size;
        try {
            size = DataBuffer.getDataTypeSize(dataType);
        } catch (IllegalArgumentException e) {
            size = Short.SIZE;  // Arbitrary value is okay because this is only a hint for choosing a buffer size.
        }
        bounds.height = Math.max(1, Math.min(BUFFER_SIZE / (size * bounds.width), bounds.height));
        return afterLastRow;
    }

    /**
     * Computes all sample values from the source tile and writes the result in the target tile.
     * This method invokes {@link #computeStrip(MathTransform1D)} repetitively for sub-regions of the tile.
     *
     * @param  converters  the converters to apply on each band.
     * @throws TransformException if an error occurred during calculation.
     */
    public final void compute(final MathTransform1D[] converters) throws TransformException {
        assert source.getBounds().contains(region) : region;
        assert target.getBounds().contains(region) : region;
        final int afterLastRow = prepareTransferRegion();
        final int ymin         = region.y;
        final int maxHeight    = region.height;             // Computed by `prepareTransferRegion()`.
        for (band=0; band < converters.length; band++) {
            final MathTransform1D converter = converters[band];
            region.y = ymin;
            do {
                region.height = Math.min(maxHeight, afterLastRow - region.y);
                computeStrip(converter);
            } while ((region.y += region.height) < afterLastRow);
        }
    }

    /**
     * Reads sample values from the {@linkplain #source} tile, applies the given operation for current
     * {@linkplain #region} and {@linkplain #band}, then writes results in the {@linkplain #target} tile.
     * The {@linkplain #region} and the {@linkplain #band} number must be set before to invoke this method.
     *
     * @param  converter  the operation to apply on sample values in current region and current band number.
     * @throws TransformException if an error occurred during calculation.
     */
    abstract void computeStrip(final MathTransform1D converter) throws TransformException;

    /**
     * Returns the number of elements in {@link #region}. This is a helper method for subclass implementations.
     */
    final int length() {
        return Math.multiplyExact(region.width, region.height);
    }




    /**
     * Read {@code double} values from the source and write {@code double} values directly in the target raster,
     * without intermediate buffer. This strategy is possible only when the target raster uses the {@code double}
     * type for storing sample values. This operation is executed in one step, without subdivisions in strips.
     */
    private static final class DoubleToDirect extends Transferer {
        /** Data buffer of target raster. */
        private final DataBufferDouble buffer;

        /** Creates a new instance for transferring data between the two specified rasters. */
        DoubleToDirect(final Raster source, final WritableRaster target, final Rectangle aoi) {
            super(source, target, aoi);
            buffer = (DataBufferDouble) target.getDataBuffer();
        }

        /** Copies source values directly in the target, then applies the conversion in-place. */
        @Override void computeStrip(final MathTransform1D converter) throws TransformException {
            double[] data = buffer.getData(band);
            data = source.getSamples(region.x, region.y, region.width, region.height, band, data);
            converter.transform(data, 0, data, 0, length());
        }
    }




    /**
     * Read {@code float} values from the source and write {@code float} values directly in the target raster,
     * without intermediate buffer. This strategy is possible only when the target raster uses the {@code float}
     * type for storing sample values. This operation is executed in one step, without subdivisions in strips.
     */
    private static final class FloatToDirect extends Transferer {
        /** Data buffer of target raster. */
        private final DataBufferFloat buffer;

        /** Creates a new instance for transferring data between the two specified rasters. */
        FloatToDirect(final Raster source, final WritableRaster target, final Rectangle aoi) {
            super(source, target, aoi);
            buffer = (DataBufferFloat) target.getDataBuffer();
        }

        /** Copies source values directly in the target, then applies the conversion in-place. */
        @Override void computeStrip(final MathTransform1D converter) throws TransformException {
            float[] data = buffer.getData(band);
            data = source.getSamples(region.x, region.y, region.width, region.height, band, data);
            converter.transform(data, 0, data, 0, length());
        }
    }




    /*
     * TODO: provide an IntegerToDirect class which would use the SampleModel.getSamples(…, int[]) method
     * instead of SampleModel.getSamples(…, float[]). The reason is that the former method is optimized
     * in Java2D while the latter is not. We would not provide that optimisation for double target type
     * because it is less commonly used.
     */




    /**
     * Read {@code double} values from the source raster and write {@code double} values in a temporary buffer.
     * Note that reading and writing data has {@code double} does not imply that raster data type must be that type.
     * The temporary buffer will be written in the target raster as a separated step. The use of a temporary buffer
     * is needed when the target raster does not use the {@code double} type, or does not use a layout that allows
     * us to write directly in the raster array.
     *
     * <h2>Implementation note</h2>
     * Having a source raster with {@code double} data type does not remove the need to use a temporary buffer,
     * because we cannot modify the source data. We still need to allocate a temporary array for collecting the
     * operation results before final writing in the target array.
     */
    private static final class DoubleToDouble extends Transferer {
        /** Temporary buffer where to copy data and apply operation. */
        private double[] buffer;

        /** Creates a new instance for transferring data between the two specified rasters. */
        DoubleToDouble(final Raster source, final WritableRaster target, final Rectangle aoi) {
            super(source, target, aoi);
        }

        /** Subdivides the region to process in smaller strips, for smaller {@linkplain #buffer}. */
        @Override int prepareTransferRegion() {
            return prepareTransferRegion(region, DataBuffer.TYPE_DOUBLE);
        }

        /** Copies source values in temporary buffer, applies conversion then copies to target. */
        @Override void computeStrip(final MathTransform1D converter) throws TransformException {
            buffer = source.getSamples(region.x, region.y, region.width, region.height, band, buffer);
            converter.transform(buffer, 0, buffer, 0, length());
            target.setSamples(region.x, region.y, region.width, region.height, band, buffer);
        }
    }




    /**
     * Read {@code float} values from the source raster and write {@code float} values in a temporary buffer.
     * Note that reading and writing data has {@code float} does not imply that raster data type must be that type.
     * The temporary buffer will be written in the target raster as a separated step. The use of a temporary buffer
     * is needed when the target raster does not use the {@code float} type, or does not use a layout that allows
     * us to write directly in the raster array.
     */
    private static final class FloatToFloat extends Transferer {
        /** Temporary buffer where to copy data and apply operation. */
        private float[] buffer;

        /** Creates a new instance for transferring data between the two specified rasters. */
        FloatToFloat(final Raster source, final WritableRaster target, final Rectangle aoi) {
            super(source, target, aoi);
        }

        /** Subdivides the region to process in smaller strips, for smaller {@linkplain #buffer}. */
        @Override int prepareTransferRegion() {
            return prepareTransferRegion(region, DataBuffer.TYPE_FLOAT);
        }

        /** Copies source values in temporary buffer, applies conversion then copies to target. */
        @Override void computeStrip(final MathTransform1D converter) throws TransformException {
            buffer = source.getSamples(region.x, region.y, region.width, region.height, band, buffer);
            converter.transform(buffer, 0, buffer, 0, length());
            target.setSamples(region.x, region.y, region.width, region.height, band, buffer);
        }
    };




    /**
     * Read {@code double} values from the source raster and write {@code int} values in a temporary buffer.
     * The floating point values will be rounded and clamped to the range of the integer type.
     * Note that we do not provide any direct version for integer types because direct access
     * to {@link DataBuffer} array disable Java2D acceleration on video card.
     */
    private static class DoubleToInteger extends Transferer {
        /** Temporary buffer where to copy data and apply operation. */
        protected double[] buffer;

        /** Temporary buffer where to round data before transfer to target raster. */
        protected int[] transfer;

        /** Creates a new instance for transferring data between the two specified rasters. */
        DoubleToInteger(final Raster source, final WritableRaster target, final Rectangle aoi) {
            super(source, target, aoi);
        }

        /** Subdivides the region to process in smaller strips, for smaller {@linkplain #buffer}. */
        @Override final int prepareTransferRegion() {
            return prepareTransferRegion(region, DataBuffer.TYPE_DOUBLE);
        }

        /** Copies source values in temporary buffer, applies conversion then copies to target. */
        @Override final void computeStrip(final MathTransform1D converter) throws TransformException {
            final int length = length();
            buffer = source.getSamples(region.x, region.y, region.width, region.height, band, buffer);
            converter.transform(buffer, 0, buffer, 0, length);
            if (transfer == null) transfer = new int[length];
            clamp(length);
            target.setSamples(region.x, region.y, region.width, region.height, band, transfer);
        }

        /** Clamps data to the range of target integer type. */
        void clamp(final int length) {
            for (int i=0; i<length; i++) {
                transfer[i] = Numerics.clamp(Math.round(buffer[i]));
            }
        }
    }




    /**
     * Read {@code double} values from the source raster and write signed {@code short} values in a temporary buffer.
     * The floating point values will be rounded and clamped to the range of the integer type.
     */
    private static final class DoubleToShort extends DoubleToInteger {
        /** Creates a new instance for transferring data between the two specified rasters. */
        DoubleToShort(final Raster source, final WritableRaster target, final Rectangle aoi) {
            super(source, target, aoi);
        }

        /** Clamps data to the range of target integer type. */
        @Override void clamp(final int length) {
            for (int i=0; i<length; i++) {
                transfer[i] = (int) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(buffer[i])));
            }
        }
    }




    /**
     * Read {@code double} values from the source raster and write unsigned {@code short} values in a temporary buffer.
     * The floating point values will be rounded and clamped to the range of the integer type.
     */
    private static final class DoubleToUShort extends DoubleToInteger {
        /** Creates a new instance for transferring data between the two specified rasters. */
        DoubleToUShort(final Raster source, final WritableRaster target, final Rectangle aoi) {
            super(source, target, aoi);
        }

        /** Clamps data to the range of target integer type. */
        @Override void clamp(final int length) {
            for (int i=0; i<length; i++) {
                transfer[i] = (int) Math.max(0, Math.min(0xFFFF, Math.round(buffer[i])));
            }
        }
    }




    /**
     * Read {@code double} values from the source raster and write unsigned {@code byte} values in a temporary buffer.
     * The floating point values will be rounded and clamped to the range of the integer type.
     */
    private static final class DoubleToByte extends DoubleToInteger {
        /** Creates a new instance for transferring data between the two specified rasters. */
        DoubleToByte(final Raster source, final WritableRaster target, final Rectangle aoi) {
            super(source, target, aoi);
        }

        /** Clamps data to the range of target integer type. */
        @Override void clamp(final int length) {
            for (int i=0; i<length; i++) {
                transfer[i] = (int) Math.max(0, Math.min(0xFF, Math.round(buffer[i])));
            }
        }
    }




    /**
     * Read {@code float} values from the source raster and write {@code int} values in a temporary buffer.
     * The floating point values will be rounded and clamped to the range of the integer type.
     * Note that we do not provide any direct version for integer types because direct access
     * to {@link DataBuffer} array disable Java2D acceleration on video card.
     */
    private static class FloatToInteger extends Transferer {
        /** Temporary buffer where to copy data and apply operation. */
        protected float[] buffer;

        /** Temporary buffer where to round data before transfer to target raster. */
        protected int[] transfer;

        /** Creates a new instance for transferring data between the two specified rasters. */
        FloatToInteger(final Raster source, final WritableRaster target, final Rectangle aoi) {
            super(source, target, aoi);
        }

        /** Subdivides the region to process in smaller strips, for smaller {@linkplain #buffer}. */
        @Override final int prepareTransferRegion() {
            return prepareTransferRegion(region, DataBuffer.TYPE_FLOAT);
        }

        /** Copies source values in temporary buffer, applies conversion then copies to target. */
        @Override final void computeStrip(final MathTransform1D converter) throws TransformException {
            final int length = length();
            buffer = source.getSamples(region.x, region.y, region.width, region.height, band, buffer);
            converter.transform(buffer, 0, buffer, 0, length);
            if (transfer == null) transfer = new int[length];
            clamp(length);
            target.setSamples(region.x, region.y, region.width, region.height, band, transfer);
        }

        /** Clamps data to the range of target integer type. */
        void clamp(final int length) {
            for (int i=0; i<length; i++) {
                transfer[i] = Math.round(buffer[i]);
            }
        }
    }




    /**
     * Read {@code float} values from the source raster and write signed {@code short} values in a temporary buffer.
     * The floating point values will be rounded and clamped to the range of the integer type.
     */
    private static final class FloatToShort extends FloatToInteger {
        /** Creates a new instance for transferring data between the two specified rasters. */
        FloatToShort(final Raster source, final WritableRaster target, final Rectangle aoi) {
            super(source, target, aoi);
        }

        /** Clamps data to the range of target integer type. */
        @Override void clamp(final int length) {
            for (int i=0; i<length; i++) {
                transfer[i] = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(buffer[i])));
            }
        }
    }




    /**
     * Read {@code float} values from the source raster and write unsigned {@code short} values in a temporary buffer.
     * The floating point values will be rounded and clamped to the range of the integer type.
     */
    private static final class FloatToUShort extends FloatToInteger {
        /** Creates a new instance for transferring data between the two specified rasters. */
        FloatToUShort(final Raster source, final WritableRaster target, final Rectangle aoi) {
            super(source, target, aoi);
        }

        /** Clamps data to the range of target integer type. */
        @Override void clamp(final int length) {
            for (int i=0; i<length; i++) {
                transfer[i] = Math.max(0, Math.min(0xFFFF, Math.round(buffer[i])));
            }
        }
    }




    /**
     * Read {@code float} values from the source raster and write unsigned {@code byte} values in a temporary buffer.
     * The floating point values will be rounded and clamped to the range of the integer type.
     */
    private static final class FloatToByte extends FloatToInteger {
        /** Creates a new instance for transferring data between the two specified rasters. */
        FloatToByte(final Raster source, final WritableRaster target, final Rectangle aoi) {
            super(source, target, aoi);
        }

        /** Clamps data to the range of target integer type. */
        @Override void clamp(final int length) {
            for (int i=0; i<length; i++) {
                transfer[i] = Math.max(0, Math.min(0xFF, Math.round(buffer[i])));
            }
        }
    }




    /**
     * Suggests a strategy for transferring data from the given source image to the given target.
     * This method assumes that the source image uses the same pixel coordinate system than the
     * target raster (i.e. that pixels at the same coordinates are at the same location on Earth).
     * It also assumes that the target tile is fully included in the bounds of a single source tile.
     * That later condition is met if the target grid tiles has been created by {@link ImageLayout}.
     *
     * @param  source  image from which to read sample values.
     * @param  target  image tile where to write sample values after processing.
     * @return object to use for applying the operation.
     */
    static Transferer create(final RenderedImage source, final WritableRaster target) {
        int tileX = ImageUtilities.pixelToTileX(source, target.getMinX());
        int tileY = ImageUtilities.pixelToTileY(source, target.getMinY());
        return create(source.getTile(tileX, tileY), target, target.getBounds());
    }

    /**
     * Suggests a strategy for transferring data from the given source to the given target.
     * The operation to apply on sample values during transfer is specified later, during
     * the call to {@link #compute(MathTransform1D[])}.
     *
     * @param  source  image tile from which to read sample values.
     * @param  target  image tile where to write sample values after processing.
     * @param  aoi     the area of interest, often {@code target.getBounds()}. It is caller responsibility
     *                 to ensure that {@code aoi} is contained in {@code source} and {@code target} bounds.
     * @return object to use for applying the operation.
     */
    static Transferer create(final Raster source, final WritableRaster target, final Rectangle aoi) {
        switch (ImageUtilities.getBandType(target.getSampleModel())) {
            case DataBuffer.TYPE_DOUBLE: {
                if (isDirect(target, aoi)) {
                    return new DoubleToDirect(source, target, aoi);
                }
                break;
            }
            case DataBuffer.TYPE_FLOAT: {
                switch (ImageUtilities.getBandType(source.getSampleModel())) {
                    case DataBuffer.TYPE_BYTE:
                    case DataBuffer.TYPE_SHORT:
                    case DataBuffer.TYPE_USHORT:        // TODO: consider using IntegerToDirect here.
                    case DataBuffer.TYPE_FLOAT: {
                        if (isDirect(target, aoi)) {
                            return new FloatToDirect(source, target, aoi);
                        } else {
                            return new FloatToFloat(source, target, aoi);
                        }
                    }
                    /*
                     * TYPE_DOUBLE, TYPE_INT and any unknown types. We handle TYPE_INT as `double`
                     * because conversion of 32 bits integer to `float` may lost precision digits.
                     */
                }
                break;
            }
            case DataBuffer.TYPE_INT:    return singlePrecision(source) ? new FloatToInteger(source, target, aoi) : new DoubleToInteger(source, target, aoi);
            case DataBuffer.TYPE_USHORT: return singlePrecision(source) ? new FloatToUShort (source, target, aoi) : new DoubleToUShort (source, target, aoi);
            case DataBuffer.TYPE_SHORT:  return singlePrecision(source) ? new FloatToShort  (source, target, aoi) : new DoubleToShort  (source, target, aoi);
            case DataBuffer.TYPE_BYTE:   return singlePrecision(source) ? new FloatToByte   (source, target, aoi) : new DoubleToByte   (source, target, aoi);
        }
        /*
         * Most conservative fallback, also used for any unknown type.
         */
        return new DoubleToDouble(source, target, aoi);
    }

    /**
     * Returns {@code true} if the given raster uses a data type that can be casted to the {@code float} type
     * without precision lost. If the type is unknown, then this method returns {@code false}. Note that this
     * method also returns {@code false} for {@link DataBuffer#TYPE_INT} because conversion of 32 bits integer
     * to the {@code float} type may lost precision digits.
     */
    private static boolean singlePrecision(final Raster source) {
        switch (ImageUtilities.getBandType(source.getSampleModel())) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_FLOAT: return true;
            default: return false;
        }
    }

    /**
     * Returns {@code true} if the given raster stores sample values at consecutive locations in each band.
     * In other words, verifies if the given raster uses a pixel stride of 1 with no gab between lines.
     * Another condition is that the data buffer must start at offset 0.
     */
    private static boolean isDirect(final Raster target, final Rectangle aoi) {
        if (target.getMinX() == aoi.x && target.getWidth() == aoi.width) {
            final SampleModel sm = target.getSampleModel();
            if (sm instanceof ComponentSampleModel) {
                final ComponentSampleModel cm = (ComponentSampleModel) sm;
                if (cm.getPixelStride() == 1 && cm.getScanlineStride() == target.getWidth()) {
                    for (final int offset : target.getDataBuffer().getOffsets()) {
                        if (offset != 0) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }
}
