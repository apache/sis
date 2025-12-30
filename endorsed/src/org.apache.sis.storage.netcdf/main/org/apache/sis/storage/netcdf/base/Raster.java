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
package org.apache.sis.storage.netcdf.base;

import java.util.List;
import java.util.function.Function;
import java.awt.Color;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import org.apache.sis.coverage.Category;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.ImageRenderer;
import org.apache.sis.coverage.grid.BufferedGridCoverage;


/**
 * Data loaded from a {@link RasterResource} and potentially shown as {@link RenderedImage}.
 * The rendered image is usually mono-banded, but may be multi-banded in some special cases
 * handled by {@link RasterResource#read(GridGeometry, int...)}.
 *
 * <p>The inherited {@link #data} buffer contains the sample values, potentially multi-banded. If there is more than
 * one band to put in the rendered image, then each band is a {@linkplain DataBuffer#getNumBanks() separated bank}
 * in the buffer, even if two banks are actually wrapping the same arrays with different offsets.
 * The latter case is better represented by {@link java.awt.image.PixelInterleavedSampleModel},
 * but it is {@link ImageRenderer} responsibility to perform this substitution as an optimization.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
final class Raster extends BufferedGridCoverage {
    /**
     * Increment to apply on index for moving to the next pixel in the same band.
     */
    private final int pixelStride;

    /**
     * Offsets to add to sample index in each band, or {@code null} if none.
     * This is non-null only if a variable dimension is used for the bands.
     */
    private final int[] bandOffsets;

    /**
     * The band to use for defining pixel colors when the image is displayed on screen.
     * All other bands, if any, will exist in the raster but be ignored at display time.
     *
     * @see Convention#getVisibleBand()
     */
    private final int visibleBand;

    /**
     * The colors to use for each category, or {@code null} for default.
     * The function may return {@code null}, which means transparent.
     */
    private final Function<Category,Color[]> colors;

    /**
     * Creates a new raster from the given resource.
     *
     * @param domain       the grid extent, CRS and conversion from cell indices to CRS.
     * @param range        sample dimensions for each image band.
     * @param data         the sample values, potentially multi-banded.
     * @param pixelStride  increment to apply on index for moving to the next pixel in the same band.
     * @param bandOffsets  offsets to add to sample index in each band, or {@code null} if none.
     * @param visibleBand  the band to use for defining pixel colors when the image is displayed on screen.
     * @param colors       the colors to use for each category, or {@code null} for default.
     */
    Raster(final GridGeometry domain,
           final List<? extends SampleDimension> range,
           final DataBuffer data,
           final int pixelStride,
           final int[] bandOffsets,
           final int visibleBand,
           final Function<Category,Color[]> colors)
    {
        super(domain, range, data);
        this.colors      = colors;
        this.pixelStride = pixelStride;
        this.bandOffsets = bandOffsets;
        this.visibleBand = visibleBand;
    }

    /**
     * Configures a two-dimensional slice of grid data as a rendered image.
     */
    @Override
    protected void configure(final ImageRenderer renderer) {
        if (bandOffsets != null) {
            renderer.setInterleavedPixelOffsets(pixelStride, bandOffsets);
        }
        if (colors != null) {
            renderer.setCategoryColors(colors);
        }
        renderer.setVisibleBand(visibleBand);
    }
}
