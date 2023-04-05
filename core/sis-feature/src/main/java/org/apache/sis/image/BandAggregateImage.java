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
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.BandedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;


/**
 * An image where each band is taken from a selection of bands in a sequence of source images.
 * This image will share the underlying data arrays when possible, or copy bands otherwise.
 * The actual strategy may be a mix of both bands copying and sharing.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @see BandSelectImage
 * @see ImageCombiner
 *
 * @since 1.4
 */
class BandAggregateImage extends WritableComputedImage {
    /**
     * The source images with only the bands to aggregate, in order.
     * Those images are views; the band sample values are not copied.
     */
    protected final RenderedImage[] filteredSources;

    /**
     * Color model of the aggregated image.
     *
     * @see #getColorModel()
     */
    private final ColorModel colorModel;

    /**
     * Domain of pixel coordinates. All images shall share the same pixel coordinate space,
     * meaning that a pixel at coordinates (<var>x</var>, <var>y</var>) in this image will
     * contain the sample values of all source images at the same coordinates.
     * It does <em>not</em> mean that all source images shall have the same bounds.
     */
    private final int minX, minY, width, height;

    /**
     * Index of the first tile. Contrarily to pixel coordinates,
     * the tile coordinate space does not need to be the same for all images.
     */
    private final int minTileX, minTileY;

    /**
     * Whether the sharing of data arrays is allowed.
     * When a source tile has the same bounds and scanline stride than the target tile,
     * it is possible to share references to data arrays without copying the pixels.
     * This sharing is decided automatically on a source-by-source basis.
     * This flag allows to disable completely the sharing for all sources.
     */
    private final boolean allowSharing;

    /**
     * Creates a new aggregation of bands.
     *
     * @param  sources         images to combine, in order.
     * @param  bandsPerSource  bands to use for each source image, in order. May contain {@code null} elements.
     * @param  colorizer       provider of color model to use for this image, or {@code null} for automatic.
     * @param  allowSharing    whether to allow the sharing of data buffers (instead of copying) if possible.
     * @throws IllegalArgumentException if there is an incompatibility between some source images
     *         or if some band indices are duplicated or outside their range of validity.
     * @return the band aggregate image.
     */
    static RenderedImage create(final RenderedImage[] sources, final int[][] bandsPerSource,
                                final Colorizer colorizer, final boolean allowSharing)
    {
        final var layout = CombinedImageLayout.create(sources, bandsPerSource, allowSharing);
        final BandAggregateImage image;
        if (layout.isWritable()) {
            image = new Writable(layout, colorizer, allowSharing);
        } else {
            image = new BandAggregateImage(layout, colorizer, allowSharing);
        }
        if (image.filteredSources.length == 1) {
            final RenderedImage c = image.filteredSources[0];
            if (image.colorModel == null) {
                return c;
            }
            final ColorModel cm = c.getColorModel();
            if (cm == null || image.colorModel.equals(cm)) {
                return c;
            }
        }
        return image;
    }

    /**
     * Creates a new aggregation of bands.
     *
     * @param  layout     pixel and tile coordinate spaces of this image, together with sample model.
     * @param  colorizer  provider of color model to use for this image, or {@code null} for automatic.
     */
    BandAggregateImage(final CombinedImageLayout layout, final Colorizer colorizer, final boolean allowSharing) {
        super(layout.sampleModel, layout.sources);
        this.allowSharing = allowSharing;
        final Rectangle r = layout.domain;
        minX            = r.x;
        minY            = r.y;
        width           = r.width;
        height          = r.height;
        minTileX        = layout.minTileX;
        minTileY        = layout.minTileY;
        filteredSources = layout.filteredSources;
        colorModel      = layout.createColorModel(colorizer);
        ensureCompatible(colorModel);
    }

    /** Returns the information inferred at construction time. */
    @Override public ColorModel getColorModel() {return colorModel;}
    @Override public int        getWidth()      {return width;}
    @Override public int        getHeight()     {return height;}
    @Override public int        getMinX()       {return minX;}
    @Override public int        getMinY()       {return minY;}
    @Override public int        getMinTileX()   {return minTileX;}
    @Override public int        getMinTileY()   {return minTileY;}

    /**
     * Creates a raster containing the selected bands of source images.
     *
     * @param  tileX  the column index of the tile to compute.
     * @param  tileY  the row index of the tile to compute.
     * @param  tile   the previous tile, reused if non-null.
     */
    @Override
    protected Raster computeTile(final int tileX, final int tileY, WritableRaster tile) {
        if (tile instanceof BandSharedRaster) {
            tile = null;        // Do not take the risk of writing in source images.
        }
        /*
         * If we are allowed to share the data arrays, try that first.
         * The cast to `BandedSampleModel` is safe because this is the
         * type given by `CombinedImageLayout` in the constructor.
         */
        BandSharedRaster shared = null;
        if (allowSharing) {
            final BandSharing sharing = BandSharing.create((BandedSampleModel) sampleModel);
            if (sharing != null) {
                final long x = Math.multiplyFull(tileX - minTileX, getTileWidth())  + minX;
                final long y = Math.multiplyFull(tileY - minTileY, getTileHeight()) + minY;
                tile = shared = sharing.createRaster(x, y, filteredSources);
            }
        }
        /*
         * Fallback when the data arrays can not be shared.
         * This code copies all sample values in new arrays.
         */
        if (tile == null) {
            tile = createTile(tileX, tileY);
        }
        int band = 0;
        for (int i=0; i < filteredSources.length; i++) {
            final RenderedImage source = filteredSources[i];
            final int numBands = ImageUtilities.getNumBands(source);
            if (shared == null || shared.needCopy(i)) {
                final Rectangle aoi = tile.getBounds();
                ImageUtilities.clipBounds(source, aoi);
                if (!aoi.isEmpty()) {
                    final int[] bands = ArraysExt.range(band, band + numBands);
                    var target = tile.createWritableChild(aoi.x, aoi.y, aoi.width, aoi.height,
                                                          aoi.x, aoi.y, bands);
                    copyData(aoi, source, target);
                }
            }
            band += numBands;
        }
        return tile;
    }

    /**
     * A {@code BandAggregateImage} where all sources are writable rendered images.
     */
    private static final class Writable extends BandAggregateImage implements WritableRenderedImage {
        /**
         * Creates a new writable rendered image.
         *
         * @param  layout     pixel and tile coordinate spaces of this image, together with sample model.
         * @param  colorizer  provider of color model to use for this image, or {@code null} for automatic.
         */
        Writable(final CombinedImageLayout layout, final Colorizer colorizer, final boolean allowSharing) {
            super(layout, colorizer, allowSharing);
        }

        /**
         * Checks out a tile for writing.
         */
        @Override
        public WritableRaster getWritableTile(final int tileX, final int tileY) {
            final WritableRaster tile = (WritableRaster) getTile(tileX, tileY);
            if (tile instanceof BandSharedRaster) {
                ((BandSharedRaster) tile).acquireWritableTiles(filteredSources);
            }
            try {
                markTileWritable(tileX, tileY, true);
            } catch (RuntimeException e) {
                if (tile instanceof BandSharedRaster) {
                    ((BandSharedRaster) tile).releaseWritableTiles(e);
                }
                throw e;
            }
            return tile;
        }

        /**
         * Relinquishes the right to write to a tile.
         */
        @Override
        public void releaseWritableTile(final int tileX, final int tileY) {
            if (markTileWritable(tileX, tileY, false)) {
                final Raster tile = getTile(tileX, tileY);
                if (tile instanceof BandSharedRaster) {
                    ((BandSharedRaster) tile).releaseWritableTiles(null);
                }
                setData(tile);
            }
        }

        /**
         * Sets a region of the image to the contents of the given raster.
         * The raster is assumed to be in the same coordinate space as this image.
         * The operation is clipped to the bounds of this image.
         *
         * @param  tile  the values to write in this image.
         */
        @Override
        public void setData(final Raster tile) {
            final BandSharedRaster shared = (tile instanceof BandSharedRaster) ? (BandSharedRaster) tile : null;
            int band = 0;
            for (int i=0; i < filteredSources.length; i++) {
                final var target = (WritableRenderedImage) filteredSources[i];
                final int numBands = ImageUtilities.getNumBands(target);
                if (shared == null || shared.needCopy(i)) {
                    final Rectangle aoi = tile.getBounds();
                    ImageUtilities.clipBounds(target, aoi);
                    if (!aoi.isEmpty()) {
                        final int[] bands = ArraysExt.range(band, band + numBands);
                        var source = tile.createChild(aoi.x, aoi.y, aoi.width, aoi.height,
                                                      aoi.x, aoi.y, bands);
                        target.setData(source);
                    }
                }
                band += numBands;
            }
        }

        /**
         * Restores the identity behavior for writable image,
         * because it may have listeners attached to this specific instance.
         */
        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }

        /**
         * Restores the identity behavior for writable image,
         * because it may have listeners attached to this specific instance.
         */
        @Override
        public boolean equals(final Object object) {
            return object == this;
        }
    }

    /**
     * Returns a hash code value for this image.
     */
    @Override
    public int hashCode() {
        return sampleModel.hashCode() + 37 * (Arrays.hashCode(filteredSources) + 31 * Objects.hashCode(colorModel));
    }

    /**
     * Compares the given object with this image for equality.
     *
     * <h4>Implementation note</h4>
     * We do not invoke {@link #equalsBase(Object)} for saving the comparisons of {@link ComputedImage#sources} array.
     * The comparison of {@link #filteredSources} array will indirectly include the comparison of raw source images.
     */
    @Override
    public boolean equals(final Object object) {
        if (object instanceof BandAggregateImage) {
            final BandAggregateImage other = (BandAggregateImage) object;
            return minTileX == other.minTileX &&
                   minTileY == other.minTileY &&
                   getBounds().equals(other.getBounds()) &&
                   sampleModel.equals(other.sampleModel) &&
                   Objects.equals(colorModel, other.colorModel) &&
                   Arrays.equals(filteredSources, other.filteredSources);
        }
        return false;
    }
}
