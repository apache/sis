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
package org.apache.sis.image.privy;

import java.awt.color.ColorSpace;
import org.apache.sis.util.Debug;
import org.apache.sis.util.privy.Numerics;


/**
 * Color space for images storing pixels as real numbers. This color space can have an arbitrary number of bands,
 * but only one band is shown. Current implementation produces grayscale image only, but it may change in future.
 *
 * <p>The use of this color space is very slow.
 * It should be used only when no standard color space can be used.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 *
 * @see ScaledColorModel
 * @see ColorModelFactory#createGrayScale(int, int, int, double, double)
 */
final class ScaledColorSpace extends ColorSpace {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5146474397268513490L;

    /**
     * The scaling factor from sample values to RGB values. The target RGB values will be in range 0
     * to 255 inclusive. Note that the target range is different than the range of {@link ColorSpace}
     * normalized values; methods in this class has to divide values by {@value ScaledColorModel#RANGE}.
     */
    final double scale;

    /**
     * The offset to subtract from sample values before to apply the {@linkplain #scale} factor.
     */
    final double offset;

    /**
     * The maximum value specified at construction time.
     */
    final double maximum;

    /**
     * Index of the band to display, from 0 inclusive to {@link #getNumComponents()} exclusive.
     */
    final int visibleBand;

    /**
     * Creates a color space for the given range of values.
     * The given range does not need to be exact; values outside that range will be clamped.
     *
     * @param  numComponents  the number of components.
     * @param  visibleBand    the band to use for computing colors.
     * @param  minimum        the minimal sample value expected, inclusive.
     * @param  maximum        the maximal sample value expected, exclusive.
     */
    ScaledColorSpace(final int numComponents, final int visibleBand, final double minimum, final double maximum) {
        super(TYPE_GRAY, numComponents);
        this.visibleBand = visibleBand;
        this.maximum     = maximum;
        this.scale       = ScaledColorModel.RANGE / (maximum - minimum);
        this.offset      = minimum;
    }

    /**
     * Creates a color space for the same range of values than the given space, but a different number of bands.
     *
     * @param  numComponents  the new number of components.
     * @param  visibleBand    the new band to select as the visible band.
     */
    ScaledColorSpace(final ScaledColorSpace parent, final int numComponents, final int visibleBand) {
        super(TYPE_GRAY, numComponents);
        this.scale       = parent.scale;
        this.offset      = parent.offset;
        this.maximum     = parent.maximum;
        this.visibleBand = visibleBand;
    }

    /**
     * Returns a RGB color for a sample value.
     *
     * @param  samples  sample values in the raster.
     * @return color as normalized RGB values between 0 and 1.
     */
    @Override
    public float[] toRGB(final float[] samples) {
        float value = Math.min(1, (float) ((samples[visibleBand] - offset) * (1d/ScaledColorModel.RANGE * scale)));
        if (!(value >= 0)) value = 0;                   // Use `!` for replacing NaN.
        return new float[] {value, value, value};
    }

    /**
     * Returns a sample value for the specified RGB color.
     *
     * @param  color  normalized RGB values between 0 and 1.
     * @return sample values in the raster.
     */
    @Override
    public float[] fromRGB(final float[] color) {
        final float[] values = new float[getNumComponents()];
        values[visibleBand] = (float) ((color[0] + color[1] + color[2])
                            / (3d/ScaledColorModel.RANGE * scale) + offset);
        return values;
    }

    /**
     * Returns a CIEXYZ color for a sample value.
     *
     * @param  values  sample values in the raster.
     * @return color as normalized CIEXYZ values between 0 and 1.
     */
    @Override
    public float[] toCIEXYZ(final float[] values) {
        final float[] codes = toRGB(values);
        codes[0] *= 0.9642f;
        codes[2] *= 0.8249f;
        return codes;
    }

    /**
     * Returns a sample value for the specified CIEXYZ color.
     *
     * @param  color  normalized CIEXYZ values between 0 and 1.
     * @return sample values in the raster.
     */
    @Override
    public float[] fromCIEXYZ(final float[] color) {
        final float[] values = new float[getNumComponents()];
        values[visibleBand] = (float) ((color[0] / 0.9642f + color[1] + color[2] / 0.8249f)
                            / (3d/ScaledColorModel.RANGE * scale) + offset);
        return values;
    }

    /**
     * Returns the minimum value for the specified RGB component.
     *
     * @param  component  the component index.
     * @return minimum normalized component value.
     */
    @Override
    public float getMinValue(final int component) {
        return (float) offset;
    }

    /**
     * Returns the maximum value for the specified RGB component.
     *
     * @param  component  the component index.
     * @return maximum normalized component value.
     */
    @Override
    public float getMaxValue(final int component) {
        return (float) maximum;
    }

    /**
     * Returns a string representation of this color space.
     *
     * @return a string representation for debugging purpose.
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(20).append(getClass().getSimpleName());
        formatRange(buffer);
        return buffer.toString();
    }

    /**
     * Formats the range of values in the given buffer.
     * This method is used for {@link #toString()} implementation and may change in any future version.
     *
     * @param  buffer  where to append the range of values.
     */
    @Debug
    final void formatRange(final StringBuilder buffer) {
        buffer.append('[').append(offset)
            .append(" … ").append(maximum)
            .append(" in band ").append(visibleBand).append(']');
    }

    /**
     * Returns a hash code value for this color space.
     */
    @Override
    public int hashCode() {
        return Long.hashCode(Double.doubleToLongBits(scale)
                      + 31 * Double.doubleToLongBits(offset))
                      +  7 * getNumComponents() + visibleBand;
    }

    /**
     * Compares this color space with the given object for equality.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof ScaledColorSpace) {
            final ScaledColorSpace that = (ScaledColorSpace) obj;
            return Numerics.equals(scale,   that.scale)             &&
                   Numerics.equals(offset,  that.offset)            &&
                   Numerics.equals(maximum, that.maximum)           &&
                   visibleBand          ==  that.visibleBand        &&
                   getNumComponents()   ==  that.getNumComponents() &&
                   getType()            ==  that.getType();
        }
        return false;
    }
}
