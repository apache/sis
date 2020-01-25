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
import java.awt.Dimension;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.image.ComputedImage;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.internal.util.Strings;


/**
 * Derives information about image location, size and tile grid. {@code ImageLayout} does not store
 * those information directly, but provides method for deriving those properties from a given image.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
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
    public static final ImageLayout DEFAULT = new ImageLayout(null, true);

    /**
     * Preferred size for tiles.
     *
     * @see ImageUtilities#DEFAULT_TILE_SIZE
     */
    private final int preferredTileWidth, preferredTileHeight;

    /**
     * Whether this instance allow tiles that are only partially filled. A value of {@code true} implies that
     * tiles in the last row or in the last column may contain empty pixels. A value of {@code false} implies
     * that this class will be unable to subdivide large images in smaller tiles if the image size is a prime
     * number.
     */
    private final boolean allowPartialTiles;

    /**
     * Creates a new image layout.
     *
     * @param  preferredTileSize  the preferred tile size, or {@code null} for the default size.
     * @param  allowPartialTiles  whether this instance allow tiles that are only partially filled.
     */
    protected ImageLayout(final Dimension preferredTileSize, final boolean allowPartialTiles) {
        if (preferredTileSize != null) {
            preferredTileWidth  = preferredTileSize.width;
            preferredTileHeight = preferredTileSize.height;
        } else {
            preferredTileWidth  = ImageUtilities.DEFAULT_TILE_SIZE;
            preferredTileHeight = ImageUtilities.DEFAULT_TILE_SIZE;
        }
        this.allowPartialTiles = allowPartialTiles;
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
        if (imageSize <= 2*preferredTileSize) {     // Factor 2 is arbitrary.
            return imageSize;
        }
        int rmax = imageSize % preferredTileSize;
        if (rmax == 0) return preferredTileSize;
        /*
         * Find tile sizes which are divisors of image size and select the one closest to desired size.
         * Note: the (i >= 0) check is a paranoiac check redundant with (imageSize % tileSize == 0) check.
         */
        final int[] divisors = MathFunctions.divisors(imageSize);
        int i = Arrays.binarySearch(divisors, preferredTileSize);
        if (i >= 0) return divisors[i];
        if ((i = ~i) < divisors.length) {
            final int smaller = divisors[i];
            final boolean tooSmall = (smaller < MIN_TILE_SIZE);
            if (++i < divisors.length) {
                final int larger = divisors[i];
                if (larger < imageSize && (tooSmall || (larger - preferredTileSize) <= preferredTileSize - smaller)) {
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
        int best = preferredTileSize;
        if (allowPartialTiles) {
            for (i = imageSize/2; --i >= MIN_TILE_SIZE;) {
                final int r = imageSize % i;
                if (r == 0) return i;       // Should never happen since we checked divisors before, but be paranoiac.
                if (r > rmax || (r == rmax && Math.abs(i - preferredTileSize) < Math.abs(best - preferredTileSize))) {
                    rmax = r;
                    best = i;
                }
            }
        }
        /*
         * At this point `best` is an "optimal" tile size (the one that left as few empty pixels as possible),
         * and `rmax` is the amount of non-empty pixels using this tile size. We will use that "optimal" size
         * only if it fills at least 75% of the tile size. Otherwise, we arbitrarily consider that it doesn't
         * worth to tile.
         */
        return (rmax >= preferredTileSize - preferredTileSize/4) ? best : imageSize;
    }

    /**
     * Suggests a tile size for the specified image size. This method suggests a tile size which is a divisor
     * of the given image size if possible, or a size that left as few empty pixels as possible otherwise.
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
    public Dimension suggestTileSize(final int imageWidth, final int imageHeight, boolean allowPartialTiles) {
        allowPartialTiles &= this.allowPartialTiles;
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
     * @param  image  the image for which to derive a tile size, or {@code null}.
     * @return suggested tile size for the given image.
     */
    public Dimension suggestTileSize(final RenderedImage image) {
        if (image == null) {
            return new Dimension(preferredTileWidth, preferredTileHeight);
        }
        boolean pt = allowPartialTiles;
        if (pt) {
            final ColorModel cm = image.getColorModel();
            if (pt = (cm != null)) {
                if (cm instanceof IndexColorModel) {
                    pt = ((IndexColorModel) cm).getTransparentPixel() == 0;
                } else {
                    pt = cm.hasAlpha();
                }
            }
        }
        /*
         * If the image is already tiled, we may select smaller tiles if the original tiles are too large
         * but those smaller tiles must be divisors of the original size. This is necessary because image
         * operations may assume that a call to `source.getTile(…)` will return a tile covering fully the
         * tile to compute.
         */
        final boolean singleXTile = image.getNumXTiles() <= 1;
        final boolean singleYTile = image.getNumYTiles() <= 1;
        int width  =  singleXTile ? image.getWidth()  : image.getTileWidth();
        int height =  singleYTile ? image.getHeight() : image.getTileHeight();
        return new Dimension(toTileSize(width,  preferredTileWidth,  pt & singleXTile),
                             toTileSize(height, preferredTileHeight, pt & singleYTile));
    }

    /**
     * Creates a sample model compatible with the sample model of the given image
     * but with a size matching the preferred tile size. This method can be used
     * for determining the {@code sampleModel} argument of {@link ComputedImage}
     * constructor.
     *
     * @param  image  the image form which to get a sample model.
     * @return image sample model with preferred tile size.
     *
     * @see ComputedImage#ComputedImage(SampleModel, RenderedImage...)
     */
    public SampleModel createCompatibleSampleModel(final RenderedImage image) {
        ArgumentChecks.ensureNonNull("image", image);
        final Dimension tile = suggestTileSize(image);
        SampleModel sm = image.getSampleModel();
        if (sm.getWidth() != tile.width || sm.getHeight() != tile.height) {
            sm = sm.createCompatibleSampleModel(tile.width, tile.height);
        }
        return sm;
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
                "allowPartialTiles", allowPartialTiles);
    }
}