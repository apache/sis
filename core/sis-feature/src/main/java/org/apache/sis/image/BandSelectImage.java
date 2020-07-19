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

import java.awt.image.Raster;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.ColorModel;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;
import org.apache.sis.internal.coverage.j2d.ColorModelFactory;


/**
 * Selects or reorder bands from a source image. This operation avoid copying sample values;
 * it works by modifying the sample model and color model.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class BandSelectImage extends SourceAlignedImage {
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
        final ColorModel cm = ColorModelFactory.createSubsetColorModel(source.getColorModel(), bands);
        /*
         * If the image is an instance of `BufferedImage`, create the subset immediately
         * (reminder: this operation will not copy pixel data). It allows us to return a
         * new instance of `BufferedImage`, which has optimization in Java2D.
         */
        if (source instanceof BufferedImage) {
            final BufferedImage bi = (BufferedImage) source;
            return new BufferedImage(cm,
                    bi.getRaster().createWritableChild(0, 0, bi.getWidth(), bi.getHeight(), 0, 0, bands),
                    bi.isAlphaPremultiplied(), null);
        }
        return new BandSelectImage(source, cm, bands.clone());
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
}
