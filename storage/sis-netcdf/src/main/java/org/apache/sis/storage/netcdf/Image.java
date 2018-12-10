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
package org.apache.sis.storage.netcdf;

import java.util.List;
import java.awt.image.DataBuffer;
import java.awt.image.ColorModel;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.internal.raster.RasterFactory;
import org.apache.sis.internal.raster.ColorModelFactory;


/**
 * Data loaded from a {@link GridResource}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class Image extends GridCoverage {
    /**
     * The sample values.
     */
    private final DataBuffer data;

    /**
     * Creates a new raster from the given resource.
     */
    Image(final GridGeometry domain, final List<SampleDimension> range, final DataBuffer data) {
        super(domain, range);
        this.data = data;
    }

    /**
     * Returns a two-dimensional slice of grid data as a rendered image.
     * This method tries to return a view as much as possible (i.e. sample values are not copied).
     *
     * @param  xAxis  dimension to use for <var>x</var> axis.
     * @param  yAxis  dimension to use for <var>y</var> axis.
     * @return the grid data as a rendered image in the given CRS dimensions.
     */
    @Override
    public RenderedImage asRenderedImage(final int xAxis, final int yAxis) {
        if (xAxis != 0 || yAxis != 1) {
            throw new IllegalArgumentException();       // TODO
        }
        final GridExtent extent = getGridGeometry().getExtent();
        final int width  = Math.toIntExact(extent.getSize(xAxis));
        final int height = Math.toIntExact(extent.getSize(yAxis));
        final WritableRaster raster = RasterFactory.createBandedRaster(data, width, height, width, null, null, null);
        final ColorModel colors = ColorModelFactory.create(data.getDataType());
        return new BufferedImage(colors, raster, false, null);
    }
}
