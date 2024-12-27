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
package org.apache.sis.coverage.privy;

import java.util.Arrays;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.BandedSampleModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;


/**
 * Helper classes for creating a <abbr>RGB</abbr> color model suitable for a sample model.
 * Usage:
 *
 * <ol>
 *   <li>Create a new {@link ColorModelBuilder} instance.</li>
 *   <li>Invoke setter methods.</li>
 *   <li>Invoke {@link #createPackedRGB()} or {@link #createBandedRGB()}.</li>
 *   <li>Discards {@code ColorModelBuilder}. Each instance shall be used only once.</li>
 * </ol>
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see ColorModelType
 */
public final class ColorModelBuilder {
    /**
     * The usual position of the alpha channel.
     * This channel is usually immediately after the <abbr>RGB</abbr> components.
     */
    private static final int STANDARD_ALPHA_BAND = 3;

    /**
     * Number of bits per sample, or {@code null} for the default values in all bands.
     * The array length is the number of bands: 3 for <abbr>RGB</abbr> or 4 for <abbr>ARGB</abbr>.
     * Each array element may be 0, which means to use the default for that specific band.
     * If no number of bits is specified, the default is {@value Byte#SIZE}.
     */
    private int[] bitsPerSample;

    /**
     * Index of the alpha channel (usually the last band), or -1 if none.
     */
    private int alphaBand;

    /**
     * Whether the alpha value (if present) is premultiplied.
     */
    private boolean isAlphaPremultiplied;

    /**
     * Creates a new builder initialized with default values.
     */
    public ColorModelBuilder() {
        alphaBand = -1;
    }

    /**
     * Sets the number of bits per sample as a potentially different number for each band.
     * The given array is stored without copy on the assumption that is will not be modified
     * (this is okay for internal API).
     * If no number of bits is specified, the default is {@value Byte#SIZE}.
     *
     * @param  numBits  number of bits per sample, or {@code null} for the default values.
     * @return {@code this} for method calls chaining.
     */
    public ColorModelBuilder bitsPerSample(final int[] numBits) {
        bitsPerSample = numBits;
        if (hasDefaultBitsPerSample()) {
            bitsPerSample = null;
        }
        return this;
    }

    /**
     * Sets the number of bits per sample as a unique number for all bands.
     * If no number of bits is specified, the default is {@value Byte#SIZE}.
     *
     * @param  numBits  number of bits per sample in each band.
     * @return {@code this} for method calls chaining.
     * @throws IllegalArgumentException if the given value is out of bounds.
     */
    public ColorModelBuilder bitsPerSample(final int numBits) {
        ArgumentChecks.ensureBetween("bitsPerSample", 1, Integer.SIZE, numBits);
        if (numBits != Byte.SIZE) {
            bitsPerSample = new int[STANDARD_ALPHA_BAND + 1];
            Arrays.fill(bitsPerSample, numBits);
        } else {
            bitsPerSample = null;
        }
        return this;
    }

    /**
     * Returns whether this builder currently uses the default number of bits per sample.
     */
    private boolean hasDefaultBitsPerSample() {
        if (bitsPerSample != null) {
            for (int numBits : bitsPerSample) {
                if (numBits != Byte.SIZE) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns the number of bits per sample in the given band.
     * If no number of bits per sample was specified, the default value is {@link Byte#SIZE}.
     *
     * @param  band  the band index for which to get the number of bits per sample.
     * @param  max   maximal value allowed.
     * @return the number of bits per sample in the given band, guaranteed between 1 and {@code max} inclusive.
     * @throws IllegalArgumentException if the number of bits per sample is out of bounds.
     */
    private int getBitsPerSample(final int band, final int max) {
        if (bitsPerSample != null && band < bitsPerSample.length) {
            int numBits = bitsPerSample[band];
            if (numBits != 0) {
                ArgumentChecks.ensureBetween("bitsPerSample", 1, max, numBits);
                return numBits;
            }
        }
        return Byte.SIZE;
    }

    /**
     * Sets the index of the alpha channel. It should be the band immediately after the color bands,
     * but some color model allows other position.
     *
     * @param  index  of the alpha channel (usually the last band), or -1 if none.
     * @return {@code this} for method calls chaining.
     */
    public ColorModelBuilder alphaBand(final int index) {
        if (index >= 0) {
            ArgumentChecks.ensureBetween("alphaBand", 0, STANDARD_ALPHA_BAND, index);
        }
        alphaBand = index;
        return this;
    }

    /**
     * Sets whether the alpha value (if present) is premultiplied.
     *
     * @param  p  whether the alpha value (if present) is premultiplied.
     * @return {@code this} for method calls chaining.
     */
    public ColorModelBuilder isAlphaPremultiplied(final boolean p) {
        isAlphaPremultiplied = p;
        return this;
    }

    /**
     * Returns the color space of the color model to build.
     * The current implementation fixes the color space to <abbr>RGB</abbr>,
     * but it may change in a future implementation if needed.
     *
     * <h4>Limitation</h4>
     * Note that {@link #createPackedRGB()} is limited to color spaces of the <abbr>RGB</abbr> family.
     * This is a restriction of {@link DirectColorModel}.
     */
    private static ColorSpace colorSpace() {
        return ColorSpace.getInstance(ColorSpace.CS_sRGB);
    }

    /**
     * Returns the data type to use for the given number of bits.
     */
    private static int dataType(final int numBits) {
        return (numBits <=  Byte.SIZE) ? DataBuffer.TYPE_BYTE :
               (numBits <= Short.SIZE) ? DataBuffer.TYPE_USHORT : DataBuffer.TYPE_INT;
    }

    /**
     * Creates a RGB color model for use with {@link SinglePixelPackedSampleModel}.
     * Pixel values are packed in a single integer (usually 32-bits) per pixel.
     * Color components are separated using a bitmask for each <abbr>RGBA</abbr> value.
     *
     * <h4>Limitations</h4>
     * The color model is restricted to the <abbr>RGB</abbr> family.
     *
     * @return color model for use with {@link java.awt.image.SinglePixelPackedSampleModel}.
     * @throws IllegalArgumentException if any argument specified to the builder is invalid.
     */
    public ColorModel createPackedRGB() {
        if (!isAlphaPremultiplied && alphaBand == STANDARD_ALPHA_BAND && hasDefaultBitsPerSample()) {
            return ColorModel.getRGBdefault();
        }
        // Red, Green, Blue, Alpha masks in that order.
        final int[] masks = new int[STANDARD_ALPHA_BAND + 1];
        int numBits = 0;
        for (int i=STANDARD_ALPHA_BAND - 1; i >= -1; i--) {
            int band = i;
            if (band < 0) {
                band = alphaBand;
                if (band < 0) break;
            }
            int n = getBitsPerSample(band, Math.min(Byte.SIZE, Integer.SIZE - numBits));
            masks[i] = ((1 << n) - 1) << numBits;
            numBits += n;
        }
        return ColorModelFactory.unique(new DirectColorModel(colorSpace(),
                numBits, masks[0], masks[1], masks[2], masks[3], isAlphaPremultiplied, dataType(numBits)));
    }

    /**
     * Creates a RGB color model for use with {@link BandedSampleModel}.
     * Each color component (sample value) is stored in a separated data element.
     *
     * <h4>Limitations</h4>
     * The current version requires the alpha channel (if present) to be the last band.
     * If this condition is not met, this method returns {@code null}.
     *
     * @return color model for use with {@link java.awt.image.BandedSampleModel}.
     * @throws IllegalArgumentException if any argument specified to the builder is invalid.
     */
    public ColorModel createBandedRGB() {
        final int numBands;
        final int transparency;
        final boolean hasAlpha = (alphaBand >= 0);
        if (hasAlpha) {
            if (alphaBand != STANDARD_ALPHA_BAND) {
                throw new IllegalArgumentException("Alpha channel must be after the color components.");
            }
            numBands = 4;
            transparency = Transparency.TRANSLUCENT;
        } else {
            numBands = 3;
            transparency = Transparency.OPAQUE;
        }
        int[] numBits = bitsPerSample;
        numBits = ArraysExt.resize(numBits != null ? numBits : ArraysExt.EMPTY_INT, numBands);
        int maxSize = 0;
        for (int i=0; i<numBands; i++) {
            if (numBits[i] == 0) {
                if (numBits == bitsPerSample) {
                    numBits = numBits.clone();
                }
                numBits[i] = Byte.SIZE;
            } else {
                maxSize = Math.max(maxSize, numBits[i]);
            }
        }
        return ColorModelFactory.unique(new ComponentColorModel(colorSpace(),
                numBits, hasAlpha, isAlphaPremultiplied, transparency, dataType(maxSize)));
    }

    /**
     * Creates a <abbr>RGB</abbr> color model for the given sample model.
     * The sample model shall use integer type and have 3 or 4 bands.
     * If no <abbr>RGB</abbr> or <abbr>ARGB</abbr> color model can be created,
     * this method default on a gray scale color model.
     *
     * @param  targetModel  the sample model for which to create a color model.
     * @return the <abbr>RGB</abbr> color model, or a gray scale color model as a fallback.
     * @throws IllegalArgumentException if any argument specified to the builder is invalid.
     */
    public ColorModel createRGB(final SampleModel targetModel) {
check:  if (ImageUtilities.isIntegerType(targetModel)) {
            final int numBands = targetModel.getNumBands();
            switch (numBands) {
                case 3:  alphaBand = -1; break;
                case 4:  alphaBand = STANDARD_ALPHA_BAND; break;
                default: break check;
            }
            bitsPerSample = targetModel.getSampleSize();
            if (targetModel.getNumDataElements() != 1) {
                return createBandedRGB();
            } else {
                for (int i=0; i<numBands; i++) {
                    if (bitsPerSample[i] > Byte.SIZE) {
                        break check;
                    }
                }
                return createPackedRGB();
            }
        }
        return ColorModelFactory.createGrayScale(targetModel, ColorModelFactory.DEFAULT_VISIBLE_BAND, null);
    }
}
