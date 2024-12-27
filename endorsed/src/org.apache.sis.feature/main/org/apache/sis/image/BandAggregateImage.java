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

import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.util.LinkedHashSet;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.BandedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.math.Statistics;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.image.privy.ImageUtilities;
import org.apache.sis.coverage.privy.BandAggregateArgument;


/**
 * An image where each band is taken from a selection of bands in a sequence of source images.
 * This image will share the underlying data arrays when possible, or copy bands otherwise.
 * The actual strategy may be a mix of both bands copying and sharing.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see BandSelectImage
 * @see ImageCombiner
 */
class BandAggregateImage extends MultiSourceImage {
    /**
     * Whether the sharing of data arrays is allowed.
     * When a source tile has the same bounds and scanline stride than the target tile,
     * it is possible to share references to data arrays without copying the pixels.
     * This sharing is decided automatically on a source-by-source basis.
     * This flag allows to disable completely the sharing for all sources.
     */
    private final boolean allowSharing;

    /**
     * Concatenated list of the sample dimensions declared in all sources, or {@code null} if none.
     */
    private final List<SampleDimension> sampleDimensions;

    /*
     * The method declaration order below is a little bit unusual,
     * but it follows an execution order.
     */

    /**
     * Returns potentially deeper sources than the user supplied image.
     * This method unwraps {@link BandSelectImage} for making possible to detect that two
     * consecutive images are actually the same image, with only different bands selected.
     *
     * @param  unwrapper  a handler where to supply the result of an aggregate decomposition.
     */
    static void unwrap(final BandAggregateArgument<RenderedImage>.Unwrapper unwrapper) {
        RenderedImage source = unwrapper.source;
        int[] bands = unwrapper.bands;
        while (source instanceof ImageAdapter) {
            source = ((ImageAdapter) source).source;
        }
        if (source instanceof BandSelectImage) {
            final var select = (BandSelectImage) source;
            bands  = select.getSourceBands(bands);
            source = select.getSource();
        }
        if (source instanceof BandAggregateImage) {
            ((BandAggregateImage) source).subset(bands, null, unwrapper);
        } else if (source != unwrapper.source) {
            unwrapper.apply(new RenderedImage[] {source}, new int[][] {bands});
        }
    }

    /**
     * Decomposes this aggregate for the specified subset of bands.
     * The result can be used either for creating a new aggregate,
     * or consumed by {@code unwrapper} for flattening an aggregation.
     *
     * <p>This is a kind of constructor, but for an image derived from this instance.
     * The returned image may be one of the source images for simplifying the result.</p>
     *
     * @param  bands      the bands to keep.
     * @param  colors     the colors to apply, or {@code null} if unspecified.
     * @param  unwrapper  where to provide decomposition result, or {@code null} for creating the image immediately.
     * @return an image with a subset of the bands of this image, or {@code null} if {@code unwrapper} was non-null.
     */
    final RenderedImage subset(final int[] bands, final ColorModel colors,
            final BandAggregateArgument<RenderedImage>.Unwrapper unwrapper)
    {
        final RenderedImage[] sources = new RenderedImage[bands.length];
        final int[][] bandsPerSource = new int[bands.length][];
        int lower=0, upper=0, sourceIndex = -1;
        RenderedImage source = null;
        for (int i=0; i<bands.length; i++) {
            final int band = bands[i];
            if (band < lower) {
                lower = upper = 0;
                sourceIndex = -1;
            }
            while (band >= upper) {
                source = getSource(++sourceIndex);
                lower  = upper;
                upper += ImageUtilities.getNumBands(source);
            }
            sources[i] = source;
            bandsPerSource[i] = new int[] {band - lower};
        }
        /*
         * Tne same image may be repeated many times in the `sources` array, each time with only one band specified.
         * But we rely on `create(…)` post-processing for merging multiple references to a single one for each image.
         */
        if (unwrapper != null) {
            unwrapper.apply(sources, bandsPerSource);
            return null;
        }
        return create(sources, bandsPerSource, (colors != null) ? Colorizer.forInstance(colors) : null, false, allowSharing, parallel);
    }

    /**
     * Creates a new aggregation of bands.
     *
     * @param  sources         images to combine, in order.
     * @param  bandsPerSource  bands to use for each source image, in order. May contain {@code null} elements.
     * @param  colorizer       provider of color model to use for this image, or {@code null} for automatic.
     * @param  forceColors     whether to force application of {@code colorizer} when a source image is returned.
     * @param  allowSharing    whether to allow the sharing of data buffers (instead of copying) if possible.
     * @param  parallel        whether parallel computation is allowed.
     * @throws IllegalArgumentException if there is an incompatibility between some source images
     *         or if some band indices are duplicated or outside their range of validity.
     * @return the band aggregate image.
     */
    static RenderedImage create(final RenderedImage[] sources, final int[][] bandsPerSource, final Colorizer colorizer,
                                final boolean forceColors, final boolean allowSharing, final boolean parallel)
    {
        final var layout = new BandAggregateLayout(sources, bandsPerSource, allowSharing);
        final BandAggregateImage image;
        if (layout.isWritable()) {
            image = new Writable(layout, colorizer, parallel);
        } else {
            image = new BandAggregateImage(layout, colorizer, parallel);
        }
        RenderedImage result = image;
        if (image.getNumSources() == 1) {
            result = image.getSource();
            if ((forceColors && colorizer != null)) {
                result = RecoloredImage.applySameColors(result, image);
            }
        } else {
            result = ImageProcessor.unique(result);
        }
        /*
         * If we need to use `BandSelectImage` for reordering bands, the `unwrap` argument
         * MUST be false for avoiding `StackOverflowError` with never-ending recusivity.
         */
        return BandSelectImage.create(result, false, layout.bandSelect);
    }

    /**
     * Creates a new aggregation of bands.
     *
     * @param  layout     pixel and tile coordinate spaces of this image, together with sample model.
     * @param  colorizer  provider of color model to use for this image, or {@code null} for automatic.
     * @param  parallel   whether parallel computation is allowed.
     */
    private BandAggregateImage(final BandAggregateLayout layout, final Colorizer colorizer, final boolean parallel) {
        super(layout.filteredSources, layout.domain, layout.minTile, layout.sampleModel,
              layout.createColorModel(colorizer), parallel);

        this.allowSharing     = layout.allowSharing;
        this.sampleDimensions = layout.sampleDimensions;
    }

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
         * type given by `BandAggregateLayout` in the constructor.
         */
        BandSharedRaster shared = null;
        if (allowSharing) {
            final BandSharing sharing = BandSharing.create((BandedSampleModel) sampleModel);
            if (sharing != null) {
                tile = shared = sharing.createRaster(tileToPixel(tileX, tileY), getSourceArray());
            }
        }
        /*
         * Fallback when the data arrays cannot be shared.
         * This code copies all sample values in new arrays.
         */
        if (tile == null) {
            tile = createTile(tileX, tileY);
        }
        int band = 0;
        final int n = getNumSources();
        for (int i=0; i<n; i++) {
            final RenderedImage source = getSource(i);
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
     * Returns the names of all recognized properties,
     * or {@code null} if this image has no properties.
     */
    @Override
    public String[] getPropertyNames() {
        final var names = new LinkedHashSet<String>();
        if (sampleDimensions != null) {
            names.add(SAMPLE_DIMENSIONS_KEY);
        }
        final int numSources = getNumSources();
        for (int i=0; i<numSources; i++) {
            String[] more = getSource(i).getPropertyNames();
            if (more != null) {
                names.addAll(Arrays.asList(more));
            }
        }
        names.retainAll(BandSelectImage.REDUCED_PROPERTIES);
        return names.isEmpty() ? null : names.toArray(String[]::new);
    }

    /**
     * Gets a property of this image as a value derived from all source images.
     */
    @Override
    @SuppressWarnings("SuspiciousSystemArraycopy")
    public Object getProperty(final String key) {
        final int numBands = sampleModel.getNumBands();
        final Object result;
        switch (key) {
            case SAMPLE_DIMENSIONS_KEY: {
                if (sampleDimensions != null) {
                    return sampleDimensions.toArray(SampleDimension[]::new);
                }
                result = new SampleDimension[numBands];
                break;
            }
            case STATISTICS_KEY: {
                result = new Statistics[numBands];
                break;
            }
            case SAMPLE_RESOLUTIONS_KEY: {
                var r = new double[numBands];
                Arrays.fill(r, Double.NaN);
                result = r;
                break;
            }
            default: return super.getProperty(key);
        }
        int offset = 0;
        boolean found = false;
        final int numSources = getNumSources();
        for (int i=0; i<numSources; i++) {
            final RenderedImage source = getSource(i);
            final int n = ImageUtilities.getNumBands(source);
            final Object value = source.getProperty(key);
            if (result.getClass().isInstance(value)) {
                System.arraycopy(value, 0, result, offset, n);
                found = true;
            }
            offset += n;
        }
        return found ? result : null;
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
        Writable(final BandAggregateLayout layout, final Colorizer colorizer, final boolean parallel) {
            super(layout, colorizer, parallel);
        }

        /**
         * Checks out a tile for writing.
         */
        @Override
        public WritableRaster getWritableTile(final int tileX, final int tileY) {
            final WritableRaster tile = (WritableRaster) getTile(tileX, tileY);
            if (tile instanceof BandSharedRaster) {
                ((BandSharedRaster) tile).acquireWritableTiles(getSourceArray());
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
            final int n = getNumSources();
            for (int i=0; i<n; i++) {
                final var target = (WritableRenderedImage) getSource(i);
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
     * Compares the given object with this image for equality.
     */
    @Override
    public boolean equals(final Object object) {
        if (super.equals(object)) {
            final var that = (BandAggregateImage) object;
            return that.allowSharing == allowSharing &&
                   Objects.equals(that.sampleDimensions, sampleDimensions);
        }
        return false;
    }

    /**
     * Returns a hash code value for this image.
     */
    @Override
    public int hashCode() {
        return super.hashCode()
                + Boolean.hashCode(allowSharing)
                + Objects.hashCode(sampleDimensions);
    }
}
