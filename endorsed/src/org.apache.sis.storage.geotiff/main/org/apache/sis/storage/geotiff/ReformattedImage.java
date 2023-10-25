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
package org.apache.sis.storage.geotiff;

import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import static javax.imageio.plugins.tiff.BaselineTIFFTagSet.*;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.math.Statistics;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.coverage.grid.j2d.ImageUtilities;
import org.apache.sis.storage.IncompatibleResourceException;


/**
 * An image prepared for writing with bands separated in the way they are stored in a TIFF file.
 * The TIFF specification stores visible bands, alpha channel and extra bands separately.
 *
 * @todo Force tile size to multiple of 16.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ReformattedImage {
    /**
     * The main image with visible bands.
     */
    final RenderedImage visibleBands;

    /*
     * TODO: alpha and extra bands not yet stored.
     * This will be handled in a future version.
     */

    /**
     * Formats the given image into something that can be written in a GeoTIFF file.
     * The visible image will have at most 3 bands and should have no alpha channel.
     * If no change is needed, then the given image is used unchanged.
     *
     * @param  writer  the writer for which to separate in image.
     * @param  image   the image to separate into visible, alpha and extra bands.
     */
    ReformattedImage(final Writer writer, final RenderedImage image) {
        final int numBands = ImageUtilities.getNumBands(image);
select: if (numBands > 1) {
            final int[] bands;
            final int band = ImageUtilities.getVisibleBand(image);
            if (band >= 0) {
                bands = new int[] {band};
            } else {
                int max = 3;                                // TIFF can store only 3 bands (ignoring extra bands).
                if (ImageUtilities.hasAlpha(image)) {
                    max = Math.min(max, numBands - 1);      // The alpha band is always the last one.
                }
                if (numBands <= max) {
                    break select;
                }
                bands = ArraysExt.range(0, max);
            }
            visibleBands = writer.processor().selectBands(image, bands);
            return;
        }
        visibleBands = image;
    }

    /**
     * Returns statistics about pixel values in the visible bands.
     * This method does not scan pixel values if statistics are not already present.
     *
     * @param  numbands  number of bands to retain.
     * @return statistics in an array of length 2, with minimums first then maximums.
     *         Array elements may be {@code null} if there is no statistics.
     */
    final double[][] statistics(final int numBands) {
        final Object property = visibleBands.getProperty(PlanarImage.STATISTICS_KEY);
found:  if (property instanceof Statistics[]) {
            final var stats = (Statistics[]) property;
            final var min = new double[numBands];
            final var max = new double[numBands];
            for (int i=0; i<numBands; i++) {
                final Statistics s = stats[i];
                if (s.count() == 0) break found;
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
    final int getSampleFormat() {
        final SampleModel sm = visibleBands.getSampleModel();
        if (ImageUtilities.isUnsignedType(sm)) return SAMPLE_FORMAT_UNSIGNED_INTEGER;
        if (ImageUtilities.isIntegerType(sm))  return SAMPLE_FORMAT_SIGNED_INTEGER;
        return SAMPLE_FORMAT_FLOATING_POINT;
    }

    /**
     * Returns the TIFF color interpretation.
     *
     * @return One of {@code PHOTOMETRIC_INTERPRETATION_*} constants.
     * @throws IncompatibleResourceException if the color model is not supported.
     */
    final int getColorInterpretation() throws IncompatibleResourceException {
        final ColorModel  cm = visibleBands.getColorModel();
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
        switch (cm.getColorSpace().getType()) {
            case ColorSpace.TYPE_GRAY: {
                return PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO;
            }
            case ColorSpace.TYPE_RGB: {
                return PHOTOMETRIC_INTERPRETATION_RGB;
            }
            default: {
                // A future version may add support for more color models.
                throw new IncompatibleResourceException("Unsupported color model");
            }
        }
    }
}
