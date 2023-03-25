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

import java.awt.Point;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.FrequencySortedSet;
import org.apache.sis.internal.feature.Resources;
import org.apache.sis.internal.coverage.j2d.ImageLayout;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;
import org.apache.sis.internal.coverage.j2d.ColorModelFactory;
import org.apache.sis.internal.coverage.MultiSourcesArgument;
import org.apache.sis.coverage.grid.DisjointExtentException;


/**
 * Computes the bounds of a destination image which will combine many source images.
 * A combination may be an aggregation or an overlay of bands, depending on the image class.
 * All images are assumed to use the same pixel coordinate space: (<var>x</var>, <var>y</var>)
 * expressed in pixel coordinates should map to the same geospatial location in all images.
 *
 * <p>Instances of this class are temporary and used only during image construction.</p>
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @see ImageCombiner
 * @see BandAggregateImage
 *
 * @since 1.4
 */
final class CombinedImageLayout extends ImageLayout {
    /**
     * The source images. This is a copy of the user-specified array,
     * except that images associated to an empty set of bands are discarded.
     */
    final RenderedImage[] sources;

    /**
     * Ordered (not necessarily sorted) indices of bands to select in each source image.
     * The length of this array is always equal to the length of the {@link #sources} array.
     * A {@code null} element means that all bands of the corresponding image should be used.
     * All non-null elements are non-empty and without duplicated values.
     */
    private final int[][] bandsPerSource;

    /**
     * The sample model of the combined image.
     */
    final SampleModel sampleModel;

    /**
     * The domain of pixel coordinates in the combined image. All sources images are assumed to use
     * the same pixel coordinate space, i.e. a pixel at coordinates (<var>x</var>, <var>y</var>) in
     * the combined image will contain values derived from pixels at the same coordinates in all
     * source images. It does <em>not</em> mean that all source images shall have the same bounds.
     */
    final Rectangle domain;

    /**
     * Index of the first tile. Other tile matrix properties such as the number of tiles and the grid offsets
     * are derived from those values together with the {@linkplain #domain}. Contrarily to pixel coordinates,
     * the tile coordinate space does not need to be the same for all images.
     *
     * @see #getMinTile()
     */
    final int minTileX, minTileY;

    /**
     * Whether to use the preferred tile size exactly as specified, without trying to compute a better size.
     * This field may be {@code true} if the tiles of the destination image are at exact same location than
     * the tiles of a source image having the preferred tile size. In such case, keeping the same size will
     * reduce the amount of tiles requested in that source image.
     */
    private final boolean exactTileSize;

    /**
     * Computes the layout of an image combining all the specified source images.
     * The optional {@code bandsPerSource} argument specifies the bands to select in each source images.
     * That array can be {@code null} for selecting all bands in all source images,
     * or may contain {@code null} elements for selecting all bands of the corresponding image.
     * An empty array element (i.e. zero band to select) discards the corresponding source image.
     *
     * <p>This static method is a workaround for RFE #4093999
     * ("Relax constraint on placement of this()/super() call in constructors").
     * This method may become the constructor after JEP 8300786 is available.</p>
     *
     * @param  sources         images to combine, in order.
     * @param  bandsPerSource  bands to use for each source image, in order. May contain {@code null} elements.
     * @throws IllegalArgumentException if there is an incompatibility between some source images
     *         or if some band indices are duplicated or outside their range of validity.
     */
    @Workaround(library="JDK", version="1.8")
    static CombinedImageLayout create(RenderedImage[] sources, int[][] bandsPerSource) {
        final var aggregate = new MultiSourcesArgument<RenderedImage>(sources, bandsPerSource);
        aggregate.identityAsNull();
        aggregate.validate(ImageUtilities::getNumBands);

        sources            = aggregate.sources();
        bandsPerSource     = aggregate.bandsPerSource();
        Rectangle domain   = null;
        int commonDataType = DataBuffer.TYPE_UNDEFINED;
        for (final RenderedImage source : sources) {
            /*
             * Ensure that all images use the same data type.
             * Get the domain of the combined image to create.
             *
             * TODO: current implementation computes the intersection of all sources.
             * But a future version should allow users to specify if they want intersection,
             * union or strict mode instead. A "strict" mode would prevent the combination of
             * images using different domains (i.e. raise an error if domains are not the same).
             */
            final int dataType = source.getSampleModel().getDataType();
            if (domain == null) {
                domain = ImageUtilities.getBounds(source);
                commonDataType = dataType;
            } else {
                ImageUtilities.clipBounds(source, domain);
                if (domain.isEmpty()) {
                    throw new DisjointExtentException(Resources.format(Resources.Keys.SourceImagesDoNotIntersect));
                }
                if (dataType != commonDataType) {
                    throw new IllegalArgumentException(Resources.format(Resources.Keys.MismatchedDataType));
                }
            }
        }
        if (domain == null) {
            // `domain` is guaranteed non-null if above block has been executed at least once.
            throw new IllegalArgumentException(Resources.format(Resources.Keys.UnspecifiedBands));
        }
        /*
         * Tile size is chosen after the domain has been computed, because we prefer a tile size which
         * is a divisor of the combined image size. Tile sizes of existing source images are preferred,
         * especially when the tiles are aligned, for increasing the chances that computation a tile of
         * the combined image causes the computation of a single tile of each source image.
         */
        long cx, cy;        // A combination of tile size with alignment on the tile matrix grid.
        cx = cy = (((long) Integer.MAX_VALUE) << Integer.SIZE) | ImageUtilities.DEFAULT_TILE_SIZE;
        final var tileGridXOffset = new FrequencySortedSet<Integer>(true);
        final var tileGridYOffset = new FrequencySortedSet<Integer>(true);
        for (final RenderedImage source : sources) {
            cx = chooseTileSize(cx, source.getTileWidth(),  domain.width,  domain.x - source.getMinX());
            cy = chooseTileSize(cy, source.getTileHeight(), domain.height, domain.y - source.getMinY());
            tileGridXOffset.add(source.getTileGridXOffset());
            tileGridYOffset.add(source.getTileGridYOffset());
        }
        final var preferredTileSize = new Dimension((int) cx, (int) cy);
        final boolean exactTileSize = ((cx | cy) >>> Integer.SIZE) == 0;
        return new CombinedImageLayout(sources, bandsPerSource, domain, preferredTileSize, exactTileSize,
                chooseMinTile(tileGridXOffset, domain.x, preferredTileSize.width),
                chooseMinTile(tileGridYOffset, domain.y, preferredTileSize.height),
                commonDataType, aggregate.numBands());
    }

    /**
     * Creates a new image layout from the values computed by {@code create(…)}.
     *
     * @param  sources            images to combine, in order.
     * @param  bandsPerSource     bands to use for each source image, in order. May contain {@code null} elements.
     * @param  domain             bounds of the image to create.
     * @param  preferredTileSize  the preferred tile size.
     * @param  commonDataType     data type of the combined image.
     * @param  numBands           number of bands of the image to create.
     */
    private CombinedImageLayout(final RenderedImage[] sources, final int[][] bandsPerSource,
            final Rectangle domain, final Dimension preferredTileSize, final boolean exactTileSize,
            final int minTileX, final int minTileY, final int commonDataType, final int numBands)
    {
        super(preferredTileSize, false);
        this.exactTileSize  = exactTileSize;
        this.bandsPerSource = bandsPerSource;
        this.sources        = sources;
        this.domain         = domain;
        this.minTileX       = minTileX;
        this.minTileY       = minTileY;
        this.sampleModel    = createBandedSampleModel(commonDataType, numBands, null, domain);
        // Sample model must be last (all other fields must be initialized before).
    }

    /**
     * Chooses the preferred tile size (width or height) between two alternatives.
     * The alternatives are {@code current} and {@code tile}. Note that having the
     * same tile size than source image is desirable but not mandatory.
     *
     * <p>The ideal situation is to have aligned tiles, so that computing one tile of combined image
     * will always cause the computation of only one tile of source image. For approaching that goal,
     * we pack the "grid alignment distance" ({@code offset}) in the higher bits and the tile size in
     * the lower bits of a {@code long}. Minimizing that {@code long} while optimize the offset first,
     * then choose the smallest tile size between tile matrices at the same offset.</p>
     *
     * @param  current    the current tile size, or 0 if none.
     * @param  tileSize   tile size (width or height) of the source image to consider.
     * @param  imageSize  full size (width or height) of the source image to consider.
     * @param  offset     location of the first pixel of the combined image relative to the first pixel of the source image.
     * @return the preferred tile size: {@code current} or {@code tile}.
     */
    private static long chooseTileSize(final long current, final int tileSize, final int imageSize, final int offset) {
        if ((imageSize % tileSize) == 0) {
            long c = Math.abs(offset % tileSize);       // How close the grid are aligned (ideal would be zero).
            c <<= Integer.SIZE;                         // Pack grid offset in higher bits.
            c |= tileSize;                              // Pack tile size in lower bits.
            /*
             * The use of `compareUnsigned(…)` below is an opportunistic trick for discarding negative tile size
             * (should be illegal, but we are paranoiac). If tile size was negative, the first bit should be set,
             * which is interpreted as a very high value when comparing as unsigned numbers.
             */
            if (Long.compareUnsigned(c, current) < 0) {
                return c;
            }
        }
        return current;
    }

    /**
     * Chooses the minimum tile index ({@code minTileX} or {@code minTileY}).
     * This value does not really matter, it has no incidence on performance or positional accuracy.
     * However using the same "tile grid offset" in the combined image compared to the source images
     * can make debugging easier. This is similar to using the same "grid to CRS" transform in grid
     * coverages.
     *
     * @param  offsets   all "tile grid offsets" sorted with most frequently used first.
     * @param  min       the minimal pixel coordinates in the combined image.
     * @param  tileSize  the tile size (width or height).
     * @return suggested minimum tile index.
     */
    private static int chooseMinTile(final FrequencySortedSet<Integer> offsets, final int min, final int tileSize) {
        for (int offset : offsets) {
            offset = min - offset;
            if (offset % tileSize == 0) {
                return offset / tileSize;
            }
        }
        return 0;
    }

    /**
     * Returns indices of the first tile ({@code minTileX}, {@code minTileY}).
     * Other tile matrix properties such as the number of tiles and the grid offsets will be derived
     * from those values together with the {@linkplain #domain}. Contrarily to pixel coordinates,
     * the tile coordinate space does not need to be the same for all images.
     *
     * @return indices of the first tile ({@code minTileX}, {@code minTileY}).
     */
    @Override
    public Point getMinTile() {
        return new Point(minTileX, minTileY);
    }

    /**
     * Suggests a tile size for the specified image size.
     * It may be exactly the size of the tiles of a source image, but not necessarily.
     * This method may compute a tile size different than the tile size of all sources.
     */
    @Override
    public Dimension suggestTileSize(int imageWidth, int imageHeight, boolean allowPartialTiles) {
        if (exactTileSize) return getPreferredTileSize();
        return super.suggestTileSize(imageWidth, imageHeight, allowPartialTiles);
    }

    /**
     * Suggests a tile size for operations derived from the given image.
     * It may be exactly the size of the tiles of a source image, but not necessarily.
     * This method may compute a tile size different than the tile size of all sources.
     */
    @Override
    public Dimension suggestTileSize(RenderedImage image, Rectangle bounds, boolean allowPartialTiles) {
        if (exactTileSize) return getPreferredTileSize();
        return super.suggestTileSize(image, bounds, allowPartialTiles);
    }

    /**
     * Returns the source images with only the user-specified bands.
     * The returned images are views; the bands are not copied.
     *
     * @return the source images with only user-supplied bands.
     */
    final RenderedImage[] getFilteredSources() {
        final RenderedImage[] images = new RenderedImage[sources.length];
        for (int i=0; i<images.length; i++) {
            RenderedImage source = sources[i];
            final int[] bands = bandsPerSource[i];
            if (bands != null) {
                source = BandSelectImage.create(source, bands);
            }
            images[i] = source;
        }
        return images;
    }

    /**
     * Builds a default color model with RGB(A) colors or the colors of the first visible band.
     * If the combined image has 3 or 4 bands and the data type is 8 bits integer (bytes),
     * then this method returns a RGB or RGBA color model depending if there is 3 or 4 bands.
     * Otherwise if {@link ImageUtilities#getVisibleBand(RenderedImage)} finds that a source image
     * declares a visible band, then the returned color model will reuse the colors of that band.
     * Otherwise a grayscale color model is built with a value range inferred from the data-type.
     */
    final ColorModel createColorModel() {
        ColorModel colors = ColorModelFactory.createRGB(sampleModel);
        if (colors != null) {
            return colors;
        }
        int visibleBand = ColorModelFactory.DEFAULT_VISIBLE_BAND;
        int base = 0;
search: for (int i=0; i < sources.length; i++) {
            final RenderedImage source = sources[i];
            final int[] bands = bandsPerSource[i];
            final int vb = ImageUtilities.getVisibleBand(source);
            if (vb >= 0) {
                if (bands == null) {
                    visibleBand = base + vb;
                    colors = source.getColorModel();
                    break;
                }
                for (int j=0; j<bands.length; j++) {
                    if (bands[j] == vb) {
                        visibleBand = base + j;
                        colors = source.getColorModel();
                        break search;
                    }
                }
            }
            base += (bands != null) ? bands.length : ImageUtilities.getNumBands(source);
        }
        colors = ColorModelFactory.derive(colors, sampleModel.getNumBands(), visibleBand);
        if (colors != null) {
            return colors;
        }
        return ColorModelFactory.createGrayScale(sampleModel, visibleBand, null);
    }

    /**
     * Ensures that a user-supplied color model is compatible.
     *
     * @param  name  parameter name of the user-supplied color model.
     * @param  cm    the color model to validate. Can be {@code null}.
     * @throws IllegalArgumentException if the color model is incompatible.
     */
    void ensureCompatible(final String name, final ColorModel cm) {
        final String reason = PlanarImage.verifyCompatibility(sampleModel, cm);
        if (reason != null) {
            String message = Resources.format(Resources.Keys.IncompatibleColorModel);
            if (!reason.isEmpty()) {
                message = message + ' ' + Errors.format(Errors.Keys.IllegalValueForProperty_2, reason, name);
            }
            throw new IllegalArgumentException(message);
        }
    }
}