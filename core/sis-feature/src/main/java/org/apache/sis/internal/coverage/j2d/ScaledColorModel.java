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
import org.apache.sis.internal.feature.Resources;


/**
 * A color model for use with {@link ScaledColorSpace} (gray scale image with missing values).
 * This color model is slightly more efficient than the default {@link ComponentColorModel} by
 * reducing the amount of object allocations, made possible by the knowledge that we use only
 * one sample value and returns only one color component (the gray).
 * In addition, this class renders the {@link Float#NaN} values as transparent.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class ScaledColorModel extends ComponentColorModel {
    /**
     * The mask to apply for getting a single color component.
     * This is also the maximum (inclusive) color/alpha value.
     */
    private static final int MASK = 0xFF;

    /**
     * Size of the range of color values. Since minimum value is zero,
     * this range is also the maximum (exclusive) color/alpha value.
     */
    static final int RANGE = 0x100;

    /**
     * The object from which to get the coefficients for converting values to colors.
     * This is a reference to the same object than {@link #getColorSpace()}, but stored
     * as a {@link ScaledColorSpace} for avoiding the need to perform casts.
     */
    private final ScaledColorSpace cs;

    /**
     * Creates a new color model for the range of values in the given color space.
     *
     * @param  colorSpace   the color space to use with this color model.
     * @param  type         one of the {@link DataBuffer} constants.
     */
    ScaledColorModel(final ScaledColorSpace colorSpace, final int type) {
        super(colorSpace, false, false, Transparency.BITMASK, type);
        cs = colorSpace;
    }

    /**
     * Creates a new color model with only a subset of the bands in this color model.
     */
    final ScaledColorModel createSubsetColorModel(final int[] bands) {
        return new ScaledColorModel(new ScaledColorSpace(cs, bands), transferType);
    }

    /**
     * Defined for consistency but should not be used.
     */
    @Override public int getRed  (int    value) {return (getRGB(value) >>> 2*Byte.SIZE) & MASK;}
    @Override public int getRed  (Object value) {return (getRGB(value) >>> 2*Byte.SIZE) & MASK;}
    @Override public int getGreen(int    value) {return (getRGB(value) >>>   Byte.SIZE) & MASK;}
    @Override public int getGreen(Object value) {return (getRGB(value) >>>   Byte.SIZE) & MASK;}
    @Override public int getBlue (int    value) {return  getRGB(value)                  & MASK;}
    @Override public int getBlue (Object value) {return  getRGB(value)                  & MASK;}
    @Override public int getAlpha(int    value) {return                                   MASK;}

    /**
     * Returns whether this color model is capable to handle transparent pixels.
     */
    @Override
    public int getTransparency() {
        return ImageUtilities.isIntegerType(transferType) ? Transparency.OPAQUE : Transparency.BITMASK;
    }

    /**
     * Returns the alpha value for the given sample values.
     * This is based only on whether or not the value is NaN.
     */
    @Override
    public int getAlpha(final Object inData) {
        final int visibleBand = cs.visibleBand;
        switch (transferType) {
            case DataBuffer.TYPE_FLOAT:  return Float .isNaN(((float[])  inData)[visibleBand]) ? 0 : MASK;
            case DataBuffer.TYPE_DOUBLE: return Double.isNaN(((double[]) inData)[visibleBand]) ? 0 : MASK;
            default: return MASK;
        }
    }

    /**
     * Returns the color/alpha components for the specified pixel in the default RGB color model format.
     */
    @Override
    public int getRGB(final Object inData) {
        final int visibleBand = cs.visibleBand;
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
        final int c = Math.max(0, Math.min(MASK, (int) ((value - cs.offset) * cs.scale)));
        return c | (c << Byte.SIZE) | (c << 2*Byte.SIZE) | (MASK << 3*Byte.SIZE);
    }

    /**
     * Returns the color/alpha components of the pixel.
     */
    @Override
    public int getRGB(final int value) {
        final int c = Math.max(0, Math.min(MASK, (int) ((value - cs.offset) * cs.scale)));
        return c | (c << Byte.SIZE) | (c << 2*Byte.SIZE) | (MASK << 3*Byte.SIZE);
    }

    /**
     * Returns {@code true} if the given object is also an instance of {@link ScaledColorModel}
     * with equals color space and same transfer type.
     *
     * <div class="note"><b>Note:</b>
     * we have to override this method because the {@link ComponentColorModel#equals(Object)} implementation
     * is confused by our overriding of {@link #getTransparency()} method. However we do not need to override
     * {@link #hashCode()}.</div>
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof ScaledColorModel) {
            final ScaledColorModel other = (ScaledColorModel) obj;
            return transferType == other.getTransferType() && cs.equals(other.cs);
        }
        return false;
    }
}
