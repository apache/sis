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

import java.awt.Transparency;
import java.awt.image.DataBuffer;
import java.awt.image.ComponentColorModel;
import java.awt.image.RasterFormatException;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.internal.feature.Resources;


/**
 * A color model for use with {@link ScaledColorSpace} (gray scale image with missing values).
 * This color model is slightly more efficient than the default {@link ComponentColorModel} by
 * reducing the amount of object allocations, made possible by the knowledge that we use only
 * one sample value and returns only one color component (the gray).
 * In addition, this class render the {@link Float#NaN} values as transparent.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class ScaledColorModel extends ComponentColorModel {
    /**
     * Index of the band to display. This is a copy of {@link ScaledColorSpace#visibleBand}.
     */
    private final int visibleBand;

    /**
     * The scaling factor from sample values to RGB normalized values.
     * This information duplicates {@link ScaledColorSpace#scale} but
     * for a scale from 0 to 256 instead than 0 to 1.
     */
    private final double scale;

    /**
     * The offset to subtract from sample values before to apply the {@linkplain #scale} factor.
     * This information duplicates {@link ScaledColorSpace#offset} but for a scale from 0 to 256
     * instead than 0 to 1.
     */
    private final double offset;

    /**
     * Creates a new color model.
     *
     * @param  colorSpace  the color space to use with this color model.
     * @param  minimum     the minimal sample value expected, inclusive.
     * @param  maximum     the maximal sample value expected, exclusive.
     * @param  type        one of the {@link DataBuffer} constants.
     */
    ScaledColorModel(final ScaledColorSpace colorSpace, final double minimum, final double maximum, final int type) {
        super(colorSpace, false, false, Transparency.BITMASK, type);
        visibleBand = colorSpace.visibleBand;
        scale  = 0x100 / (maximum - minimum);
        offset = minimum;
    }

    /**
     * Returns the red component of the given value.
     * Defined for consistency but should not be used.
     */
    @Override
    public int getRed(final Object inData) {
        return getRGB(inData) & 0xFF;
    }

    /**
     * Returns the green component of the given value.
     * Defined for consistency but should not be used.
     */
    @Override
    public int getGreen(final Object inData) {
        return (getRGB(inData) >>> Byte.SIZE) & 0xFF;
    }

    /**
     * Returns the green component of the given value.
     * Defined for consistency but should not be used.
     */
    @Override
    public int getBlue(final Object inData) {
        return (getRGB(inData) >>> 2*Byte.SIZE) & 0xFF;
    }

    /**
     * Returns the alpha value for the given sample values.
     * This is based only on whether or not the value is NaN.
     */
    @Override
    public int getAlpha(final Object inData) {
        switch (transferType) {
            case DataBuffer.TYPE_FLOAT:  return Float .isNaN(((float[])  inData)[visibleBand]) ? 0 : 0xFF;
            case DataBuffer.TYPE_DOUBLE: return Double.isNaN(((double[]) inData)[visibleBand]) ? 0 : 0xFF;
            default: return 0xFF;
        }
    }

    /**
     * Returns the color/alpha components for the specified pixel in the default RGB color model format.
     */
    @Override
    public int getRGB(final Object inData) {
        final double value;
        switch (transferType) {
            case DataBuffer.TYPE_BYTE:   value = Byte .toUnsignedInt(((byte[])   inData)[visibleBand]); break;
            case DataBuffer.TYPE_USHORT: value = Short.toUnsignedInt(((short[])  inData)[visibleBand]); break;
            case DataBuffer.TYPE_SHORT:  value =                     ((short[])  inData)[visibleBand];  break;
            case DataBuffer.TYPE_INT:    value =                     ((int[])    inData)[visibleBand];  break;
            case DataBuffer.TYPE_FLOAT:  value =                     ((float[])  inData)[visibleBand];  break;
            case DataBuffer.TYPE_DOUBLE: value =                     ((double[]) inData)[visibleBand];  break;
            default: throw new RasterFormatException(Resources.format(Resources.Keys.UnknownDataType_1, transferType));
        }
        if (Double.isNaN(value)) {
            return 0;                                           // Transparent pixel.
        }
        final int c = Math.max(0, Math.min(0xFF, (int) ((value - offset) * scale)));
        return c | (c << Byte.SIZE) | (c << 2*Byte.SIZE) | 0xFF000000;
    }

    /**
     * Returns a hash code value for this color model.
     */
    @Override
    public int hashCode() {
        return Long.hashCode(Double.doubleToLongBits(scale)
                      + 31 * Double.doubleToLongBits(offset))
                      +  7 * getNumComponents() + visibleBand;
    }

    /**
     * Compares this color model with the given object for equality.
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof ScaledColorModel && super.equals(other)) {
            final ScaledColorModel that = (ScaledColorModel) other;
            return visibleBand == that.visibleBand
                    && Numerics.equals(scale,  that.scale)
                    && Numerics.equals(offset, that.offset);
        }
        return false;
    }
}
