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

import java.util.Arrays;
import java.util.logging.Logger;
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.DataBuffer;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.PackedColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import static java.lang.Math.abs;
import static java.lang.Math.rint;
import static java.lang.Math.floorDiv;
import static java.lang.Math.toIntExact;
import static java.lang.Math.multiplyFull;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.image.DataType;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.system.Modules;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.Static;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.util.resources.Vocabulary;
import static org.apache.sis.util.privy.Numerics.COMPARISON_THRESHOLD;


/**
 * Utility methods related to images and their color model or sample model.
 * Those methods only fetch information, they do not create new rasters or sample/color models
 * (see {@code *Factory} classes for creating those objects).
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ImageUtilities extends Static {
    /**
     * The logger for operations on images and rasters.
     */
    public static final Logger LOGGER = Logger.getLogger(Modules.RASTER);

    /**
     * Do not allow instantiation of this class.
     */
    private ImageUtilities() {
    }

    /**
     * Returns a shape containing all pixels that are valid in this image.
     * The returned shape may conservatively contain more than the minimal set of valid pixels.
     *
     * @param  image  the image for which to get the valid area.
     * @return a shape (not necessarily the smallest) containing all pixels that are valid.
     */
    public static Shape getValidArea(final RenderedImage image) {
        if (image instanceof PlanarImage) {
            return ((PlanarImage) image).getValidArea();
        } else {
            return getBounds(image);
        }
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
     * Note that {@link Rectangle#width} and/or {@link Rectangle#width} results may be negative.
     * Consequently, the caller should test {@link Rectangle#isEmpty()} on the returned value.
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
     * Returns the number of bands in the given image, or 0 if the image or its sample model is null.
     *
     * @param  image  the image for which to get the number of bands, or {@code null}.
     * @return number of bands in the specified image, or 0 if the image or its sample model is null.
     *
     * @see SampleModel#getNumBands()
     * @see Raster#getNumBands()
     */
    public static int getNumBands(final RenderedImage image) {
        if (image != null) {
            final SampleModel sm = image.getSampleModel();
            if (sm != null) return sm.getNumBands();
        }
        return 0;
    }

    /**
     * If the given image is showing only one band, returns the index of that band.
     * Otherwise, returns -1. Images showing only one band are SIS-specific
     * (usually an image shows all its bands).
     *
     * @param  image  the image for which to get the visible band, or {@code null}.
     * @return index of the visible band, or -1 if there is none or more than one.
     */
    public static int getVisibleBand(final RenderedImage image) {
        if (image != null) {
            final ColorModel cm = image.getColorModel();
            if (cm != null) {
                if (cm instanceof MultiBandsIndexColorModel) {
                    return ((MultiBandsIndexColorModel) cm).visibleBand;
                }
                final ColorSpace cs = cm.getColorSpace();
                if (cs instanceof ScaledColorSpace) {
                    return ((ScaledColorSpace) cs).visibleBand;
                }
            }
            final SampleModel sm = image.getSampleModel();
            if (sm != null && sm.getNumBands() == 1) {           // Should never be null, but we are paranoiac.
                return ColorModelFactory.DEFAULT_VISIBLE_BAND;
            }
        }
        return -1;
    }

    /**
     * Returns the index of the band shown by the given color model.
     * This is zero for standard color models, but <abbr>SIS</abbr>
     * sometime allows color models to show another band.
     *
     * @param  cm  the color model for which to get the visible band.
     * @return index of the visible band.
     */
    public static int getVisibleBand(final IndexColorModel cm) {
        if (cm instanceof MultiBandsIndexColorModel) {
            return ((MultiBandsIndexColorModel) cm).visibleBand;
        }
        return 0;
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
     *
     * @see #hasAlpha(RenderedImage)
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
     * Returns names of bands based on inspection of the sample model and color model.
     * The bands are identified by {@link Vocabulary.Keys} values for
     * red, green, blue, cyan, magenta, yellow, black, gray, <i>etc</i>.
     * If a band cannot be identified, then its corresponding value is 0.
     *
     * @param  cm  the color model for which to get band names, or {@code null} if unknown.
     * @param  sm  the image sample model (cannot be null).
     * @return {@link Vocabulary.Keys} identifying the bands.
     */
    @SuppressWarnings("fallthrough")
    public static short[] bandNames(final ColorModel cm, final SampleModel sm) {
        final int n = sm.getNumBands();
        final short[] keys = new short[n];
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
                 * a standard constructor. However, we do not know yet what would be the behavior
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
                    LOGGER.warning("Band names may be in wrong order.");
                }
            }
        }
        return keys;
    }

    /**
     * The values to be returned by {@link #toNumberEnum(DataType)}.
     */
    private static final byte[] NUMBER_ENUMS = {
        Numbers.BYTE, Numbers.SHORT, Numbers.SHORT, Numbers.INTEGER, Numbers.FLOAT, Numbers.DOUBLE
    };

    /**
     * Converts a {@link DataBuffer} enumeration value to {@link Numbers} enumeration value.
     * This method ignores whether the type is signed or unsigned.
     *
     * @param  type  the {@link DataBuffer} enumeration value.
     * @return the {@link Numbers} enumeration value.
     */
    public static byte toNumberEnum(final DataType type) {
        return NUMBER_ENUMS[type.toDataBufferType()];
    }

    /**
     * Returns {@code true} if the given data buffer type is an integer type.
     * Returns {@code false} if the type is a floating point type or in case
     * of doubt (e.g. for {@link DataBuffer#TYPE_UNDEFINED}).
     *
     * @param  dataType  one of {@link DataBuffer} constants.
     * @return whether the given constant is for an integer type.
     */
    public static boolean isIntegerType(final int dataType) {
        return dataType >= DataBuffer.TYPE_BYTE && dataType <= DataBuffer.TYPE_INT;
    }

    /**
     * Returns whether samples values stored using {@code source} model can be converted to {@code target} model
     * without data lost. This method verifies the number of bands and the size of data in each band.
     *
     * @param  source  model of sample values to convert.
     * @param  target  model of converted sample values.
     * @return whether the conversion from source model to target model is lossless.
     */
    public static boolean isLosslessConversion(final SampleModel source, final SampleModel target) {
        if (source != target) {
            final int numBands = source.getNumBands();
            if (target.getNumBands() < numBands) {
                return false;                           // Conversion would lost some bands.
            }
            final boolean sourceIsInteger = isIntegerType(source.getDataType());
            final boolean targetIsInteger = isIntegerType(target.getDataType());
            if (targetIsInteger && !sourceIsInteger) {
                return false;                           // Conversion from floating point type to integer type.
            }
            boolean hasSameSize = false;
            for (int i=0; i<numBands; i++) {
                final int d = target.getSampleSize(i) - source.getSampleSize(i);
                hasSameSize |= (d == 0);
                if (d < 0) {
                    return false;
                }
            }
            if (hasSameSize) {
                /*
                 * Need more checks if at least one band uses the same number of bits:
                 *   - Conversion from `int` to `float` can loose significant digits.
                 *   - Conversion from signed short to unsigned short (or conversely) can change values.
                 */
                if (sourceIsInteger != targetIsInteger || DataType.isUnsigned(source) != DataType.isUnsigned(target)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Converts a <var>x</var> pixel coordinates to a tile index.
     *
     * <h4>Implementation note</h4>
     * This method performs its calculation using <em>tile grid offset</em> instead of minimum coordinate
     * values because the former does not assume that image coordinates start at the beginning of first tile.
     * In practice it would be risky to have image {@code minX} different than first tile {@code minX},
     * but Apache SIS tries to handle the most general cases when possible.
     *
     * @param  image  the image containing tiles.
     * @param  x      the pixel coordinate for which to get tile index.
     * @return tile index for the given pixel coordinate.
     * @throws ArithmeticException if the result overflows 32 bits integer.
     */
    public static int pixelToTileX(final RenderedImage image, final int x) {
        return toIntExact(floorDiv((x - (long) image.getTileGridXOffset()), image.getTileWidth()));
    }

    /**
     * Converts a <var>y</var> pixel coordinates to a tile index.
     * See {@link #pixelToTileX(RenderedImage, int)} for an implementation note.
     *
     * @param  image  the image containing tiles.
     * @param  y      the pixel coordinate for which to get tile index.
     * @return tile index for the given pixel coordinate.
     * @throws ArithmeticException if the result overflows 32 bits integer.
     */
    public static int pixelToTileY(final RenderedImage image, final int y) {
        return toIntExact(floorDiv((y - (long) image.getTileGridYOffset()), image.getTileHeight()));
    }

    /**
     * Converts a tile column index to smallest <var>x</var> pixel coordinate inside the tile.
     * The returned value is a coordinate of the pixel in upper-left corner.
     *
     * <h4>Implementation note</h4>
     * This method performs its calculation using <em>tile grid offset</em> instead of minimum coordinate
     * values because the former does not assume that image coordinates start at the beginning of first tile.
     * In practice it would be risky to have image {@code minX} different than first tile {@code minX},
     * but Apache SIS tries to handle the most general cases when possible.
     *
     * @param  image  the image containing tiles.
     * @param  tileX  the tile index for which to get pixel coordinate.
     * @return smallest <var>x</var> pixel coordinate inside the tile.
     * @throws ArithmeticException if the result overflows 32 bits integer.
     */
    public static int tileToPixelX(final RenderedImage image, final int tileX) {
        // Following `long` arithmetic never overflows even if all values are `Integer.MAX_VALUE`.
        return toIntExact(multiplyFull(tileX, image.getTileWidth()) + image.getTileGridXOffset());
    }

    /**
     * Converts a tile row index to smallest <var>y</var> pixel coordinate inside the tile.
     * The returned value is a coordinate of the pixel in upper-left corner.
     * See {@link #tileToPixelX(RenderedImage, int)} for an implementation note.
     *
     * @param  image  the image containing tiles.
     * @param  tileY  the tile index for which to get pixel coordinate.
     * @return smallest <var>y</var> pixel coordinate inside the tile.
     * @throws ArithmeticException if the result overflows 32 bits integer.
     */
    public static int tileToPixelY(final RenderedImage image, final int tileY) {
        return toIntExact(multiplyFull(tileY, image.getTileHeight()) + image.getTileGridYOffset());
    }

    /**
     * Converts pixel coordinates to pixel indices.
     * This method does <strong>not</strong> clip the rectangle to image bounds.
     *
     * <h4>Implementation note</h4>
     * This method performs its calculation using <em>tile grid offset</em> instead of minimum coordinate
     * values because the former does not assume that image coordinates start at the beginning of first tile.
     * The intent is to be consistent with {@link #pixelToTileX(RenderedImage, int)}.
     *
     * @param  image   the image containing tiles.
     * @param  pixels  the pixel coordinates for which to get tile indices.
     * @return tile indices that fully contain the pixel coordinates.
     * @throws ArithmeticException if the result overflows 32 bits integer.
     */
    public static Rectangle pixelsToTiles(final RenderedImage image, final Rectangle pixels) {
        final Rectangle r = new Rectangle();
        if (!pixels.isEmpty()) {
            int  size;
            long offset, shifted;
            size     = image.getTileWidth();
            offset   = image.getTileGridXOffset();
            shifted  = pixels.x - offset;
            r.x      = toIntExact(floorDiv(shifted, size));
            r.width  = toIntExact(floorDiv(shifted + (pixels.width - 1), size) - r.x + 1);
            size     = image.getTileHeight();
            offset   = image.getTileGridYOffset();
            shifted  = pixels.y - offset;
            r.y      = toIntExact(floorDiv(shifted, size));
            r.height = toIntExact(floorDiv(shifted + (pixels.height - 1), size) - r.y + 1);
        }
        return r;
    }

    /**
     * Converts tile indices to pixel coordinate inside the tiles.
     * Tiles will be fully included in the returned range of pixel indices.
     * This method does <strong>not</strong> clip the rectangle to image bounds.
     *
     * <h4>Implementation note</h4>
     * This method performs its calculation using <em>tile grid offset</em> instead of minimum coordinate
     * values because the former does not assume that image coordinates start at the beginning of first tile.
     * The intent is to be consistent with {@link #tileToPixelX(RenderedImage, int)}.
     *
     * @param  image  the image containing tiles.
     * @param  tiles  the tile indices for which to get pixel coordinates.
     * @return pixel coordinates that fully contain the tiles.
     * @throws ArithmeticException if the result overflows 32 bits integer.
     */
    public static Rectangle tilesToPixels(final RenderedImage image, final Rectangle tiles) {
        final Rectangle r = new Rectangle();
        if (!tiles.isEmpty()) {
            int size, offset;
            size     = image.getTileWidth();
            offset   = image.getTileGridXOffset();
            r.x      = toIntExact(multiplyFull(tiles.x, size) + offset);
            r.width  = toIntExact(((((long) tiles.x) + tiles.width) * size) + offset - r.x);
            size     = image.getTileHeight();
            offset   = image.getTileGridYOffset();
            r.y      = toIntExact(multiplyFull(tiles.y, size) + offset);
            r.height = toIntExact(((((long) tiles.y) + tiles.height) * size) + offset - r.y);
        }
        return r;
    }

    /**
     * Converts tile indices from the specified source image to the specified target image.
     *
     * @param  source  image for which tile indices are given.
     * @param  tartet  image for which tile indices are desired.
     * @param  tiles   ranges of indices of tiles in the source image.
     * @return ranges of indices of tiles in the target image.
     */
    public static Rectangle convertTileIndices(RenderedImage source, RenderedImage target, Rectangle tiles) {
        Rectangle pixels = tilesToPixels(source, tiles);
        clipBounds(target, pixels);
        return pixelsToTiles(target, pixels);
    }

    /**
     * If scale and shear coefficients are close to integers, replaces their current values by their rounded values.
     * The scale and shear coefficients are handled in a "all or nothing" way; either all of them or none are rounded.
     * The translation terms are handled separately, provided that the scale and shear coefficients have been rounded.
     *
     * <p>This rounding is useful in order to accelerate some rendering operations. In particular Java2D has an
     * optimization when drawing {@link RenderedImage}: if the transform has only a translation (scale factors
     * are equal to 1) and if that translation is integer, then Java2D will fetch only tiles that are required
     * for the area to draw. Otherwise Java2D fetches a copy of the whole image.</p>
     *
     * <p>This method assumes that the given argument is a transform from something to display coordinates in pixel
     * units, or other kind of measurements usually expressed as integer values. In particular this method assumes
     * that if the scale and shear factors are integers, then translation terms should also be integer. Be careful
     * to not use this method with transforms where the translation terms may have a 0.5 offset (e.g. for mapping
     * pixel centers).</p>
     *
     * @param  tr  the transform to round in place. Target coordinates should be integer measurements such as pixels.
     * @return whether the transform has integer coefficients (possibly after rounding applied by this method).
     */
    public static boolean roundIfAlmostInteger(final AffineTransform tr) {
        double r;
        final double m00, m01, m10, m11;
        if (abs((m00 = rint(r=tr.getScaleX())) - r) <= COMPARISON_THRESHOLD &&
            abs((m01 = rint(r=tr.getShearX())) - r) <= COMPARISON_THRESHOLD &&
            abs((m11 = rint(r=tr.getScaleY())) - r) <= COMPARISON_THRESHOLD &&
            abs((m10 = rint(r=tr.getShearY())) - r) <= COMPARISON_THRESHOLD)
        {
            /*
             * At this point the scale and shear coefficients can be rounded to integers.
             * Continue only if this rounding does not make the transform non-invertible.
             *
             * Note: we round translation terms without checking if they are close to integers
             * on the assumption that the transform target coordinates are pixel coordinates.
             */
            if ((m00!=0 || m01!=0) && (m10!=0 || m11!=0)) {
                final double m02 = rint(tr.getTranslateX());
                final double m12 = rint(tr.getTranslateY());
                tr.setTransform(m00, m10, m01, m11, m02, m12);
                return true;
            }
        }
        return false;
    }
}
