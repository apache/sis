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
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Workaround;
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
final class BandAggregateImage extends ComputedImage {
    /**
     * The source images with only the bands to aggregate, in order.
     * Those images are views; the band sample values are not copied.
     */
    private final RenderedImage[] filteredSources;

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
     * Creates a new aggregation of bands.
     * This static method is a workaround for RFE #4093999
     * ("Relax constraint on placement of this()/super() call in constructors").
     *
     * @param  sources          images to combine, in order.
     * @param  bandsPerSource   bands to use for each source image, in order. May contain {@code null} elements.
     * @param  colorizer        provider of color model to use for this image, or {@code null} for automatic.
     * @throws IllegalArgumentException if there is an incompatibility between some source images
     *         or if some band indices are duplicated or outside their range of validity.
     * @return the band aggregate image.
     */
    @Workaround(library="JDK", version="1.8")
    static RenderedImage create(RenderedImage[] sources, int[][] bandsPerSource, Colorizer colorizer) {
        var image = new BandAggregateImage(CombinedImageLayout.create(sources, bandsPerSource), colorizer);
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
        return ImageProcessor.unique(image);
    }

    /**
     * Creates a new aggregation of bands.
     *
     * @param  layout     pixel and tile coordinate spaces of this image, together with sample model.
     * @param  colorizer  provider of color model to use for this image, or {@code null} for automatic.
     */
    private BandAggregateImage(final CombinedImageLayout layout, final Colorizer colorizer) {
        super(layout.sampleModel, layout.sources);
        final Rectangle r = layout.domain;
        minX            = r.x;
        minY            = r.y;
        width           = r.width;
        height          = r.height;
        minTileX        = layout.minTileX;
        minTileY        = layout.minTileY;
        filteredSources = layout.getFilteredSources();
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
     * Creates a raster sharing containing a copy of the selected bands in source images.
     *
     * @param  tileX     the column index of the tile to compute.
     * @param  tileY     the row index of the tile to compute.
     * @param  previous  the previous tile, reused if non-null.
     *
     * @todo Share data arrays instead of copying when possible.
     */
    @Override
    protected Raster computeTile(final int tileX, final int tileY, WritableRaster tile) {
        if (tile == null) {
            tile = createTile(tileX, tileY);
        }
        int band = 0;
        for (final RenderedImage source : filteredSources) {
            final Rectangle aoi = tile.getBounds();
            ImageUtilities.clipBounds(source, aoi);
            final int numBands = ImageUtilities.getNumBands(source);
            final int[] bands = ArraysExt.range(band, band + numBands);
            var target = tile.createWritableChild(aoi.x, aoi.y, aoi.width, aoi.height,
                                                  aoi.x, aoi.y, bands);
            band += numBands;
            copyData(aoi, source, target);
        }
        return tile;
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
