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

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.image.DataType;
import org.apache.sis.measure.NumberRange;


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
     * Sentinel value for saying that {@link #alphaBand} has not been set.
     * This is different than setting {@code alphaBand} to -1, which means "no alpha".
     */
    private static final int UNSPECIFIED_ALPHA = -2;

    /**
     * The color space of the color model to build.
     * The default value is a <abbr>RGB</abbr> color space.
     *
     * <h4>Limitation</h4>
     * Note that {@link #createPackedRGB()} is limited to color spaces of the <abbr>RGB</abbr> family.
     * This is a restriction of {@link DirectColorModel}.
     */
    private final ColorSpace colorSpace;

    /**
     * The data type, or {@code null} for automatic.
     *
     * @see #dataType(DataType)
     * @see #dataType(int)
     */
    private DataType dataType;

    /**
     * Number of bits per sample, or {@code null} for the default values in all bands.
     * The array length is the number of bands: 3 for <abbr>RGB</abbr> or 4 for <abbr>ARGB</abbr>.
     * Each array element may be 0, which means to use the default for that specific band.
     * If no number of bits is specified, the default is {@value Byte#SIZE}.
     *
     * @see #bitsPerSample(int[])
     * @see #getBitsPerSample(int, int)
     */
    private int[] bitsPerSample;

    /**
     * Alternative to {@link #bitsPerSample} when all samples use the same number of bits.
     * Stored separately because the number of bands may not be known.
     * The default value is {@value Byte#SIZE}.
     *
     * @see #bitsPerSample(int)
     * @see #getBitsPerSample(int, int)
     */
    private int defaultBitsPerSample;

    /**
     * The red, green, blue and alpha masks, in that order. This is used only for {@link DirectColorModel},
     * where all sample values are packed in a single integer value.
     *
     * @see #componentMasks(int...)
     */
    private int[] componentMasks;

    /**
     * The band to show in the case of gray scale or indexed color model.
     * The default value is {@value ColorModelFactory#DEFAULT_VISIBLE_BAND}.
     *
     * @see #visibleBand(int)
     * @see ColorModelFactory#DEFAULT_VISIBLE_BAND
     */
    private int visibleBand;

    /**
     * Index of the alpha channel (usually the last band), or negative if none.
     */
    private int alphaBand;

    /**
     * Whether the alpha value (if present) is premultiplied.
     */
    private boolean isAlphaPremultiplied;

    /**
     * Range of the sample values in the {@link #visibleBand}, or {@code null} if unknown.
     * This is used for the gray scale fallback if the <abbr>ARGN</abbr> color model cannot be created.
     */
    private NumberRange<?> sampleValuesRange;

    /**
     * Creates a new builder initialized with default values for an <abbr>RGB</abbr> color space.
     */
    public ColorModelBuilder() {
        this(true);
    }

    /**
     * Creates a new builder initialized with default values.
     *
     * @param  isRGB  {@code true} for an <abbr>RGB</abbr> color space, or {@code false} for gray scale.
     */
    public ColorModelBuilder(final boolean isRGB) {
        colorSpace = ColorSpace.getInstance(isRGB ? ColorSpace.CS_sRGB : ColorSpace.CS_GRAY);
        defaultBitsPerSample = Byte.SIZE;
        alphaBand = UNSPECIFIED_ALPHA;
    }

    /**
     * Sets the type of data. If this method is not invoked,
     * the default is to determine the type automatically from the number of bits.
     *
     * @param  type  the data type to use, or {@code null} for automatic.
     * @return {@code this} for method calls chaining.
     */
    public ColorModelBuilder dataType(final DataType type) {
        dataType = type;
        return this;
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
        defaultBitsPerSample = Byte.SIZE;
        bitsPerSample = null;
        if (numBits != null) {
            for (int n : numBits) {
                if (n != Byte.SIZE) {
                    bitsPerSample = numBits;
                    break;
                }
            }
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
        defaultBitsPerSample = numBits;
        bitsPerSample = null;
        return this;
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
        return defaultBitsPerSample;
    }

    /**
     * Sets the red, green, blue and alpha masks, in that order. Those masks are used only for
     * {@link DirectColorModel}, where all sample values are packed in a single integer value.
     * Trailing zeros are trimmed, as a convenience for setting the mask of the alpha band to
     * zero when there is no alpha band.
     *
     * <p>The given array may be stored without copy on the assumption that is will not be modified
     * (this is okay for internal API). If no mask is specified, the default masks will be computed
     * from the {@code bitsPerSample} values.</p>
     *
     * @param  masks  the red, green, blue and alpha masks (in that order), or {@code null} for defaults.
     * @return {@code this} for method calls chaining.
     */
    public ColorModelBuilder componentMasks(int... masks) {
        if (masks != null) {
            for (int i = masks.length; --i >= 0;) {
                if (masks[i] != 0) {
                    masks = ArraysExt.resize(masks, i+1);
                    break;
                }
            }
        }
        componentMasks = masks;
        return this;
    }

    /**
     * Sets the band to show in the case of gray scale or indexed color model.
     * The default value is {@value ColorModelFactory#DEFAULT_VISIBLE_BAND}.
     *
     * @param  index  of the alpha channel (usually the first band).
     * @param  range  range of the sample values, or {@code null} if unknown.
     * @return {@code this} for method calls chaining.
     */
    public ColorModelBuilder visibleBand(final int index, final NumberRange<?> range) {
        ArgumentChecks.ensurePositive("visibleBand", index);
        visibleBand = index;
        sampleValuesRange = range;
        return this;
    }

    /**
     * Sets the index of the alpha channel. It should be the band immediately after the color bands,
     * but some color model allows other position.
     *
     * @param  index  of the alpha channel (usually the last band), or -1 if none.
     * @return {@code this} for method calls chaining.
     */
    public ColorModelBuilder alphaBand(final int index) {
        alphaBand = Math.max(index, UNSPECIFIED_ALPHA + 1);
        return this;
    }

    /**
     * Sets whether the alpha value (if present) is premultiplied.
     *
     * @param  p  whether the alpha value (if present) is premultiplied.
     * @return {@code this} for method calls chaining.
     */
    public ColorModelBuilder alphaPremultiplied(final boolean p) {
        isAlphaPremultiplied = p;
        return this;
    }

    /**
     * Returns the given array, or an empty array if the given array is null.
     */
    private static int[] orEmpty(final int[] numBits) {
        return (numBits != null) ? numBits : ArraysExt.EMPTY_INT;
    }

    /**
     * Returns the number of bands. If the user provided no indication,
     * a default value suitable for <abbr>RGB(A)</abbr> colors is returned.
     *
     * @return the number of bands.
     */
    private int numBands() {
        int numBands = Math.max(orEmpty(bitsPerSample).length, orEmpty(componentMasks).length);
        if (numBands == 0) {
            numBands = colorSpace.getNumComponents();
            if (alphaBand >= 0) {
                ArgumentChecks.ensureBetween("alphaBand", 0, numBands, alphaBand);
                numBands++;
            }
        }
        return numBands;
    }

    /**
     * Returns the data type to use for the given number of bits.
     * The number of bits is ignored if the user explicitly specified a data type.
     */
    private int dataType(final int numBits) {
        if (dataType != null) {
            return dataType.toDataBufferType();
        }
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
        int numBits = 0;
        int[] masks = componentMasks;                   // Red, Green, Blue, Alpha masks in that order.
        if (masks == null) {
            final int numBands = numBands();
            if (numBands == 4 && alphaBand == 3 && !isAlphaPremultiplied) {
                return ColorModel.getRGBdefault();      // Shared instance.
            }
            masks = new int[numBands];
            for (int i=numBands; --i >= -1;) {
                if (i != alphaBand) {
                    int band = i;
                    if (band < 0) {
                        band = alphaBand;
                        if (band < 0) break;
                    }
                    int n = getBitsPerSample(band, Math.min(Byte.SIZE, Integer.SIZE - numBits));
                    masks[i] = ((1 << n) - 1) << numBits;
                    numBits += n;
                }
            }
        } else {
            for (int i = masks.length; --i >= 0;) numBits |= masks[i];
            numBits = Integer.SIZE - Integer.numberOfLeadingZeros(numBits);
        }
        return ColorModelFactory.unique(new DirectColorModel(colorSpace, numBits,
                masks[0], masks[1], masks[2], masks[3], isAlphaPremultiplied, dataType(numBits)));
    }

    /**
     * Creates a RGB color model for use with {@link ComponentSampleModel}.
     * Each color component (sample value) is stored in a separated data element.
     * Note that "banded" is taken in a loose sense here, as the data do not need
     * to be stored in separated arrays as long as the components are distinct elements.
     *
     * <h4>Limitations</h4>
     * The current version requires the alpha channel (if present) to be the last band.
     * If this condition is not met, this method returns {@code null}.
     *
     * @return color model for use with {@link ComponentSampleModel}.
     * @throws IllegalArgumentException if any argument specified to the builder is invalid.
     */
    public ColorModel createBandedRGB() {
        final int numBands = numBands();
        final boolean hasAlpha = (alphaBand >= 0);
        if (hasAlpha && alphaBand != numBands - 1) {
            throw new IllegalArgumentException("Alpha channel must be after the color components.");
        }
        int[] numBits = ArraysExt.resize(orEmpty(bitsPerSample), numBands);
        int maxSize = 0;
        for (int i=0; i<numBands; i++) {
            if (numBits[i] == 0) {
                if (numBits == bitsPerSample) {
                    numBits = numBits.clone();
                }
                numBits[i] = defaultBitsPerSample;
            }
            maxSize = Math.max(maxSize, numBits[i]);
        }
        return ColorModelFactory.unique(new ComponentColorModel(colorSpace,
                numBits, hasAlpha, isAlphaPremultiplied,
                hasAlpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE,
                dataType(maxSize)));
    }

    /**
     * Creates a <abbr>RGB</abbr> or gray scale color model for the given sample model.
     * If this method does not know how to create the requested color model,
     * it defaults on a gray scale color model.
     *
     * @param  targetModel  the sample model for which to create a color model.
     * @return the <abbr>RGB</abbr> color model, or a gray scale color model as a fallback.
     * @throws IllegalArgumentException if any argument specified to the builder is invalid.
     */
    public ColorModel createRGB(final SampleModel targetModel) {
check:  if (DataType.isInteger(targetModel) && colorSpace.getType() == ColorSpace.TYPE_RGB) {
            final int numBands = targetModel.getNumBands();
            if (alphaBand == UNSPECIFIED_ALPHA) {
                switch (numBands) {
                    case 3:  alphaBand = -1; break;
                    case 4:  alphaBand =  3; break;
                    default: break check;
                }
            }
            if (bitsPerSample == null) {
                bitsPerSample(targetModel.getSampleSize());
            }
            if (targetModel.getNumDataElements() != 1) {
                return createBandedRGB();
            } else {
                for (int i=0; i<numBands; i++) {
                    if (getBitsPerSample(i, Integer.SIZE) > Byte.SIZE) {
                        break check;
                    }
                }
                return createPackedRGB();
            }
        }
        return ColorModelFactory.createGrayScale(targetModel, visibleBand, sampleValuesRange);
    }
}
