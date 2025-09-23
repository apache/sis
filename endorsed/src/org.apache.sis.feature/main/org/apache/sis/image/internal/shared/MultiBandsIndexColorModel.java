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
import java.awt.image.Raster;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.IndexColorModel;
import java.awt.image.BandedSampleModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.SinglePixelPackedSampleModel;


/**
 * An {@link IndexColorModel} tolerant with image having more than one band.
 * This class can support only the types supported by {@code IndexColorModel}
 * parent class. As of Java 14 they are restricted to {@link DataBuffer#TYPE_BYTE}
 * and {@code DataBuffer#TYPE_USHORT}.
 *
 * <p><b>Reminder:</b> {@link #getNumComponents()} will return 3 or 4 no matter
 * how many bands were specified to the constructor. This is not specific to this class;
 * {@code IndexColorModel} behave that way. So we cannot rely on this method for checking
 * the number of bands.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Andrea Aime (TOPP)
 */
final class MultiBandsIndexColorModel extends IndexColorModel {
    /**
     * The number of bands.
     */
    final int numBands;

    /**
     * The visible band.
     */
    final int visibleBand;

    /**
     * Constructs an index color model with the specified properties.
     *
     * @param bits          the number of bits each pixel occupies.
     * @param size          the size of the color component arrays.
     * @param cmap          the array of color components.
     * @param start         the starting offset of the first color component.
     * @param hasAlpha      indicates whether alpha values are contained in the {@code cmap} array.
     * @param transparent   The index of the fully transparent pixel.
     * @param transferType  The data type of the array used to represent pixel values.
     * @param numBands      the number of bands.
     * @param visibleBand   the band to display.
     *
     * @throws IllegalArgumentException if {@code bits} is less than 1 or greater than 16.
     * @throws IllegalArgumentException if {@code size} is less than 1.
     * @throws IllegalArgumentException if {@code transferType} is not one of
     *         {@code DataBuffer.TYPE_BYTE} or {@code DataBuffer.TYPE_USHORT}.
     */
    public MultiBandsIndexColorModel(final int bits,
                                     final int size,
                                     final int[] cmap,
                                     final int start,
                                     final boolean hasAlpha,
                                     final int transparent,
                                     final int transferType,
                                     final int numBands,
                                     final int visibleBand)
    {
        super(bits, size, cmap, start, hasAlpha, transparent, transferType);
        this.numBands    = numBands;
        this.visibleBand = visibleBand;
    }

    /**
     * Returns a color model with a different number of bands and a different visible band.
     *
     * <p><b>Note:</b> the new color model will use a copy of the color map of this color model.
     * There is no way to share the {@code int[]} array of ARGB values between two {@link IndexColorModel}s.</p>
     *
     * @param  numBands     new number of bands.
     * @param  visibleBand  new visible band.
     * @return a color model with the same colors but the specified number of bands.
     */
    final IndexColorModel derive(final int numBands, final int visibleBand) {
        if (numBands == this.numBands && visibleBand == this.visibleBand) {
            return this;
        }
        final int[]   cmap        = getARGB();
        final boolean hasAlpha    = hasAlpha();
        final int     transparent = getTransparentPixel();
        if (numBands == 1) {
            return new IndexColorModel(pixel_bits, cmap.length, cmap, 0, hasAlpha, transparent, transferType);
        }
        return new MultiBandsIndexColorModel(pixel_bits, cmap.length, cmap, 0,
                    hasAlpha, transparent, transferType, numBands, visibleBand);
    }

    /**
     * Returns all ARGB colors in this color model.
     * This is a copy of the {@code cmap} used by this color model.
     */
    private int[] getARGB() {
        final int[] cmap = new int[getMapSize()];
        for (int i=0; i<cmap.length; i++) {
            cmap[i] = getRGB(i);
        }
        return cmap;
    }

    /**
     * Converts a RGB color to a representation of a pixel in this color model.
     * This method returns an array with a length equal to the number of bands specified to
     * the constructor ({@code IndexColorModel} would returns an array of length 1). All array
     * elements are set to the same value. Replicating the pixel value is a somewhat arbitrary
     * choice, but this choice makes this image appears as a gray scale image if the underlying
     * {@link DataBuffer} were displayed again with a RGB color model instead of this one. Such
     * a gray scale image seems more neutral than an image where only the Red component would vary.
     *
     * <p>All other {@code getDataElement(…)} methods in this color model are ultimately defined
     * in terms of this method, so overriding this method if needed should be enough.</p>
     */
    @Override
    public Object getDataElements(final int RGB, Object pixel) {
        if (pixel == null) {
            switch (transferType) {
                case DataBuffer.TYPE_SHORT:  // Handled as a matter of principle.
                case DataBuffer.TYPE_USHORT: pixel = new short[numBands]; break;
                case DataBuffer.TYPE_BYTE:   pixel = new byte [numBands]; break;
                case DataBuffer.TYPE_INT:    pixel = new int  [numBands]; break;
            }
        }
        pixel = super.getDataElements(RGB, pixel);
        switch (transferType) {
            case DataBuffer.TYPE_BYTE: {
                final byte[] array = (byte[]) pixel;
                Arrays.fill(array, 1, numBands, array[0]);
                break;
            }
            case DataBuffer.TYPE_SHORT:      // Handled as a matter of principle.
            case DataBuffer.TYPE_USHORT: {
                final short[] array = (short[]) pixel;
                Arrays.fill(array, 1, numBands, array[0]);
                break;
            }
            case DataBuffer.TYPE_INT: {
                final int[] array = (int[]) pixel;
                Arrays.fill(array, 1, numBands, array[0]);
                break;
            }
        }
        return pixel;
    }

    /**
     * Returns the pixel value as an integer.
     */
    private int pixel(final Object inData) {
        switch (transferType) {
            case DataBuffer.TYPE_BYTE:   return  Byte.toUnsignedInt( ((byte[]) inData)[visibleBand]);
            case DataBuffer.TYPE_USHORT: return Short.toUnsignedInt(((short[]) inData)[visibleBand]);
            case DataBuffer.TYPE_SHORT:  return                     ((short[]) inData)[visibleBand];
            case DataBuffer.TYPE_INT:    return                       ((int[]) inData)[visibleBand];
            default: throw new UnsupportedOperationException();
        }
    }

    /**
     * Returns the number of bits per pixel described by this color model.
     * Must return a value different then {@code super.getPixelSize()} for avoiding that
     * {@link IndexColorModel#equals(Object)} consider this color model equal to a standard
     * {@code IndexColorModel} with the same color palette.
     */
    @Override
    public int getPixelSize() {
        return pixel_bits * numBands;
    }

    /**
     * Returns an array of unnormalized color/alpha components for a specified pixel in this color model.
     * This method is the converse of {@link #getDataElements(int, Object)}.
     */
    @Override
    public int[] getComponents(final Object pixel, final int[] components, final int offset) {
        return getComponents(pixel(components), components, offset);
    }

    /**
     * Returns the red color component for the specified pixel,
     * scaled from 0 to 255 in the default RGB {@code ColorSpace}.
     */
    @Override
    public int getRed(final Object inData) {
        return getRed(pixel(inData));
    }

    /**
     * Returns the green color component for the specified pixel,
     * scaled from 0 to 255 in the default RGB {@code ColorSpace}.
     */
    @Override
    public int getGreen(final Object inData) {
        return getGreen(pixel(inData));
    }

    /**
     * Returns the blue color component for the specified pixel,
     * scaled from 0 to 255 in the default RGB {@code ColorSpace}.
     */
    @Override
    public int getBlue(final Object inData) {
        return getBlue(pixel(inData));
    }

    /**
     * Returns the alpha component for the specified pixel, scaled from 0 to 255.
     */
    @Override
    public int getAlpha(final Object inData) {
        return getAlpha(pixel(inData));
    }

    /**
     * Creates a {@code WritableRaster} with the specified width and height that has
     * a data layout ({@code SampleModel}) compatible with this {@code ColorModel}.
     *
     * The difference with standard implementation is that this method creates a banded raster on the assumption that
     * the number of bands is greater than 1. By contrast, the standard implementation provides various optimizations
     * for one-banded raster.
     */
    @Override
    public WritableRaster createCompatibleWritableRaster(final int width, final int height) {
        return Raster.createWritableRaster(createCompatibleSampleModel(width, height), null);
    }

    /**
     * Creates a {@code SampleModel} with the specified width and height
     * that has a data layout compatible with this {@code ColorModel}.
     */
    @Override
    public SampleModel createCompatibleSampleModel(final int width, final int height) {
        final SampleModel sm;
        if (pixel_bits <= DataBuffer.getDataTypeSize(transferType) / numBands) {
            final int[] bitMasks = new int[numBands];
            Arrays.fill(bitMasks, pixel_bits);
            sm = new SinglePixelPackedSampleModel(transferType, width, height, width, bitMasks);
        } else {
            sm = new BandedSampleModel(transferType, width, height, numBands);
        }
        return RasterFactory.unique(sm);
    }

    /**
     * Returns {@code true} if {@code raster} is compatible with this {@code ColorModel}.
     * This method performs the same checks as the standard implementation except for the number of bands,
     * which is required to be equal to {@link #numBands} instead of 1. The actual checks are delegated to
     * {@link #isCompatibleSampleModel(SampleModel)} instead of duplicated in this method.
     */
    @Override
    public boolean isCompatibleRaster(final Raster raster) {
        return isCompatibleSampleModel(raster.getSampleModel());
    }

    /**
     * Checks if the specified {@code SampleModel} is compatible with this {@code ColorModel}.
     * This method performs the same checks as the standard implementation except for the number
     * of bands and for not accepting {@code MultiPixelPackedSampleModel}.
     */
    @Override
    public boolean isCompatibleSampleModel(final SampleModel sm) {
        return (sm instanceof ComponentSampleModel || sm instanceof SinglePixelPackedSampleModel) &&
                sm.getTransferType()                 == transferType &&
                sm.getNumBands()                     == numBands     &&
                (1 << sm.getSampleSize(visibleBand)) >= getMapSize();
    }

    /**
     * Compares this color model with the given object for equality.
     *
     * @param  obj  the other object to compare with this color model.
     * @return whether both object are equal.
     */
    @Override
    public boolean equals(final Object obj) {
         if (obj instanceof MultiBandsIndexColorModel) {
             final MultiBandsIndexColorModel other = (MultiBandsIndexColorModel) obj;
             return numBands == other.numBands && visibleBand == other.visibleBand && super.equals(other);
         }
         return false;
    }

    /**
     * Returns a hash code value for this color model.
     *
     * @return a hash code value.
     */
    @Override
    public int hashCode() {
        return super.hashCode() + 31 * numBands + 37 * visibleBand;
    }
}
