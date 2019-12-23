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

import java.util.Arrays;
import java.awt.Rectangle;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.PackedColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Utility methods related to images and their color model or sample model.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class ImageUtilities {
    /**
     * Do not allow instantiation of this class.
     */
    private ImageUtilities() {
    }

    /**
     * Returns the bounds of the given image as a new rectangle.
     *
     * @param  image  the image for which to get the bounds.
     * @return the bounds of the given image.
     *
     * @see Raster#getBounds()
     */
    public static Rectangle getBounds(final RenderedImage image) {
        return new Rectangle(image.getMinX(), image.getMinY(), image.getWidth(), image.getHeight());
    }

    /**
     * Returns names of bands based on inspection of the color model.
     * The bands are identified by {@link Vocabulary.Keys} values for
     * red, green, blue, cyan, magenta, yellow, black, gray, <i>etc</i>.
     * If a band can not be identified, then its corresponding value is 0.
     *
     * @param  image  the image for which to get band names (can not be null).
     * @return {@link Vocabulary.Keys} identifying the bands.
     */
    @SuppressWarnings("fallthrough")
    public static short[] bandNames(final RenderedImage image) {
        final SampleModel sm = image.getSampleModel();
        final int n;
        if (sm != null) {
            n = sm.getNumBands();
        } else {
            // Should not happen since SampleModel is essential, but we try to be robust.
            n = image.getTile(image.getMinTileX(), image.getMinTileY()).getNumBands();
        }
        final short[] keys = new short[n];
        final ColorModel cm = image.getColorModel();
        if (cm instanceof IndexColorModel) {
            /*
             * IndexColorModel normally uses exactly one band. But SIS has a custom subtype which
             * allows to use an arbitrary band for displaying purpose and ignore all other bands.
             */
            int visibleBand = 0;
            if (cm instanceof MultiBandsIndexColorModel) {
                visibleBand = ((MultiBandsIndexColorModel) cm).visibleBand;
            }
            if (visibleBand < n) {
                keys[visibleBand] = Vocabulary.Keys.ColorIndex;
            }
        } else if (cm != null) {
            final ColorSpace cs = cm.getColorSpace();
            if (cs != null) {
                /*
                 * Get one of the following sets of color names (ignoring order for now):
                 *
                 *   - Red, Green, Blue
                 *   - Cyan, Magenta, Yellow, Black
                 *   - Gray
                 */
                switch (cs.getType()) {
                    case ColorSpace.TYPE_CMYK: {
                        if (n >= 4)  keys[3] = Vocabulary.Keys.Black;
                        // Fallthrough
                    }
                    case ColorSpace.TYPE_CMY: {
                        switch (n) {
                            default: keys[2] = Vocabulary.Keys.Yellow;      // Fallthrough everywhere.
                            case 2:  keys[1] = Vocabulary.Keys.Magenta;
                            case 1:  keys[0] = Vocabulary.Keys.Cyan;
                            case 0:  break;
                        }
                        break;
                    }
                    case ColorSpace.TYPE_RGB: {
                        switch (n) {
                            default: keys[2] = Vocabulary.Keys.Blue;        // Fallthrough everywhere.
                            case 2:  keys[1] = Vocabulary.Keys.Green;
                            case 1:  keys[0] = Vocabulary.Keys.Red;
                            case 0:  break;
                        }
                        break;
                    }
                    case ColorSpace.TYPE_GRAY: {
                        if (n != 0)  keys[0] = Vocabulary.Keys.Gray;
                        break;
                    }
                }
                /*
                 * If the color model has more components than the number of colors,
                 * then the additional component is an alpha channel.
                 */
                final int nc = cm.getNumColorComponents();
                if (nc < n && nc < cm.getNumComponents()) {
                    keys[nc] = Vocabulary.Keys.Transparency;
                }
                /*
                 * In current version we do not try to adapt the bands order to the masks.
                 * A few tests suggest that the following methods provide the same values:
                 *
                 *   - PackedColorModel.getMasks()
                 *   - SinglePixelPackedSampleModel.getBitMasks()
                 *
                 * For a BufferedImage.TYPE_INT_ARGB, both methods give in that order:
                 *
                 *    masks[0]:  00FF0000     (red)
                 *    masks[1]:  0000FF00     (green)
                 *    masks[2]:  000000FF     (blue)
                 *    masks[3]:  FF000000     (alpha)  —  this last element is absent with TYPE_INT_RGB.
                 *
                 * For a BufferedImage.TYPE_INT_BGR, both methods give in that order:
                 *
                 *    masks[0]:  000000FF     (red)
                 *    masks[1]:  0000FF00     (green)
                 *    masks[2]:  00FF0000     (blue)
                 *
                 * So it looks like that SampleModel already normalizes the color components
                 * to (Red, Green, Blue) order, at least when the image has been created with
                 * a standard constructor. However we do not know yet what would be the behavior
                 * if masks are not the same. For now we just log a warning.
                 */
                int[] m1 = null;
                int[] m2 = null;
                if (cm instanceof PackedColorModel) {
                    m1 = ((PackedColorModel) cm).getMasks();
                }
                if (sm instanceof SinglePixelPackedSampleModel) {
                    m2 = ((SinglePixelPackedSampleModel) sm).getBitMasks();
                }
                if (!Arrays.equals(m1, m2)) {
                    // If this logging happen, we should revisit this method and improve it.
                    Logging.getLogger(Modules.RASTER).warning("Band names may be in wrong order.");
                }
            }
        }
        return keys;
    }
}
