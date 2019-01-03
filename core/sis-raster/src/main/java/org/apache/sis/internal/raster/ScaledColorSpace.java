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

import java.awt.color.ColorSpace;
import org.apache.sis.internal.util.Strings;


/**
 * Color space for images storing pixels as real numbers. The color model can have an
 * arbitrary number of bands, but in current implementation only one band is used.
 * Current implementation create a gray scale.
 *
 * <p>The use of this color model is very slow.
 * It should be used only when no standard color model can be used.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class ScaledColorSpace extends ColorSpace {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 438226855772441165L;

    /**
     * Minimal normalized RGB value.
     */
    private static final float MIN_VALUE = 0f;

    /**
     * Maximal normalized RGB value.
     */
    private static final float MAX_VALUE = 1f;

    /**
     * The scaling factor from sample values to RGB normalized values.
     */
    private final float scale;

    /**
     * The offset to subtract from sample values before to apply the {@linkplain #scale} factor.
     */
    private final float offset;

    /**
     * Index of the band to display.
     */
    private final int visibleBand;

    /**
     * Creates a color model for the given range of values.
     *
     * @param  numComponents  the number of components.
     * @param  visibleBand    the band to use for computing colors.
     * @param  minimum        the minimal sample value expected.
     * @param  maximum        the maximal sample value expected.
     */
    public ScaledColorSpace(final int numComponents, final int visibleBand, final double minimum, final double maximum) {
        super(TYPE_GRAY, numComponents);
        this.visibleBand = visibleBand;
        final double scale  = (MAX_VALUE - MIN_VALUE) / (maximum - minimum);
        this.scale  = (float) scale;
        this.offset = (float) (minimum - MIN_VALUE / scale);
    }

    /**
     * Returns a RGB color for a sample value.
     *
     * @param  samples  sample values in the raster.
     * @return color as normalized RGB values between 0 and 1.
     */
    @Override
    public float[] toRGB(final float[] samples) {
        float value = (samples[visibleBand] - offset) * scale;
        if (!(value >= MIN_VALUE)) {                            // Use '!' for catching NaN.
            value = MIN_VALUE;
        } else if (value > MAX_VALUE) {
            value = MAX_VALUE;
        }
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
        values[visibleBand] = (color[0] + color[1] + color[2]) / (3 * scale) + offset;
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
        values[visibleBand] = (color[0] / 0.9642f + color[1] + color[2] / 0.8249f) / (3 * scale) + offset;
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
        return MIN_VALUE / scale + offset;
    }

    /**
     * Returns the maximum value for the specified RGB component.
     *
     * @param  component  the component index.
     * @return maximum normalized component value.
     */
    @Override
    public float getMaxValue(final int component) {
        return MAX_VALUE / scale + offset;
    }

    /**
     * Returns a string representation of this color model.
     *
     * @return a string representation for debugging purpose.
     */
    @Override
    public String toString() {
        return Strings.range(getClass(), getMinValue(visibleBand), getMaxValue(visibleBand));
    }
}
