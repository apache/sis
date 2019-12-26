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
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.PackedColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import org.apache.sis.internal.feature.Resources;
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
     * Creates an opaque image with a gray scale color model. The image can have an arbitrary
     * number of bands, but in current implementation only one band is used.
     *
     * <p><b>Warning:</b> displaying this image is very slow, except in a few special cases.
     * It should be used only when no standard color model can be used.</p>
     *
     * @param  dataType       the color model type as one of {@code DataBuffer.TYPE_*} constants.
     * @param  width          the desired image width.
     * @param  height         the desired image height.
     * @param  numComponents  the number of components.
     * @param  visibleBand    the band to use for computing colors.
     * @param  minimum        the minimal sample value expected.
     * @param  maximum        the maximal sample value expected.
     * @return the color space for the given range of values.
     */
    public static BufferedImage createGrayScale(final int dataType, final int width, final int height,
            final int numComponents, final int visibleBand, final double minimum, final double maximum)
    {
        switch (dataType) {
            case DataBuffer.TYPE_BYTE: {
                if (numComponents == 1 && minimum <= 0 && maximum >= 0xFF) {
                    return new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
                }
                break;
            }
            case DataBuffer.TYPE_USHORT: {
                if (numComponents == 1 && minimum <= 0 && maximum >= 0xFFFF) {
                    return new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
                }
                break;
            }
        }
        final ColorModel cm = ColorModelFactory.createGrayScale(DataBuffer.TYPE_INT, 1, 0, -10, 10);
        return new BufferedImage(cm, cm.createCompatibleWritableRaster(width, height), false, null);
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
     * Names of {@link DataBuffer} types.
     */
    private static final String[] TYPE_NAMES = new String[DataBuffer.TYPE_DOUBLE + 1];
    static {
        TYPE_NAMES[DataBuffer.TYPE_BYTE]   = "byte";
        TYPE_NAMES[DataBuffer.TYPE_SHORT]  = "short";
        TYPE_NAMES[DataBuffer.TYPE_USHORT] = "ushort";
        TYPE_NAMES[DataBuffer.TYPE_INT]    = "int";
        TYPE_NAMES[DataBuffer.TYPE_FLOAT]  = "float";
        TYPE_NAMES[DataBuffer.TYPE_DOUBLE] = "double";
    }

    /**
     * Returns the name of the {@link DataBuffer} type used by the given sample model.
     *
     * @param  sm  the sample model for which to get the data type name, or {@code null}.
     * @return name of the given constant, or {@code null} if unknown.
     */
    public static String getDataTypeName(final SampleModel sm) {
        if (sm != null) {
            final int type = sm.getDataType();
            if (type >= 0 && type < TYPE_NAMES.length) {
                return TYPE_NAMES[type];
            }
        }
        return null;
    }

    /**
     * Returns the key of a localizable text that describes the transparency.
     * This method returns one of the following values:
     * <ul>
     *   <li>{@link Resources.Keys#ImageAllowsTransparency}</li>
     *   <li>{@link Resources.Keys#ImageHasAlphaChannel}</li>
     *   <li>{@link Resources.Keys#ImageIsOpaque}</li>
     *   <li>0 if the transparency is unknown.</li>
     * </ul>
     *
     * @param  cm  the color model from which to get the transparency, or {@code null}.
     * @return a {@link Resources.Keys} value for the transparency, or 0 if unknown.
     */
    public static short getTransparencyDescription(final ColorModel cm) {
        if (cm != null) {
            if (cm.hasAlpha()) {
                return Resources.Keys.ImageHasAlphaChannel;
            }
            switch (cm.getTransparency()) {
                case ColorModel.TRANSLUCENT:
                case ColorModel.BITMASK: return Resources.Keys.ImageAllowsTransparency;
                case ColorModel.OPAQUE:  return Resources.Keys.ImageIsOpaque;
            }
        }
        return 0;
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
