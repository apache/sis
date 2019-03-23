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
package org.apache.sis.internal.netcdf;

import java.util.List;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.RasterFormatException;
import org.opengis.coverage.CannotEvaluateException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.ImageRenderer;


/**
 * Data loaded from a {@link RasterResource} and potentially shown as {@link RenderedImage}.
 * The rendered image is usually mono-banded, but may be multi-banded in some special cases
 * handled by {@link RasterResource#read(GridGeometry, int...)}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class Raster extends GridCoverage {
    /**
     * The sample values, potentially multi-banded. If there is more than one band to put in the rendered image,
     * then each band is a {@linkplain DataBuffer#getNumBanks() separated bank} in the buffer, even if two banks
     * are actually wrapping the same arrays with different offsets.
     *
     * <div class="note">The later case is better represented by {@link java.awt.image.PixelInterleavedSampleModel},
     * but it is {@link ImageRenderer} responsibility to perform this substitution as an optimization.</div>
     */
    private final DataBuffer data;

    /**
     * Increment to apply on index for moving to the next pixel in the same band.
     */
    private final int pixelStride;

    /**
     * Name to display in error messages. Not to be used for processing.
     */
    private final String label;

    /**
     * Creates a new raster from the given resource.
     */
    Raster(final GridGeometry domain, final List<SampleDimension> range, final DataBuffer data, final int pixelStride,
            final String label)
    {
        super(domain, range);
        this.data        = data;
        this.label       = label;
        this.pixelStride = pixelStride;
    }

    /**
     * Returns a two-dimensional slice of grid data as a rendered image.
     * This returns a view as much as possible; sample values are not copied.
     */
    @Override
    public RenderedImage render(final GridExtent target) {
        try {
            final ImageRenderer renderer = new ImageRenderer(this, target);
            renderer.setData(data);
            renderer.setSampleStride(pixelStride);
            return renderer.image();
        } catch (IllegalArgumentException | ArithmeticException | RasterFormatException e) {
            throw new CannotEvaluateException(Resources.format(Resources.Keys.CanNotRender_2, label, e), e);
        }
    }
}
