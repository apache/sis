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
package org.apache.sis.image;

import java.util.Arrays;
import java.util.Objects;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.BandedSampleModel;
import java.awt.image.WritableRenderedImage;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.privy.Strings;
import org.apache.sis.system.Configuration;
import org.apache.sis.coverage.privy.RasterFactory;
import org.apache.sis.feature.internal.Resources;


/**
 * Preferences about the tiling of an image in relationship with a given image size.
 * {@code ImageLayout} contains a <em>preferred</em> tile size, together with methods
 * for deriving an actual tile size for a given image size.
 * The rules for deriving a tile size are configurable by flags.
 *
 * <p>An image layout can be specified in more details with a {@link SampleModel}.
 * The size of a sample model usually determines the size of tiles, but the former
 * may be replaced by the tile size {@linkplain #suggestTileSize(int, int) computed}
 * by this {@code ImageLayout} class.</p>
 *
 * <p>This class contains no information about colors.
 * The {@link ColorModel} to associate with a {@link SampleModel}
 * is controlled by a separated interface: {@link Colorizer}.</p>
 *
 * <p>Instances of this class are immutable and thread-safe.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public class ImageLayout {
    /**
     * The minimum tile width or height. The {@link #toTileSize(int, int, boolean)} method will not
     * suggest tiles smaller than this size. This size must be smaller than {@link #DEFAULT_TILE_SIZE}.
     *
     * <p>Tiles of 180×180 pixels consume about 127 kB, assuming 4 bytes per pixel. This is about half
     * the consumption of tiles of 256×256 pixels. We select a size which is a multiple of 90 because
     * images are often used with a resolution of e.g. ½° per pixel.</p>
     *
     * @see #DEFAULT_TILE_SIZE
     */
    @Configuration
    private static final int MIN_TILE_SIZE = 180;

    /**
     * Default width and height of tiles, in pixels.
     * This is currently set to {@value} pixels, but may change in any future Apache <abbr>SIS</abbr> version.
     */
    @Configuration
    public static final int DEFAULT_TILE_SIZE = 256;

    /**
     * The default instance with a preferred tile width and height of {@value #DEFAULT_TILE_SIZE} pixels.
     * Image sizes are preserved but the tile sizes are flexible. The last row and last column of tiles
     * in an image are allowed to be only partially filled.
     */
    public static final ImageLayout DEFAULT = new ImageLayout(null, null, true, false, true, null);

    /**
     * Preferred sample model, or {@code null} if none. The sample model width and height may be replaced
     * by the tile size {@linkplain #suggestTileSize(int, int) computed} by this {@code ImageLayout} class.
     *
     * @see #createCompatibleSampleModel(RenderedImage, Rectangle)
     */
    protected final SampleModel sampleModel;

    /**
     * Preferred size (in pixels) for tiles.
     * The actual tile size will also depend on the size of the image to tile.
     * The {@linkplain #DEFAULT default} value is {@value #DEFAULT_TILE_SIZE}.
     *
     * @see #DEFAULT_TILE_SIZE
     * @see #getPreferredTileSize()
     * @see #suggestTileSize(int, int)
     */
    protected final int preferredTileWidth, preferredTileHeight;

    /**
     * Whether to allow changes of tile size when needed. If this flag is {@code false},
     * then {@link #suggestTileSize(int, int)} unconditionally returns the preferred tile size.
     *
     * <p>The {@linkplain #DEFAULT default} value is {@code true}.</p>
     *
     * @see #allowTileSizeAdjustments(boolean)
     * @see #suggestTileSize(int, int)
     */
    public final boolean isTileSizeAdjustmentAllowed;

    /**
     * Whether to allow changes of image size when needed. An image may be resized when the
     * {@link #suggestTileSize(int, int)} method cannot find a size close enough to the preferred tile size.
     * For example, if the image width is a prime number, there is no way to divide the image horizontally with
     * an integer number of tiles. The only way to get an integer number of tiles is to change the image size.
     * This is done by changing the fields of the {@code bounds} argument given to the
     * {@link #suggestTileSize(RenderedImage, Rectangle)} method.
     *
     * <p>The {@linkplain #DEFAULT default} value is {@code false}.</p>
     *
     * @see #allowImageBoundsAdjustments(boolean)
     * @see #suggestTileSize(RenderedImage, Rectangle)
     */
    public final boolean isImageBoundsAdjustmentAllowed;

    /**
     * Whether to allow tiles that are only partially filled in the last row and last column of the tile matrix.
     * This flag may be ignored (handled as {@code false}) when the image for which to compute a tile size is opaque.
     *
     * <p>The {@linkplain #DEFAULT default} value is {@code true}.</p>
     *
     * @see #allowPartialTiles(boolean)
     */
    public final boolean isPartialTilesAllowed;

    /**
     * Preferred tile index where images start their tile matrix.
     * This property usually has no incidence on the appearance or performance
     * of an image and may be ignored by image operations.
     * The {@linkplain #DEFAULT default} value is 0.
     *
     * @see #getPreferredMinTile()
     */
    protected final int preferredMinTileX, preferredMinTileY;

    /**
     * Creates a new image layout with the given properties.
     *
     * @param  sampleModel                     preferred sample model, or {@code null} if none.
     * @param  preferredTileSize               preferred tile size, or {@code null} for the default size.
     * @param  isTileSizeAdjustmentAllowed     whether tile size can be modified if needed.
     * @param  isImageBoundsAdjustmentAllowed  whether image size can be modified if needed.
     * @param  isPartialTilesAllowed           whether to allow tiles that are only partially filled.
     * @param  preferredMinTile                preferred tile index where image start their tile matrix, or {@code null} for (0,0).
     */
    protected ImageLayout(final SampleModel sampleModel,
                          final Dimension   preferredTileSize,
                          final boolean     isTileSizeAdjustmentAllowed,
                          final boolean     isImageBoundsAdjustmentAllowed,
                          final boolean     isPartialTilesAllowed,
                          final Point       preferredMinTile)
    {
        this.sampleModel = sampleModel;
        if (preferredTileSize != null) {
            preferredTileWidth  = Math.max(1, preferredTileSize.width);
            preferredTileHeight = Math.max(1, preferredTileSize.height);
        } else {
            preferredTileWidth  = DEFAULT_TILE_SIZE;
            preferredTileHeight = DEFAULT_TILE_SIZE;
        }
        this.isTileSizeAdjustmentAllowed    = isTileSizeAdjustmentAllowed;
        this.isImageBoundsAdjustmentAllowed = isImageBoundsAdjustmentAllowed;
        this.isPartialTilesAllowed          = isPartialTilesAllowed;
        if (preferredMinTile != null) {
            preferredMinTileX = preferredMinTile.x;
            preferredMinTileY = preferredMinTile.y;
        } else {
            preferredMinTileX = 0;
            preferredMinTileY = 0;
        }
    }

    /**
     * Creates a new layout for writing in the given destination.
     *
     * @param  source    image from which to take tile size and indices.
     * @param  minTileX  column index of the first tile.
     * @param  minTileY  row index of the first tile.
     * @return layout giving exactly the tile size and indices of given image.
     */
    static ImageLayout forDestination(final WritableRenderedImage source, final int minTileX, final int minTileY) {
        return new FixedDestination(source, minTileX, minTileY);
    }

    /**
     * Override sample model with the one of the destination.
     */
    private static final class FixedDestination extends ImageLayout {
        /** The destination image. */
        private final WritableRenderedImage destination;

        /** Creates a new layout with exactly the tile size of given image. */
        FixedDestination(final WritableRenderedImage destination, final int minTileX, final int minTileY) {
            super(destination.getSampleModel(),
                  new Dimension(destination.getTileWidth(), destination.getTileHeight()),
                  false, false, true, new Point(minTileX, minTileY));
            this.destination = destination;
        }

        /** Returns an existing image where to write the computation result. */
        @Override public WritableRenderedImage getDestination() {
            return destination;
        }

        /** Returns the target sample model, which is fixed to the same as the destination image. */
        @Override public SampleModel createCompatibleSampleModel(RenderedImage image, Rectangle bounds) {
            return destination.getSampleModel();
        }
    }

    /**
     * Returns a new layout with the same properties than this layout except for the sample model.
     * If the given argument value results in no change, returns {@code this}.
     *
     * @param  model    the new sample model, or {@code null} if none.
     * @param  cascade  whether to set the preferred tile size to the size of the given sample model.
     * @return the layout for the given sample model.
     */
    public ImageLayout withSampleModel(final SampleModel model, final boolean cascade) {
        int width  = preferredTileWidth;
        int height = preferredTileHeight;
        if (cascade && model != null) {
            width  = model.getWidth();
            height = model.getHeight();
        }
        if (Objects.equals(sampleModel, model) && width == preferredTileWidth && height == preferredTileHeight) {
            return this;
        }
        return new ImageLayout(model, new Dimension(width, height),
                isTileSizeAdjustmentAllowed, isImageBoundsAdjustmentAllowed, isPartialTilesAllowed,
                getPreferredMinTile());
    }

    /**
     * Returns a new layout with the same properties than this layout except whether it allows changes of tile size.
     * If the given argument value results in no change, returns {@code this}.
     *
     * @param  allowed whether to allow changes of tile size when needed.
     * @return the layout for the given flag.
     *
     * @see #isTileSizeAdjustmentAllowed
     */
    public ImageLayout allowTileSizeAdjustments(boolean allowed) {
        if (isTileSizeAdjustmentAllowed == allowed) return this;
        return new ImageLayout(sampleModel,
                getPreferredTileSize(), allowed, isImageBoundsAdjustmentAllowed, isPartialTilesAllowed,
                getPreferredMinTile());
    }

    /**
     * Returns a new layout with the same properties than this layout except whether it allows changes of image size.
     * If the given argument value results in no change, returns {@code this}.
     *
     * @param  allowed whether to allow changes of image size when needed.
     * @return the layout for the given flag.
     *
     * @see #isImageBoundsAdjustmentAllowed
     */
    public ImageLayout allowImageBoundsAdjustments(boolean allowed) {
        if (isImageBoundsAdjustmentAllowed == allowed) return this;
        return new ImageLayout(sampleModel,
                getPreferredTileSize(), isTileSizeAdjustmentAllowed, allowed, isPartialTilesAllowed,
                getPreferredMinTile());
    }

    /**
     * Returns a new layout with the same properties than this layout except whether it allows partially filled tiles.
     * If the given argument value results in no change, returns {@code this}.
     *
     * @param  allowed whether to allow tiles that are only partially filled in the last row and last column of the tile matrix.
     * @return the layout for the given flag.
     *
     * @see #isPartialTilesAllowed
     */
    public ImageLayout allowPartialTiles(boolean allowed) {
        if (isPartialTilesAllowed == allowed) return this;
        return new ImageLayout(sampleModel,
                getPreferredTileSize(), isTileSizeAdjustmentAllowed, isImageBoundsAdjustmentAllowed, allowed,
                getPreferredMinTile());
    }

    /**
     * Creates a new layout with the tile size and tile indices of the given image.
     * Other properties of this {@code ImageLayout} (sample model and all Boolean flags) are inherited unchanged.
     * If the given argument value results in no change, returns {@code this}.
     *
     * @param  source  image from which to take tile size and tile indices.
     * @return layout giving exactly the tile size and indices of given image.
     *
     * @see #getPreferredTileSize()
     * @see #getPreferredMinTile()
     */
    public ImageLayout withTileMatrix(final RenderedImage source) {
        final var preferredTileSize = new Dimension(source.getTileWidth(), source.getTileHeight());
        final var preferredMinTile  = new Point(source.getMinTileX(), source.getMinTileY());
        if (preferredTileSize.width  == preferredTileWidth  &&
            preferredTileSize.height == preferredTileHeight &&
            preferredMinTile .x      == preferredMinTileX   &&
            preferredMinTile .y      == preferredMinTileY)
        {
            return this;
        }
        return new ImageLayout(sampleModel,
                preferredTileSize, isTileSizeAdjustmentAllowed, isImageBoundsAdjustmentAllowed, isPartialTilesAllowed,
                preferredMinTile);
    }

    /**
     * Returns a new layout with the same flags but a different preferred tile size.
     * If the given argument values result in no change, returns {@code this}.
     *
     * @param  size  the new tile size.
     * @return the layout for the given size.
     *
     * @see #getPreferredTileSize()
     */
    public ImageLayout withPreferredTileSize(final Dimension size) {
        if (size.width == preferredTileWidth && size.height == preferredTileHeight) {
            return this;
        }
        return new ImageLayout(sampleModel,
                size, isTileSizeAdjustmentAllowed, isImageBoundsAdjustmentAllowed,
                isPartialTilesAllowed, getPreferredMinTile());
    }

    /**
     * Returns the preferred tile size. This is the dimension values specified at construction time.
     *
     * @return the preferred tile size.
     */
    public final Dimension getPreferredTileSize() {
        return new Dimension(preferredTileWidth, preferredTileHeight);
    }

    /**
     * Suggests a tile size close to {@code tileSize} for the specified {@code imageSize}.
     * First, this method tries to return a tile size which is a divisor of the image size.
     * If no such divisor is found and {@code allowPartialTiles} is {@code true}, then this
     * method returns a size that minimize the number of empty pixels in the last tile.
     *
     * @param  imageSize          the image size (width or height).
     * @param  preferredTileSize  the preferred tile size, which is often {@value #DEFAULT_TILE_SIZE}.
     * @param  allowPartialTiles  whether to allow tiles that are only partially filled.
     * @return the suggested tile size, or {@code imageSize} if none.
     */
    private static int toTileSize(final int imageSize, final int preferredTileSize, final boolean allowPartialTiles) {
        final int maxTileSize = 2*preferredTileSize;    // Factor 2 is arbitrary (may be revisited in future versions).
        if (imageSize <= maxTileSize) {
            return imageSize;
        }
        int rmax = imageSize % preferredTileSize;
        if (rmax == 0) return preferredTileSize;
        /*
         * Find tile sizes which are divisors of image size and select the one closest to desired size.
         * Note: the (i >= 0) case should never happen because it an exact match existed, it should have
         * been found by the (imageSize % tileSize == 0) check.
         */
        final int[] divisors = MathFunctions.divisors(imageSize);
        int i = Arrays.binarySearch(divisors, preferredTileSize);
        if ((i = ~i) < divisors.length) {
            final int smaller;
            final boolean tooSmall;
            if (i == 0) {
                smaller  = 0;
                tooSmall = true;
            } else {
                smaller  = divisors[i - 1];
                tooSmall = (smaller < MIN_TILE_SIZE);
            }
            final int larger = divisors[i];
            if (larger <= (allowPartialTiles ? maxTileSize : imageSize)) {
                if (tooSmall || (larger - preferredTileSize) <= (preferredTileSize - smaller)) {
                    return larger;
                }
            }
            if (!tooSmall) {
                return smaller;
            }
        }
        /*
         * Found no exact divisor. If we are allowed to return an approximated size,
         * search the divisor which will minimize the number of empty pixels.
         */
        if (!allowPartialTiles) {
            return imageSize;
        }
        int best = preferredTileSize;
        for (i = maxTileSize; --i >= MIN_TILE_SIZE;) {
            final int r = imageSize % i;                    // Should never be 0 since we checked divisors before.
            if (r > rmax || (r == rmax && Math.abs(i - preferredTileSize) < Math.abs(best - preferredTileSize))) {
                rmax = r;
                best = i;
            }
        }
        /*
         * At this point `best` is an "optimal" tile size (the one that left as few empty pixels as possible),
         * and `rmax` is the number of non-empty pixels using this tile size.
         */
        return best;
    }

    /**
     * Suggests a tile size for the specified image size. This method suggests a tile size which is a divisor
     * of the given image size if possible, or a size that left as few empty pixels as possible otherwise.
     * The {@link #isPartialTilesAllowed} flag specifies whether to allow tiles that are only partially filled.
     * A value of {@code true} implies that tiles in the last row or in the last column may contain empty pixels.
     * A value of {@code false} implies that this class will be unable to subdivide large images in smaller tiles
     * if the image size is a prime number.
     *
     * <p>The {@link #isPartialTilesAllowed} flag should be {@code false} when the tiled image is opaque,
     * or if the sample value for transparent pixels is different than zero. This restriction is for
     * avoiding black or colored borders on the image left size and bottom size.</p>
     *
     * @param  imageWidth   the image width in pixels.
     * @param  imageHeight  the image height in pixels.
     * @return suggested tile size for the given image size.
     */
    public Dimension suggestTileSize(final int imageWidth, final int imageHeight) {
        int tileWidth  = preferredTileWidth;
        int tileHeight = preferredTileHeight;
        if (isTileSizeAdjustmentAllowed) {
            tileWidth  = toTileSize(imageWidth,  tileWidth,  isPartialTilesAllowed);
            tileHeight = toTileSize(imageHeight, tileHeight, isPartialTilesAllowed);
        }
        return new Dimension(tileWidth, tileHeight);
    }

    /**
     * Suggests a tile size for operations derived from the given image.
     * If the given image is null, then this method returns the preferred tile size.
     * Otherwise, if the given image is already tiled, then this method preserves the
     * current tile size unless the tiles are too large, in which case they may be subdivided.
     * Otherwise (untiled image), this method proposes a tile size.
     *
     * <p>This method checks whether the {@linkplain RenderedImage#getColorModel() image color model} supports transparency.
     * If not, then this method will not return a size that may result in the creation of partially empty tiles.
     * In other words, the {@link #isPartialTilesAllowed} flag is ignored (handled as {@code false}) for opaque
     * images.</p>
     *
     * @param  image   the image for which to derive a tile size, or {@code null}.
     * @param  bounds  the bounds of the image to create, or {@code null} if same as {@code image}.
     * @return suggested tile size for the given image.
     */
    public Dimension suggestTileSize(final RenderedImage image, final Rectangle bounds) {
        if (bounds != null && bounds.isEmpty()) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, "bounds"));
        }
        boolean allowPartialTiles = isPartialTilesAllowed;
        if (allowPartialTiles && image != null && !isImageBoundsAdjustmentAllowed) {
            final ColorModel cm = image.getColorModel();
            allowPartialTiles = (cm != null);
            if (allowPartialTiles) {
                if (cm instanceof IndexColorModel) {
                    allowPartialTiles = ((IndexColorModel) cm).getTransparentPixel() == 0;
                } else {
                    allowPartialTiles = (cm.getTransparency() != ColorModel.OPAQUE);
                }
            }
        }
        /*
         * If the image is already tiled, we may select smaller tiles if the original tiles are too large.
         * But those smaller tiles must be divisors of the original size. This is necessary because image
         * operations may assume that a call to `source.getTile(…)` will return a tile covering fully the
         * tile to compute.
         */
        final boolean singleXTile, singleYTile;
        final int width, height;
        if (bounds != null) {
            singleXTile = true;
            singleYTile = true;
            width  = bounds.width;
            height = bounds.height;
        } else if (image != null) {
            singleXTile = image.getNumXTiles() <= 1;
            singleYTile = image.getNumYTiles() <= 1;
            width  = singleXTile ? image.getWidth()  : image.getTileWidth();
            height = singleYTile ? image.getHeight() : image.getTileHeight();
        } else {
            return getPreferredTileSize();
        }
        int tileWidth  = preferredTileWidth;
        int tileHeight = preferredTileHeight;
        if (isTileSizeAdjustmentAllowed) {
            tileWidth  = toTileSize(width,  tileWidth,  allowPartialTiles & singleXTile);
            tileHeight = toTileSize(height, tileHeight, allowPartialTiles & singleYTile);
        }
        /*
         * Optionally adjust the image bounds for making it divisible by the tile size.
         */
        if (isImageBoundsAdjustmentAllowed && bounds != null && !bounds.isEmpty()) {
            final int sx = sizeToAdd(bounds.width,  tileWidth);
            final int sy = sizeToAdd(bounds.height, tileHeight);
            if ((bounds.width  += sx) < 0) bounds.width  -= tileWidth;     // if (overflow) reduce to valid range.
            if ((bounds.height += sy) < 0) bounds.height -= tileHeight;
            bounds.translate(-sx/2, -sy/2);
        }
        return new Dimension(tileWidth, tileHeight);
    }

    /**
     * Computes the size to add to the width or height for making it divisible by the given tile size.
     */
    private static int sizeToAdd(int size, final int tileSize) {
        size %= tileSize;
        if (size != 0) {
            size = tileSize - size;
        }
        return size;
    }

    /**
     * Ensures that the number of bands specified (directly or indirectly) in a method call
     * is compatible with the sample model.
     *
     * @param  actual  the number of bands inferred from the argument in a method call.
     */
    private void checkBandCount(final int actual) {
        int expected = sampleModel.getNumBands();
        if (expected != actual) {
            throw new IllegalStateException(Resources.format(Resources.Keys.UnexpectedNumberOfBands_2, expected, actual));
        }
    }

    /**
     * Returns a sample model with a size computed from the given image size.
     * If this {@code ImageLayout} contains a {@link #sampleModel}, then the latter is used as a template.
     * Otherwise, the new sample model is {@linkplain RenderedImage#getSampleModel() derived from the image}.
     *
     * @param  image   the image from which to derive a sample model.
     * @param  bounds  the bounds of the image to create, or {@code null} if same as {@code image}.
     * @return image sample model with a tile size derived from the given image size.
     * @throws IllegalStateException if {@link #sampleModel} is non-null
     *         and the given image does not have the same number of bands.
     *
     * @see ComputedImage#ComputedImage(SampleModel, RenderedImage...)
     */
    public SampleModel createCompatibleSampleModel(final RenderedImage image, final Rectangle bounds) {
        SampleModel sm = image.getSampleModel();
        if (sampleModel != null) {
            checkBandCount(sm.getNumBands());
            sm = sampleModel;
        }
        return createCompatibleSampleModel(sm, suggestTileSize(image, bounds));
    }

    /**
     * Returns a sample model equivalent to the given one, but using a different width and height.
     * If the given sample model already has the desired size, then it is returned unchanged.
     */
    private static SampleModel createCompatibleSampleModel(SampleModel sm, final Dimension tile) {
        if (sm.getWidth() != tile.width || sm.getHeight() != tile.height) {
            sm = sm.createCompatibleSampleModel(tile.width, tile.height);
            sm = RasterFactory.unique(sm);
        }
        return sm;
    }

    /**
     * Returns a sample model for the given data type with a size computed from the given image bounds.
     * If this {@code ImageLayout} contains a {@link #sampleModel}, then the latter is used as a template.
     *
     * @param  dataType  the default data type for the sample model to create.
     * @param  bounds    the bounds of the image to create.
     * @param  numBands  the number of bands in the sample model to create.
     * @return image sample model with a tile size derived from the given image bounds.
     * @throws IllegalStateException if {@link #sampleModel} is non-null but does not
     *         have the same number of bands as the given {@code numBands} argument.
     */
    public SampleModel createSampleModel(final DataType dataType, final Rectangle bounds, final int numBands) {
        ArgumentChecks.ensureNonNull("bounds", bounds);
        if (sampleModel != null) {
            checkBandCount(numBands);
            return createCompatibleSampleModel(sampleModel, suggestTileSize(null, bounds));
        }
        return createBandedSampleModel(null, bounds, dataType, numBands, 0);
    }

    /**
     * Creates a banded sample model for the given data type.
     * At least one of {@code image} and {@code bounds} arguments must be non null.
     * This method uses the {@linkplain #suggestTileSize(RenderedImage, Rectangle)
     * suggested tile size} for the given image and bounds.
     *
     * <p>This method constructs the simplest possible banded sample model:
     * All {@linkplain BandedSampleModel#getBandOffsets() band offsets} are zero and
     * all {@linkplain BandedSampleModel#getBankIndices() bank indices} are identity mapping.
     * This simplicity is needed by current implementation of {@link BandAggregateImage}.
     * User-specified {@link #sampleModel} is ignored.</p>
     *
     * @param  image           the image which will be the source of the image for which a sample model is created.
     * @param  bounds          the bounds of the image to create, or {@code null} if same as {@code image}.
     * @param  dataType        desired data type.
     * @param  numBands        desired number of bands.
     * @param  scanlineStride  the line stride of the image data, or ≤ 0 for automatic.
     * @return a banded sample model of the given type with the given number of bands.
     */
    final BandedSampleModel createBandedSampleModel(final RenderedImage image, final Rectangle bounds,
            final DataType dataType, final int numBands, int scanlineStride)
    {
        final Dimension tileSize = suggestTileSize(image, bounds);
        if (scanlineStride <= 0) {
            scanlineStride = tileSize.width;
        }
        // Pixel stride, bank indices and band offsets intentionally non-configurable. See Javadoc.
        return RasterFactory.unique(new BandedSampleModel(dataType.toDataBufferType(),
                tileSize.width, tileSize.height, scanlineStride, ArraysExt.range(0, numBands), new int[numBands]));
    }

    /**
     * Returns the preferred indices of the upper-left tile in an image tile matrix.
     * This property usually has no incidence on the appearance or performance of an image.
     * It usually doesn't change neither the calculations that depend on georeferencing,
     * because these calculations depend on pixel coordinates rather than tile coordinates.
     * Therefore, this property is only a hint and may be ignored by image operations.
     *
     * @return preferred tile indices of the upper-left tile.
     *
     * @see RenderedImage#getMinTileX()
     * @see RenderedImage#getMinTileY()
     */
    public final Point getPreferredMinTile() {
        return new Point(preferredMinTileX, preferredMinTileY);
    }

    /**
     * Returns an existing image where to write the computation result, or {@code null} if none.
     *
     * <p>This method is not yet in public API because it is currently set only by {@link ImageCombiner}.
     * Only the image operations needed by {@code ImageCombiner} take this information in account.</p>
     *
     * @return preexisting destination of computation result, or {@code null} if none.
     */
    WritableRenderedImage getDestination() {
        return null;
    }

    /**
     * Returns a string representation for debugging purpose.
     *
     * @return a string representation for debugging purpose.
     */
    @Override
    public String toString() {
        var sb = new StringBuilder();
        var preferredTileSize = sb.append(preferredTileWidth).append('×').append(preferredTileHeight).toString();
        sb.setLength(0);
        var preferredMinTile = sb.append('(').append(preferredMinTileX).append(", ").append(preferredMinTileY).append(')').toString();
        return Strings.toString(getClass(),
                "preferredTileSize",              preferredTileSize,
                "isTileSizeAdjustmentAllowed",    isTileSizeAdjustmentAllowed,
                "isImageBoundsAdjustmentAllowed", isImageBoundsAdjustmentAllowed,
                "isPartialTilesAllowed",          isPartialTilesAllowed,
                "preferredMinTile",               preferredMinTile);
    }
}
