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

import java.util.Set;
import java.util.Arrays;
import java.util.Hashtable;
import java.lang.reflect.Array;
import java.awt.Image;
import java.awt.image.Raster;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import java.awt.image.ColorModel;
import java.awt.image.TileObserver;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.image.privy.ImageUtilities;
import org.apache.sis.image.privy.TileOpExecutor;
import org.apache.sis.image.privy.ColorModelFactory;
import org.apache.sis.image.privy.ObservableImage;


/**
 * Selects or reorder bands from a source image.
 * This operation never copies sample values.
 * Instead, it works by modifying the sample model and color model.
 * This economical behavior is important for implementation of other
 * operations on top of this one, such as {@link BandAggregateImage}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
class BandSelectImage extends SourceAlignedImage {
    /**
     * Properties to inherit from the source images, after bands reduction if applicable.
     *
     * @see #getProperty(String)
     */
    private static final Set<String> INHERITED_PROPERTIES = Set.of(
            GRID_GEOMETRY_KEY, POSITIONAL_ACCURACY_KEY,                         // Properties to forward as-is.
            SAMPLE_DIMENSIONS_KEY, SAMPLE_RESOLUTIONS_KEY, STATISTICS_KEY);     // Properties to forward after band reduction.

    /**
     * Inherited properties that require band reduction.
     * Shall be a subset of {@link #INHERITED_PROPERTIES}.
     * All values must be arrays.
     */
    static final Set<String> REDUCED_PROPERTIES = Set.of(
            SAMPLE_DIMENSIONS_KEY, SAMPLE_RESOLUTIONS_KEY, STATISTICS_KEY);

    /**
     * The selected bands.
     */
    private final int[] bands;

    /**
     * Creates a new "band select" operation for the given source.
     * It is caller responsibility to verify the validity of given {@code bands} indices.
     *
     * @param  source  the image in which to select bands.
     * @param  cm      the color model to associate to this image.
     * @param  bands   the bands to select. Should be a clone of user-specified argument
     *                 (this constructor retains the given array reference as-is, without cloning).
     */
    private BandSelectImage(final RenderedImage source, final ColorModel cm, final int[] bands) {
        super(source, cm, source.getSampleModel().createSubsetSampleModel(bands));
        this.bands = bands;
        ensureCompatible(sampleModel, cm);
    }

    /**
     * Returns the indices of bands in the source image for the given bands in this image.
     * A reference to the given array will be returned if the band indices are the same.
     *
     * @param  bands  the band to select in this image.
     * @return the bands to select in source image.
     *
     * @see #getSource()
     */
    final int[] getSourceBands(final int[] subset) {
        final int[] select = new int[subset.length];
        for (int i=0; i<subset.length; i++) {
            select[i] = bands[subset[i]];
        }
        return Arrays.equals(subset, select) ? subset : select;
    }

    /**
     * Creates a new "band select" operation for the given source.
     *
     * @param  source  the image in which to select bands.
     * @param  unwrap  whether to allow unwrapping of {@link BandAggregateImage} source.
     * @param  bands   the bands to select. Not cloned in order to share common arrays when possible.
     *                 If that array instance was user supplied, then it should be cloned by caller.
     */
    static RenderedImage create(RenderedImage source, final boolean unwrap, int... bands) {
        final int numBands = ImageUtilities.getNumBands(source);
        if (bands.length == numBands && ArraysExt.isRange(0, bands)) {
            return source;
        }
        ArgumentChecks.ensureNonEmptyBounded("bands", false, 0, numBands - 1, bands);
        final ColorModel cm = ColorModelFactory.createSubset(source.getColorModel(), bands);
        /*
         * Since this operation applies its own ColorModel anyway, skip operation that was doing nothing else
         * than changing the color model. Operations adding properties such as stastics are kept because this
         * class can inherit some of them (see `REDUCED_PROPERTIES`).
         */
        if (source instanceof RecoloredImage) {
            source = ((RecoloredImage) source).source;
        }
        if (source instanceof BandSelectImage) {
            final var select = (BandSelectImage) source;
            bands  = select.getSourceBands(bands);
            source = select.getSource();
        }
        if (unwrap && source instanceof BandAggregateImage) {
            return ((BandAggregateImage) source).subset(bands, cm, null);
        }
        /*
         * If the image is an instance of `BufferedImage`, create the subset immediately
         * (reminder: this operation will not copy pixel data). It allows us to return a
         * new instance of `BufferedImage`, which has optimizations in Java2D.
         *
         * Note that buffered images do not support null color models.
         * In case a color model subset cannot be computed, the BandSelectImage fallback is used,
         * hoping user won't need the color model. We could have tried to create an arbitrary model,
         * but it is difficult to know if it would do more good than harm.
         */
        final RenderedImage image;
        if (cm != null && source instanceof BufferedImage) {
            final BufferedImage bi = (BufferedImage) source;
            @SuppressWarnings("UseOfObsoleteCollectionType")
            final var properties = new Hashtable<String,Object>(8);
            for (final String key : INHERITED_PROPERTIES) {
                final Object value = getProperty(bi, key, bands);
                if (value != Image.UndefinedProperty) {
                    properties.put(key, value);
                }
            }
            image = new ObservableImage(cm,
                    bi.getRaster().createWritableChild(0, 0, bi.getWidth(), bi.getHeight(), 0, 0, bands),
                    bi.isAlphaPremultiplied(), properties);
        } else if (source instanceof WritableRenderedImage) {
            image = new Writable(source, cm, bands);
        } else {
            image = new BandSelectImage(source, cm, bands);
        }
        return ImageProcessor.unique(image);
    }

    /**
     * Returns the names of all recognized properties,
     * or {@code null} if this image has no properties.
     * This method may conservatively return the names of properties that <em>may</em> exist.
     * It does not check if the property would be an array with only null values,
     * because doing that check may cause potentially costly computation.
     */
    @Override
    public String[] getPropertyNames() {
        return filterPropertyNames(getSource().getPropertyNames(), INHERITED_PROPERTIES, null);
    }

    /**
     * Gets a property from this image.
     */
    @Override
    public Object getProperty(final String key) {
        if (INHERITED_PROPERTIES.contains(key)) {
            return getProperty(getSource(), key, bands);
        } else {
            return super.getProperty(key);
        }
    }

    /**
     * Gets a property from the given image, reducing the number of dimensions if needed.
     * It is caller responsibility to verify that the given key is one of the keys enumerated
     * in {@link #INHERITED_PROPERTIES}.
     */
    private static Object getProperty(final RenderedImage source, final String key, final int[] bands) {
        final Object value = source.getProperty(key);
        if (value != null && REDUCED_PROPERTIES.contains(key)) {
            final Class<?> componentType = value.getClass().getComponentType();
            if (componentType != null) {
                final Object reduced = Array.newInstance(componentType, bands.length);
                boolean hasValue = false;
                for (int i=0; i<bands.length; i++) {
                    Object element = Array.get(value, bands[i]);
                    Array.set(reduced, i, element);
                    hasValue |= (element != null);
                }
                return hasValue ? reduced : Image.UndefinedProperty;
            }
        }
        return value;
    }

    /**
     * Creates a raster sharing the same data buffer as the source image but showing only a subset of the bands.
     *
     * @param  tileX     the column index of the tile to compute.
     * @param  tileY     the row index of the tile to compute.
     * @param  previous  ignored.
     */
    @Override
    protected Raster computeTile(final int tileX, final int tileY, final WritableRaster previous) {
        final Raster parent = getSource().getTile(tileX, tileY);
        final int x = parent.getMinX();
        final int y = parent.getMinY();
        /*
         * The following method call is a bit inefficient because it will create a new `SampleModel` for each tile
         * and all those sample models are identical to the one we created at `BandSelectImage` construction time.
         * But it does not seem possible to tell `Raster` to share the existing `SampleModel` instance.
         *
         * Alternatively, we could have tried to do the work of `Raster.createChild(…)` method ourselves.
         * But we don't because that method is overridden in various Java2D `SunWritableRaster` classes.
         */
        return parent.createChild(x, y, parent.getWidth(), parent.getHeight(), x, y, bands);
    }

    /**
     * Applies the band selection on the given writable raster.
     * The child is created in the same way as {@code computeTile(…)}.
     */
    final WritableRaster apply(final WritableRaster parent) {
        final int x = parent.getMinX();
        final int y = parent.getMinY();
        return parent.createWritableChild(x, y, parent.getWidth(), parent.getHeight(), x, y, bands);
    }

    /**
     * A {@code BandSelectImage} where the source is a writable rendered image.
     */
    private static final class Writable extends BandSelectImage implements WritableRenderedImage {
        /** Creates a new "band select" operation for the given source. */
        Writable(final RenderedImage source, final ColorModel cm, final int[] bands) {
            super(source, cm, bands);
        }

        /** Returns the source as a writable image. */
        private WritableRenderedImage target() {
            return (WritableRenderedImage) getSource();
        }

        /** Checks out a tile for writing. */
        @Override public WritableRaster getWritableTile(final int tileX, final int tileY) {
            markTileWritable(tileX, tileY, true);
            final WritableRaster parent = target().getWritableTile(tileX, tileY);
            return apply(parent);
        }

        /** Relinquishes the right to write to a tile. */
        @Override public void releaseWritableTile(final int tileX, final int tileY) {
            target().releaseWritableTile(tileX, tileY);
            markTileWritable(tileX, tileY, false);
        }

        /** Adds an observer to be notified when a tile is checked out for writing. */
        @Override public void addTileObserver(final TileObserver observer) {
            target().addTileObserver(observer);
        }

        /** Removes an observer from the list of observers notified when a tile is checked out for writing. */
        @Override public void removeTileObserver(final TileObserver observer) {
            target().removeTileObserver(observer);
        }

        /** Sets a region of the image to the contents of the given raster. */
        @Override public void setData(final Raster data) {
            final WritableRenderedImage target = target();
            final var executor = new TileOpExecutor(target, data.getBounds()) {
                @Override protected void writeTo(final WritableRaster tile) {
                    apply(tile).setRect(data);
                }
            };
            executor.writeTo(target);
        }

        /** Restores the identity behavior for writable image. */
        @Override public int hashCode() {
            return System.identityHashCode(this);
        }

        /** Restores the identity behavior for writable image. */
        @Override public boolean equals(final Object object) {
            return object == this;
        }
    }

    /**
     * Returns a hash code value for this image.
     */
    @Override
    public int hashCode() {
        return super.hashCode() + 97 * Arrays.hashCode(bands);
    }

    /**
     * Compares the given object with this image for equality.
     */
    @Override
    public boolean equals(final Object object) {
        if (super.equals(object)) {
            final BandSelectImage other = (BandSelectImage) object;
            return Arrays.equals(bands, other.bands);
        }
        return false;
    }
}
