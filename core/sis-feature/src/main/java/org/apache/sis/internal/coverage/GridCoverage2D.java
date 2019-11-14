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
package org.apache.sis.internal.coverage;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Collection;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.coverage.CannotEvaluateException;

/**
 * A {@link GridCoverage} with data stored in a {@link RenderedImage}.
 *
 * @author Johann Sorel (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
public class GridCoverage2D extends GridCoverage {
    /**
     * The sample values, stored as a RenderedImage.
     */
    protected final RenderedImage image;

    /**
     * Result of the call to {@link #forConvertedValues(boolean)}, created when first needed.
     */
    private GridCoverage converted;

    /**
     *
     * @param grid  the grid extent, CRS and conversion from cell indices to CRS.
     * @param bands sample dimensions for each image band.
     * @param image the sample values as a RenderedImage, potentially multi-banded in packed view.
     */
    public GridCoverage2D(final GridGeometry grid, final Collection<? extends SampleDimension> bands, final RenderedImage image) {
        super(grid, bands);
        this.image = image;
        ArgumentChecks.ensureNonNull("image", image);
    }

    /**
     * Returns a grid coverage that contains real values or sample values, depending if {@code converted} is {@code true}
     * or {@code false} respectively.
     *
     * If the given value is {@code false}, then the default implementation returns a grid coverage which produces
     * {@link RenderedImage} views. Those views convert each sample value on the fly. This is known to be very slow
     * if an entire raster needs to be processed, but this is temporary until another implementation is provided in
     * a future SIS release.
     *
     * @return a coverage containing converted or packed values, depending on {@code converted} argument value.
     */
    @Override
    public GridCoverage forConvertedValues(final boolean converted) {
        if (converted) {
            synchronized (this) {
                if (this.converted == null) {
                    this.converted = BufferedGridCoverage.convert(this);
                }
                return this.converted;
            }
        }
        return this;
    }

    /**
     * Returns a two-dimensional slice of grid data as a rendered image.
     * This method may return a view or a copy.
     *
     * @return the grid slice as a rendered image.
     */
    @Override
    public RenderedImage render(GridExtent sliceExtent) throws CannotEvaluateException {
        if (sliceExtent == null || sliceExtent.equals(getGridGeometry().getExtent())) {
            return image;
        } else {
            final int[] imgAxes = sliceExtent.getSubspaceDimensions(2);
            final int subX = Math.toIntExact(sliceExtent.getLow(imgAxes[0]));
            final int subY = Math.toIntExact(sliceExtent.getLow(imgAxes[1]));
            final int subWidth = Math.toIntExact(Math.round(sliceExtent.getSize(imgAxes[0])));
            final int subHeight = Math.toIntExact(Math.round(sliceExtent.getSize(imgAxes[1])));

            if (image instanceof BufferedImage) {
                final BufferedImage bi = (BufferedImage) image;
                return bi.getSubimage(subX, subY, subWidth, subHeight);
            } else {
                //todo : current approach makes a copy of the datas, a better solution should be found
                final WritableRaster raster = image.getTile(image.getMinTileX(), image.getMinTileY()).createCompatibleWritableRaster(subWidth, subHeight);
                final WritableRaster derivate = raster.createWritableTranslatedChild(subX, subY);
                image.copyData(derivate);
                ColorModel cm = image.getColorModel();
                return new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);
            }
        }
    }

}
