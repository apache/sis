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
package org.apache.sis.image.internal.shared;

import java.util.Arrays;
import java.util.Objects;
import java.lang.reflect.Array;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;


/**
 * Helper class for handling the fill values of a raster. A value can be specified for each band.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class FillValues {
    /**
     * The fill values as an array of primitive type: {@code int[]}, {@code float[]} or {@code double[]}.
     * No other type is allowed. This field is never null and the array length is the number of bands.
     */
    public final Object asPrimitiveArray;

    /**
     * {@code true} if all values in {@link #asPrimitiveArray} are positive 0.
     * This is the value of newly constructed Java arrays or {@link Raster} instances.
     * Consequently, calls to {@link #fill(WritableRaster)} can be skipped
     * if the raster is new and this flag is {@code true}.
     */
    public final boolean isFullyZero;

    /**
     * Stores the given fill values as an array of primitive type.
     * If the given array is {@code null}, or if any element in the given array is {@code null},
     * then the default fill value is NaN for floating point data types or zero for integer data types.
     * If the array is shorter than the number of bands, then above-cited default values are used for missing values.
     * If longer than the number of bands, extraneous values are ignored.
     *
     * @param  model       the sample model of the image for which to prepare fill values.
     * @param  values      the fill values, or {@code null} for the default values.
     * @param  allowFloat  whether to allow {@link #asPrimitiveArray} to be {@code float[]}.
     *                     If {@code false}, then {@code asPrimitiveArray} can only be
     *                     {@code double[]} or {@code int[]}.
     */
    public FillValues(final SampleModel model, final Number[] values, final boolean allowFloat) {
        final int dataType = model.getDataType();
        final int numBands = model.getNumBands();
        final int toCopy   = (values != null) ? Math.min(values.length, numBands) : 0;
        long zeroFlag;
        if (ImageUtilities.isIntegerType(dataType)) {
            final int[] fill = new int[numBands];
            asPrimitiveArray = fill;
            zeroFlag = 0;
            for (int i=0; i<toCopy; i++) {
                final Number f = values[i];
                if (f != null) {
                    zeroFlag |= (fill[i] = f.intValue());
                }
            }
        } else if (allowFloat && dataType == DataBuffer.TYPE_FLOAT) {
            final float[] fill = new float[numBands];
            Arrays.fill(fill, Float.NaN);
            asPrimitiveArray = fill;
            zeroFlag = toCopy ^ numBands;
            for (int i=0; i<toCopy; i++) {
                final Number f = values[i];
                if (f != null) {
                    // Negative zero intentionally considered as "non-zero".
                    zeroFlag |= Float.floatToRawIntBits(fill[i] = f.floatValue());
                } else {
                    zeroFlag = -1;
                }
            }
        } else {
            final double[] fill = new double[numBands];
            Arrays.fill(fill, Double.NaN);
            asPrimitiveArray = fill;
            zeroFlag = toCopy ^ numBands;
            for (int i=0; i<toCopy; i++) {
                final Number f = values[i];
                if (f != null) {
                    // Negative zero intentionally considered as "non-zero".
                    zeroFlag |= Double.doubleToRawLongBits(fill[i] = f.doubleValue());
                } else {
                    zeroFlag = -1;
                }
            }
        }
        isFullyZero = (zeroFlag == 0);
    }

    /**
     * Fills the given raster with the fill value. The raster sample model should be the same
     * than the sample model specified at construction time of this {@code FillValues} object.
     *
     * <h4>Implementation note</h4>
     * This method fills the raster using its API, without direct access to the backing array.
     * We do not use direct array access because it would disable Java2D hardware acceleration
     * in subsequent operations.
     *
     * @param  tile  the raster to fill.
     *
     * @see #isFullyZero
     */
    public void fill(final WritableRaster tile) {
        final int xmin       = tile.getMinX();
        final int width      = tile.getWidth();
        final int numBands   = tile.getNumBands();
        final int chunkWidth = Integer.highestOneBit(Math.max(Math.min(width*numBands, 256) / numBands, 1));
        /*
         * For performance reason, prepare an array where fill values are repeated an arbitrary number of times.
         * The number of bands in the tile should match the number of bands at `FillValues` construction time,
         * but if they differ (we are paranoiac) we will adjust.
         */
        Object chunk = Array.newInstance(asPrimitiveArray.getClass().getComponentType(), chunkWidth * numBands);
        System.arraycopy(asPrimitiveArray, 0, chunk, 0, Math.min(numBands, Array.getLength(asPrimitiveArray)));
        for (int p = 1; p < chunkWidth; p <<= 1) {
            final int n = p * numBands;
            System.arraycopy(chunk, 0, chunk, n, n);
        }
        /*
         * Write the fill values at the beginning of first row.
         */
        int y = tile.getMinY();
        if (chunk instanceof int[]) {
            tile.setPixels(xmin, y, chunkWidth, 1, (int[]) chunk);
        } else if (chunk instanceof float[]) {
            tile.setPixels(xmin, y, chunkWidth, 1, (float[]) chunk);
        } else {
            tile.setPixels(xmin, y, chunkWidth, 1, (double[]) chunk);
        }
        /*
         * Read the data that we just wrote, but allowing Java2D to use an opaque, potentially packed, format.
         * The chunk below will contain the same data as previous `chunk` but may be encoded differently.
         * It allows (sometimes) faster copies. We use that packed array for the remaining of first row and
         * all next rows.
         */
        chunk = tile.getDataElements(xmin, y, chunkWidth, 1, null);
        final int ymax = y + tile.getHeight();
        final int xmax = xmin + width;
        int x = xmin + chunkWidth;
        do {
            int remaining;
            while ((remaining = xmax - x) > 0) {
                tile.setDataElements(x, y, Math.min(chunkWidth, remaining), 1, chunk);
                x += chunkWidth;
            }
            x = xmin;
        } while (++y < ymax);
    }

    /**
     * Fills the given image with the fill value. The image sample model should be the same
     * than the sample model specified at construction time of this {@code FillValues} object.
     *
     * @param  image  the image to fill.
     */
    public void fill(final WritableRenderedImage image) {
        int y = image.getMinTileY();
        for (int ny = image.getNumYTiles(); --ny >= 0; y++) {
            int x = image.getMinTileX();
            for (int nx = image.getNumXTiles(); --nx >= 0; x++) {
                final WritableRaster raster = image.getWritableTile(x, y);
                try {
                    fill(raster);
                } finally {
                    image.releaseWritableTile(x, y);
                }
            }
        }
    }

    /**
     * Compares this object with given object for equality.
     *
     * @param  obj  the other object to compare with this object.
     * @return {@code true} if the two objects have the same fill values.
     */
    @Override
    public boolean equals(final Object obj) {
        return (obj instanceof FillValues) && Objects.deepEquals(asPrimitiveArray, ((FillValues) obj).asPrimitiveArray);
    }

    /**
     * Returns a hash code for the fill values.
     */
    @Override
    public int hashCode() {
        return ~Arrays.deepHashCode(new Object[] {asPrimitiveArray});
    }
}
