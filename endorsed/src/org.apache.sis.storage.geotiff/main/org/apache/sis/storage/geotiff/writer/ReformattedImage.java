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
package org.apache.sis.storage.geotiff.writer;

import java.util.function.Supplier;
import java.awt.Dimension;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import static javax.imageio.plugins.tiff.BaselineTIFFTagSet.*;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.math.Statistics;
import org.apache.sis.pending.jdk.JDK18;
import org.apache.sis.image.ImageLayout;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.coverage.privy.ImageUtilities;
import org.apache.sis.coverage.privy.SampleModelFactory;
import org.apache.sis.io.stream.HyperRectangleWriter;


/**
 * An image prepared for writing in a TIFF file. The TIFF specification puts some restrictions on tile size
 * (must be a multiple of 16), and {@code HyperRectangleWriter} has some other restrictions on data layout.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ReformattedImage {
    /**
     * Divisor of tile sizes mandated by the TIFF specification.
     * All tile sizes must be a multiple of this value.
     * This constant must be a power of 2.
     */
    private static final int TILE_DIVISOR = 16;

    /**
     * Number of color bands before the extra bands. This number does not include the alpha channel,
     * which is considered an extra band in TIFF. This number does not apply to gray scale images,
     * which use only one band.
     */
    private static final int NUM_COLOR_BANDS = 3;

    /**
     * Whether the set of color bands includes only one band: gray scale or color palette.
     * If {@code false}, then the number of color bands is {@value #NUM_COLOR_BANDS}.
     * Alpha channel and extra components are ignored.
     */
    private final boolean singleBand;

    /**
     * The values to write in the {@code ExtraSamples} TIFF tag, or {@code null} if none.
     * Values are some of the {@code EXTRA_SAMPLES_*} constants.
     * The alpha channel, if presents, is declared here.
     */
    public short[] extraSamples;

    /**
     * The image reformatted in a way that can be written in a TIFF file.
     */
    public final RenderedImage exportable;

    /**
     * Formats the given image into something that can be written in a GeoTIFF file.
     * The visible image will have at most 3 bands and should have no alpha channel.
     * If no change is needed, then the given image is used unchanged.
     *
     * @param  image        the image to separate into visible, alpha and extra bands.
     * @param  processor    supplier of the image processor to use if the image must be transformed.
     * @param  anyTileSize  whether to disable the <abbr>TIFF</abbr> requirement that tile sizes are multiple of 16 pixels.
     */
    public ReformattedImage(RenderedImage image, final Supplier<ImageProcessor> processor, final boolean anyTileSize) {
        int alphaBand = -1;
        boolean isAlphaPremultiplied = false;
        final int numBands = ImageUtilities.getNumBands(image);
        final int visibleBand = ImageUtilities.getVisibleBand(image);
        final boolean banded;   // Whether to prefer `BandedSampleModel`.
        if (visibleBand >= 0) {
            /*
             * Indexed color model or gray scale image. This is conceptually a single band.
             * But as an extension, Apache SIS allows the image to contain additional bands
             * where only one band is shown. We need to order the visible band first.
             * All other bands will be extra samples.
             */
            banded = true;
            singleBand = true;
            if (visibleBand != 0) {
                final int[] bands = ArraysExt.range(0, numBands);
                System.arraycopy(bands, 0, bands, 1, visibleBand);
                bands[0] = visibleBand;
                image = processor.get().selectBands(image, bands);      // Reorder bands without copy.
            }
        } else {
            /*
             * Any case with 2 or more bands. GeoTIFF wants us to store either 1 or 3 bands.
             * So if we cannot store 3 bands (excluding alpha channel), we will handle the
             * image as gray scale.
             */
            final ColorModel cm = image.getColorModel();
            if (cm != null) {
                isAlphaPremultiplied = cm.isAlphaPremultiplied();
                alphaBand = cm.getNumColorComponents();     // Alpha channel is right after color components.
                if (alphaBand >= cm.getNumComponents()) {
                    alphaBand = -1;
                }
            }
            final int expected = (alphaBand < 0 ? NUM_COLOR_BANDS : NUM_COLOR_BANDS + 1);
            banded = (numBands != expected);
            singleBand = numBands < expected;
        }
        /*
         * Check if there is any extra samples. If yes, prepare an `extraSamples` array with all
         * values initialized to `EXTRA_SAMPLES_UNSPECIFIED` (value 0) except for the alpha channel.
         */
        final int numColors = singleBand ? 1 : Math.min(numBands, NUM_COLOR_BANDS);
        if (numBands > numColors) {
            extraSamples = new short[numBands - numColors];
            if (alphaBand >= 0) {
                extraSamples[alphaBand - numColors] = isAlphaPremultiplied
                        ? (short) EXTRA_SAMPLES_ASSOCIATED_ALPHA
                        : (short) EXTRA_SAMPLES_UNASSOCIATED_ALPHA;
            }
        }
        /*
         * If the image cannot be written directly, reformat. Because this operation
         * forces the copy of pixel values, it should be executed in last resort.
         */
        boolean reformat = false;
        SampleModel sm = image.getSampleModel();
        if (!HyperRectangleWriter.Builder.isSupported(sm)) {
            final var factory = new SampleModelFactory(sm);
            if (factory.unpack(banded)) {
                sm = factory.build();
                reformat = true;
            }
        }
        /*
         * Force the image tile size to multiple of 16 pixels. This is a requirement of the TIFF specification.
         * We can ignore this requirement when the image is untiled on the X axis, because in such case the SIS
         * writer will write the image as strips instead of as tiles.
         */
        if (!anyTileSize && image.getNumXTiles() != 1) {
            var tileSize = new Dimension(image.getTileWidth(), image.getTileHeight());
            if (((tileSize.width | tileSize.height) & (TILE_DIVISOR - 1)) != 0) {
                final int width  = JDK18.ceilDiv(image.getWidth(),  TILE_DIVISOR);
                final int height = JDK18.ceilDiv(image.getHeight(), TILE_DIVISOR);
                tileSize.width   = JDK18.ceilDiv(tileSize.width,    TILE_DIVISOR);
                tileSize.height  = JDK18.ceilDiv(tileSize.height,   TILE_DIVISOR);
                tileSize = ImageLayout.DEFAULT.withPreferredTileSize(tileSize).suggestTileSize(width, height);
                tileSize.width  *= TILE_DIVISOR;
                tileSize.height *= TILE_DIVISOR;
                sm = sm.createCompatibleSampleModel(tileSize.width, tileSize.height);
                reformat = true;
            }
        }
        if (reformat) {
            image = processor.get().reformat(image, sm);
        }
        exportable = image;
    }

    /**
     * Returns statistics about pixel values in the visible bands.
     * This method does not scan pixel values if statistics are not already present.
     *
     * @param  numbands  number of bands to retain.
     * @return statistics in an array of length 2, with minimums first then maximums.
     *         Array elements may be {@code null} if there is no statistics.
     */
    public double[][] statistics(final int numBands) {
        final Object property = exportable.getProperty(PlanarImage.STATISTICS_KEY);
found:  if (property instanceof Statistics[]) {
            final var stats = (Statistics[]) property;
            final var min = new double[numBands];
            final var max = new double[numBands];
            for (int i=0; i<numBands; i++) {
                final Statistics s = stats[i];
                if (s == null || s.count() == 0) {
                    break found;    // Some statistics are missing.
                }
                min[i] = s.minimum();
                max[i] = s.maximum();
            }
            return new double[][] {min, max};
        }
        return new double[2][];
    }

    /**
     * Returns the TIFF sample format.
     *
     * @return One of {@code SAMPLE_FORMAT_*} constants.
     */
    public int getSampleFormat() {
        final SampleModel sm = exportable.getSampleModel();
        if (ImageUtilities.isUnsignedType(sm)) return SAMPLE_FORMAT_UNSIGNED_INTEGER;
        if (ImageUtilities.isIntegerType(sm))  return SAMPLE_FORMAT_SIGNED_INTEGER;
        return SAMPLE_FORMAT_FLOATING_POINT;
    }

    /**
     * Returns the TIFF color interpretation.
     *
     * @return One of {@code PHOTOMETRIC_INTERPRETATION_*} constants.
     */
    public int getColorInterpretation() {
        final ColorModel cm = exportable.getColorModel();
        if (singleBand) {
            if (cm instanceof IndexColorModel) {
                final var   icm   = (IndexColorModel) cm;
                final int   last  = icm.getMapSize() - 1;
                final float scale = 255f / last;
                boolean white = true;
                boolean black = true;
                for (int i=0; i <= last; i++) {
                    final int expected = Math.round(i * scale);
                    if (black) black = icm.getRGB(     i) == expected;
                    if (white) white = icm.getRGB(last-i) == expected;
                    if (!(black | white)) break;
                }
                if (black) return PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO;
                if (white) return PHOTOMETRIC_INTERPRETATION_WHITE_IS_ZERO;
                return PHOTOMETRIC_INTERPRETATION_PALETTE_COLOR;
            }
            return PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO;
        }
        if (cm != null) {
            /*
             * All color interpretations returned in this block shall expect 3 bands.
             * Gray scale images are handled as RGB (should not be a concern, because
             * they should have been handled in the previous block). If a color space
             * has 4 or more components, all components after 3 are extra samples.
             */
            final ColorSpace cs = cm.getColorSpace();
            if (cs != null) {
                switch (cs.getType()) {
                    //   ColorSpace.TYPE_GRAY:  should never happen here.
                    case ColorSpace.TYPE_RGB:   return PHOTOMETRIC_INTERPRETATION_RGB;
                    case ColorSpace.TYPE_CMY:   return PHOTOMETRIC_INTERPRETATION_CMYK;    // Black stored as extra sample.
                    case ColorSpace.TYPE_Lab:   return PHOTOMETRIC_INTERPRETATION_CIELAB;
                    case ColorSpace.TYPE_YCbCr: return PHOTOMETRIC_INTERPRETATION_Y_CB_CR;
                    // A future version may add support for more color models.
                }
            }
        }
        return PHOTOMETRIC_INTERPRETATION_RGB;      // Default value for unknown color space.
    }
}
