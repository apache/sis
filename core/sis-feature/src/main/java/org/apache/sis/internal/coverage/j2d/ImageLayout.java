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
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.BandedSampleModel;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.image.ComputedImage;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.Strings;


/**
 * Derives information about image location, size and tile grid. {@code ImageLayout} does not store
 * those information directly, but provides method for deriving those properties from a given image.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
public class ImageLayout {
    /**
     * The minimum tile size. The {@link #toTileSize(int, int, boolean)} method will not suggest tiles
     * smaller than this size. This size must be smaller than {@link ImageUtilities#DEFAULT_TILE_SIZE}.
     *
     * <p>Tiles of 180×180 pixels consume about 127 kB, assuming 4 bytes per pixel. This is about half
     * the consumption of tiles of 256×256 pixels. We select a size which is a multiple of 90 because
     * images are often used with a resolution of e.g. ½° per pixel.</p>
     */
    private static final int MIN_TILE_SIZE = 180;

    /**
     * The default instance which will target {@value ImageUtilities#DEFAULT_TILE_SIZE} pixels as tile
     * width and height.
     */
    public static final ImageLayout DEFAULT = new ImageLayout(null, false);

    /**
     * Same as {@link #DEFAULT}, but makes image size an integer amount of tiles.
     */
    public static final ImageLayout SIZE_ADJUST = new ImageLayout(null, true);

    /**
     * Preferred size for tiles.
     *
     * @see ImageUtilities#DEFAULT_TILE_SIZE
     */
    private final int preferredTileWidth, preferredTileHeight;

    /**
     * Whether image size can be modified if needed. Changes are applied only if an image can not be tiled
     * because {@link #suggestTileSize(int, int, boolean)} can not find a tile size close to the desired size.
     * For example if the image width is a prime number, there is no way to divide the image horizontally with
     * an integer number of tiles. The only way to get an integer number of tiles is to change the image size.
     *
     * <p>If this flag is {@code true}, then the {@code bounds} argument given to the
     * {@link #suggestTileSize(RenderedImage, Rectangle, boolean)} will be modified in-place.</p>
     */
    public final boolean isBoundsAdjustmentAllowed;

    /**
     * Creates a new image layout.
     *
     * @param  preferredTileSize          the preferred tile size, or {@code null} for the default size.
     * @param  isBoundsAdjustmentAllowed  whether image size can be modified if needed.
     */
    public ImageLayout(final Dimension preferredTileSize, final boolean isBoundsAdjustmentAllowed) {
        if (preferredTileSize != null) {
            preferredTileWidth  = preferredTileSize.width;
            preferredTileHeight = preferredTileSize.height;
        } else {
            preferredTileWidth  = ImageUtilities.DEFAULT_TILE_SIZE;
            preferredTileHeight = ImageUtilities.DEFAULT_TILE_SIZE;
        }
        this.isBoundsAdjustmentAllowed = isBoundsAdjustmentAllowed;
    }

    /**
     * Creates a new layout with exactly the tile size of given image.
     *
     * @param  source  image from which to take tile size and indices.
     * @return layout giving exactly the tile size and indices of given image.
     */
    public static ImageLayout fixedSize(final RenderedImage source) {
        return new FixedSize(source);
    }

    /**
     * Override preferred tile size with a fixed size.
     */
    private static final class FixedSize extends ImageLayout {
        /** Indices of the first tile. */
        private final int xmin, ymin;

        /** Creates a new layout with exactly the tile size of given image. */
        FixedSize(final RenderedImage source) {
            super(new Dimension(source.getTileWidth(), source.getTileHeight()), false);
            xmin = source.getMinTileX();
            ymin = source.getMinTileY();
        }

        /** Returns the fixed tile size. All parameters are ignored. */
        @Override public Dimension suggestTileSize(int imageWidth, int imageHeight, boolean allowPartialTiles) {
            return getPreferredTileSize();
        }

        /** Returns the fixed tile size. All parameters are ignored. */
        @Override public Dimension suggestTileSize(RenderedImage image, Rectangle bounds, boolean allowPartialTiles) {
            return getPreferredTileSize();
        }

        /** Returns indices of the first tile. */
        @Override public Point getMinTile() {
            return new Point(xmin, ymin);
        }
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
     * method returns a size that minimize the amount of empty pixels in the last tile.
     *
     * @param  imageSize          the image size (width or height).
     * @param  preferredTileSize  the preferred tile size, which is often {@value ImageUtilities#DEFAULT_TILE_SIZE}.
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
         * search the divisor which will minimize the amount of empty pixels.
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
         * and `rmax` is the amount of non-empty pixels using this tile size.
         */
        return best;
    }

    /**
     * Suggests a tile size for the specified image size. This method suggests a tile size which is a divisor
     * of the given image size if possible, or a size that left as few empty pixels as possible otherwise.
     * The {@code allowPartialTile} argument specifies whether to allow tiles that are only partially filled.
     * A value of {@code true} implies that tiles in the last row or in the last column may contain empty pixels.
     * A value of {@code false} implies that this class will be unable to subdivide large images in smaller tiles
     * if the image size is a prime number.
     *
     * <p>The {@code allowPartialTile} argument should be {@code false} if the tiled image is opaque,
     * or if the sample value for transparent pixels is different than zero. This restriction is for
     * avoiding black or colored borders on the image left size and bottom size.</p>
     *
     * @param  imageWidth         the image width in pixels.
     * @param  imageHeight        the image height in pixels.
     * @param  allowPartialTiles  whether to allow tiles that are only partially filled.
     * @return suggested tile size for the given image size.
     */
    public Dimension suggestTileSize(final int imageWidth, final int imageHeight, final boolean allowPartialTiles) {
        return new Dimension(toTileSize(imageWidth,  preferredTileWidth,  allowPartialTiles),
                             toTileSize(imageHeight, preferredTileHeight, allowPartialTiles));
    }

    /**
     * Suggests a tile size for operations derived from the given image.
     * If the given image is null, then this method returns the preferred tile size.
     * Otherwise if the given image is already tiled, then this method preserves the
     * current tile size unless the tiles are too large, in which case they may be subdivided.
     * Otherwise (untiled image) this method proposes a tile size.
     *
     * <p>This method also checks whether the color model supports transparency. If not, then this
     * method will not return a size that may result in the creation of partially empty tiles.</p>
     *
     * @param  image   the image for which to derive a tile size, or {@code null}.
     * @param  bounds  the bounds of the image to create, or {@code null} if same as {@code image}.
     * @param  allowPartialTiles  whether to allow tiles that are only partially filled.
     *         This argument is ignored (reset to {@code false}) if the given image is opaque.
     * @return suggested tile size for the given image.
     */
    public Dimension suggestTileSize(final RenderedImage image, final Rectangle bounds, boolean allowPartialTiles) {
        if (bounds != null && bounds.isEmpty()) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, "bounds"));
        }
        if (allowPartialTiles && image != null && !isBoundsAdjustmentAllowed) {
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
        final Dimension tileSize = new Dimension(
                toTileSize(width,  preferredTileWidth,  allowPartialTiles & singleXTile),
                toTileSize(height, preferredTileHeight, allowPartialTiles & singleYTile));
        /*
         * Optionally adjust the image bounds for making it divisible by the tile size.
         */
        if (isBoundsAdjustmentAllowed && bounds != null && !bounds.isEmpty()) {
            final int sx = sizeToAdd(bounds.width,  tileSize.width);
            final int sy = sizeToAdd(bounds.height, tileSize.height);
            if ((bounds.width  += sx) < 0) bounds.width  -= tileSize.width;     // if (overflow) reduce to valid range.
            if ((bounds.height += sy) < 0) bounds.height -= tileSize.height;
            bounds.translate(-sx/2, -sy/2);
        }
        return tileSize;
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
     * Creates a banded sample model of the given type with
     * {@linkplain #suggestTileSize(RenderedImage, Rectangle, boolean) the suggested tile size} for the given image.
     *
     * @param  type      desired data type as a {@link java.awt.image.DataBuffer} constant.
     * @param  numBands  desired number of bands.
     * @param  image     the image which will be the source of the image for which a sample model is created.
     * @param  bounds    the bounds of the image to create, or {@code null} if same as {@code image}.
     * @return a banded sample model of the given type with the given number of bands.
     */
    public BandedSampleModel createBandedSampleModel(final int type, final int numBands,
            final RenderedImage image, final Rectangle bounds)
    {
        final Dimension tile = suggestTileSize(image, bounds, isBoundsAdjustmentAllowed);
        return RasterFactory.unique(new BandedSampleModel(type, tile.width, tile.height, numBands));
    }

    /**
     * Creates a sample model compatible with the sample model of the given image
     * but with a size matching the preferred tile size. This method can be used
     * for determining the {@code sampleModel} argument of {@link ComputedImage}
     * constructor.
     *
     * @param  image   the image form which to get a sample model.
     * @param  bounds  the bounds of the image to create, or {@code null} if same as {@code image}.
     * @return image sample model with preferred tile size.
     *
     * @see ComputedImage#ComputedImage(SampleModel, RenderedImage...)
     */
    public SampleModel createCompatibleSampleModel(final RenderedImage image, final Rectangle bounds) {
        ArgumentChecks.ensureNonNull("image", image);
        final Dimension tile = suggestTileSize(image, bounds, isBoundsAdjustmentAllowed);
        SampleModel sm = image.getSampleModel();
        if (sm.getWidth() != tile.width || sm.getHeight() != tile.height) {
            sm = sm.createCompatibleSampleModel(tile.width, tile.height);
            sm = RasterFactory.unique(sm);
        }
        return sm;
    }

    /**
     * Returns indices of the first tile ({@code minTileX}, {@code minTileY}), or {@code null} for (0,0).
     * The default implementation returns {@code null}.
     *
     * @return indices of the first tile ({@code minTileX}, {@code minTileY}), or {@code null} for (0,0).
     */
    public Point getMinTile() {
        return null;
    }

    /**
     * Returns a string representation for debugging purpose.
     *
     * @return a string representation for debugging purpose.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(),
                "preferredTileSize", new StringBuilder().append(preferredTileWidth).append('×').append(preferredTileHeight),
                "isBoundsAdjustmentAllowed", isBoundsAdjustmentAllowed);
    }
}
