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
import java.awt.image.ColorModel;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;
import org.apache.sis.internal.coverage.j2d.ColorModelFactory;
import org.apache.sis.internal.jdk9.JDK9;


/**
 * Selects or reorder bands from a source image. This operation avoid copying sample values;
 * it works by modifying the sample model and color model.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.1
 * @module
 */
final class BandSelectImage extends SourceAlignedImage {
    /**
     * Properties to inherit from the source image, after bands reduction if applicable.
     *
     * @see #getProperty(String)
     */
    private static final Set<String> INHERITED_PROPERTIES = JDK9.setOf(
            GRID_GEOMETRY_KEY, POSITIONAL_ACCURACY_KEY,         // Properties to forward as-is.
            SAMPLE_RESOLUTIONS_KEY, STATISTICS_KEY);            // Properties to forward after band reduction.

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
    }

    /**
     * Creates a new "band select" operation for the given source.
     *
     * @param  source  the image in which to select bands.
     * @param  bands   the bands to select.
     */
    static RenderedImage create(final RenderedImage source, final int[] bands) {
        final int numBands = ImageUtilities.getNumBands(source);
        if (bands.length == numBands && ArraysExt.isRange(0, bands)) {
            return source;
        }
        ArgumentChecks.ensureNonEmpty("bands", bands, 0, numBands - 1, false);
        final ColorModel cm = ColorModelFactory.createSubset(source.getColorModel(), bands)
                .orElse(null);
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
        if (cm != null && source instanceof BufferedImage) {
            final BufferedImage bi = (BufferedImage) source;
            @SuppressWarnings("UseOfObsoleteCollectionType")
            final Hashtable<String,Object> properties = new Hashtable<>(8);
            for (final String key : INHERITED_PROPERTIES) {
                final Object value = getProperty(bi, key, bands);
                if (value != Image.UndefinedProperty) {
                    properties.put(key, value);
                }
            }
            return new BufferedImage(cm,
                    bi.getRaster().createWritableChild(0, 0, bi.getWidth(), bi.getHeight(), 0, 0, bands),
                    bi.isAlphaPremultiplied(), properties);
        }
        return new BandSelectImage(source, cm, bands.clone());
    }

    /**
     * Returns the names of all recognized properties,
     * or {@code null} if this image has no properties.
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
        if (value != null && (key.equals(SAMPLE_RESOLUTIONS_KEY) || key.equals(STATISTICS_KEY))) {
            final Class<?> componentType = value.getClass().getComponentType();
            if (componentType != null) {
                final Object reduced = Array.newInstance(componentType, bands.length);
                for (int i=0; i<bands.length; i++) {
                    Array.set(reduced, i, Array.get(value, bands[i]));
                }
                return reduced;
            }
        }
        return value;
    }

    /**
     * Creates a raster sharing the same data buffer than the source image but showing only a subset of the bands.
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
         * Alternatively we could have tried to do the work of `Raster.createChild(…)` method ourselves.
         * But we don't because that method is overridden in various Java2D `SunWritableRaster` classes.
         */
        return parent.createChild(x, y, parent.getWidth(), parent.getHeight(), x, y, bands);
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
