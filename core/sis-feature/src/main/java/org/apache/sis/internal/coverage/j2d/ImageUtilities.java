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
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.PackedColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.awt.image.RasterFormatException;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import org.apache.sis.internal.feature.Resources;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.util.Static;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Utility methods related to images and their color model or sample model.
 * Those methods only fetch information, they do not create new rasters or sample/color models
 * (see {@code *Factory} classes for creating those objects).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class ImageUtilities extends Static {
    /**
     * Default width and height of tiles, in pixels.
     */
    public static final int DEFAULT_TILE_SIZE = 256;

    /**
     * Suggested size for a tile cache in number of tiles. This value can be used for very simple caching mechanism,
     * keeping the most recently used tiles up to 10 Mb of memory. This is not for sophisticated caching mechanism;
     * instead the "real" caching should be done by {@link org.apache.sis.image.ComputedImage}.
     */
    public static final int SUGGESTED_TILE_CACHE_SIZE = 10 * (1024 * 1024) / (DEFAULT_TILE_SIZE * DEFAULT_TILE_SIZE);

    /**
     * Approximate size of the buffer to use for copying data from/to a raster, in bits.
     * The actual buffer size may be smaller or larger, depending on the actual tile size.
     * This value does not need to be very large. The current value is 8 kb.
     *
     * @see #prepareTransferRegion(Rectangle, int)
     */
    private static final int BUFFER_SIZE = 32 * DEFAULT_TILE_SIZE * Byte.SIZE;

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
     * Clips the given rectangle to the bounds of the given image.
     *
     * @param  image  the image.
     * @param  aoi    a region of interest to clip to the image bounds.
     */
    public static void clipBounds(final RenderedImage image, final Rectangle aoi) {
        int low = aoi.x;
        int min = image.getMinX();
        if (low < min) aoi.x = min;
        aoi.width = Numerics.clamp(Math.min(
                ((long) min) + image.getWidth(),
                ((long) low) + aoi.width) - aoi.x);

        low = aoi.y;
        min = image.getMinY();
        if (low < min) aoi.y = min;
        aoi.height = Numerics.clamp(Math.min(
                ((long) min) + image.getHeight(),
                ((long) low) + aoi.height) - aoi.y);
    }

    /**
     * Returns the data type of the given image.
     *
     * @param  image  the image for which to get the data type, or {@code null}.
     * @return the data type, or {@link DataBuffer#TYPE_UNDEFINED} if unknown.
     */
    public static int getDataType(final RenderedImage image) {
        if (image != null) {
            final SampleModel sm = image.getSampleModel();
            if (sm != null) return sm.getDataType();            // Should never be null, but we are paranoiac.
        }
        return DataBuffer.TYPE_UNDEFINED;
    }

    /**
     * Returns the data type of the given raster.
     *
     * @param  raster  the raster for which to get the data type, or {@code null}.
     * @return the data type, or {@link DataBuffer#TYPE_UNDEFINED} if unknown.
     */
    public static int getDataType(final Raster raster) {
        if (raster != null) {
            final DataBuffer buffer = raster.getDataBuffer();
            if (buffer != null) {
                return buffer.getDataType();
            }
        }
        return DataBuffer.TYPE_UNDEFINED;
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

    /**
     * Suggests the height of a transfer region for a tile of the given size. The given region should be
     * contained inside {@link Raster#getBounds()}. This method modifies {@link Rectangle#height} in-place.
     * The {@link Rectangle#width} value is never modified, so caller can iterate on all raster rows without
     * the need to check if the row is incomplete.
     *
     * @param  bounds    on input, the region of interest. On output, the suggested transfer region bounds.
     * @param  dataType  one of {@link DataBuffer} constant. It is okay if an unknown constant is used since
     *                   this information is used only as a hint for adjusting the {@link #BUFFER_SIZE} value.
     * @return the maximum <var>y</var> value plus 1. This can be used as stop condition for iterating over rows.
     * @throws ArithmeticException if the maximum <var>y</var> value overflows 32 bits integer capacity.
     * @throws RasterFormatException if the given bounds is empty.
     */
    public static int prepareTransferRegion(final Rectangle bounds, final int dataType) {
        if (bounds.isEmpty()) {
            throw new RasterFormatException(Resources.format(Resources.Keys.EmptyTileOrImageRegion));
        }
        final int afterLastRow = Math.addExact(bounds.y, bounds.height);
        int size;
        try {
            size = DataBuffer.getDataTypeSize(dataType);
        } catch (IllegalArgumentException e) {
            size = Short.SIZE;  // Arbitrary value is okay because this is only a hint for choosing a buffer size.
        }
        bounds.height = Math.max(1, Math.min(BUFFER_SIZE / (size * bounds.width), bounds.height));
        return afterLastRow;
    }
}
