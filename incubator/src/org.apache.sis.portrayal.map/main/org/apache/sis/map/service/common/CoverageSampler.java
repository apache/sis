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
package org.apache.sis.map.service.common;

import java.awt.Point;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.logging.Level;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageProcessor;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.image.Interpolation;
import org.apache.sis.image.PixelIterator;
import org.apache.sis.map.service.Scene2D;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;


/**
 * Resamples two-dimensional coverages on a target grid, usually the display grid.
 * The sampled values are the converted (geophysics) values.
 *
 * <ul>
 *   <li>A target cell is inside the coverage if its center is inside a coverage cell.</li>
 *   <li>Target cells outside the coverage have no value (NaN),
 *       including when the resampling fills them with a value such as 0 for integer data.</li>
 *   <li>The resampling does not apply optimizations which would change the values, such as reducing the precision.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class CoverageSampler {
    /**
     * Number of bits of the integer bands whose values are taken as 0 … 255 color components.
     */
    private static final int BYTE_SIZE = 8;

    /**
     * The two-dimensional target grid.
     */
    private final GridGeometry target;

    /**
     * Number of cells in a row and number of rows.
     */
    private final int width, height;

    /**
     * Creates a sampler on the given grid.
     *
     * @param  target  the two-dimensional target grid.
     */
    public CoverageSampler(final GridGeometry target) {
        this.target = target;
        final GridExtent extent = target.getExtent();
        width  = Math.toIntExact(extent.getSize(0));
        height = Math.toIntExact(extent.getSize(1));
    }

    /**
     * Returns the two-dimensional target grid.
     *
     * @return the grid of the sampled cells.
     */
    public GridGeometry getTarget() {
        return target;
    }

    /**
     * Returns the number of cells of the target grid.
     *
     * @return number of cells.
     */
    public int size() {
        return width * height;
    }

    /**
     * Returns the given coverage resampled on the target grid, as an image.
     * The values are not converted: packed values remain packed, for example for keeping the colors of the coverage.
     * Pixel (0,0) of the image is the low corner of the target extent.
     *
     * @param  coverage       the two-dimensional coverage.
     * @param  interpolation  the interpolation of the values between coverage cells.
     * @return the resampled image.
     * @throws TransformException if the coverage cannot be resampled on the target grid.
     */
    public RenderedImage resample(final GridCoverage coverage, final Interpolation interpolation) throws TransformException {
        final GridCoverageProcessor processor = new GridCoverageProcessor();
        processor.setInterpolation(interpolation);
        processor.setOptimizations(EnumSet.noneOf(GridCoverageProcessor.Optimization.class));
        return processor.resample(coverage, target).render(null);
    }

    /**
     * Returns the converted values of all bands of the given coverage on the cells of the target grid.
     *
     * @param  coverage       the two-dimensional coverage.
     * @param  interpolation  the interpolation of the values between coverage cells.
     * @return the values of each target cell.
     * @throws TransformException if the coverage cannot be resampled on the target grid.
     */
    public SampleTable sample(final GridCoverage coverage, final Interpolation interpolation) throws TransformException {
        final int[] bands = new int[coverage.getSampleDimensions().size()];
        Arrays.setAll(bands, (band) -> band);
        return sample(coverage, interpolation, bands);
    }

    /**
     * Returns the converted values of the given bands of the coverage on the cells of the target grid.
     * The bands of the table are in the order of the given indices.
     *
     * @param  coverage       the two-dimensional coverage.
     * @param  interpolation  the interpolation of the values between coverage cells.
     * @param  bands          indices of the coverage bands to sample.
     * @return the values of each target cell.
     * @throws TransformException if the coverage cannot be resampled on the target grid.
     */
    public SampleTable sample(final GridCoverage coverage, final Interpolation interpolation, final int[] bands)
            throws TransformException
    {
        final RenderedImage image = resample(coverage.forConvertedValues(true), interpolation);
        final SampleModel model = image.getSampleModel();
        final int numBands = model.getNumBands();
        final double[][] values = new double[bands.length][width * height];
        final boolean[] bytes = new boolean[bands.length];
        for (int i = 0; i < bands.length; i++) {
            Arrays.fill(values[i], Double.NaN);
            if (bands[i] < numBands) {
                bytes[i] = model.getDataType() <= DataBuffer.TYPE_INT && model.getSampleSize(bands[i]) == BYTE_SIZE;
            }
        }
        final boolean[] inside = inside(coverage.getGridGeometry());
        final double[] pixel = new double[numBands];
        final PixelIterator it = PixelIterator.create(image);
        while (it.next()) {
            final Point p = it.getPosition();       // Relative to the low corner of the target extent.
            if (p.x >= 0 && p.y >= 0 && p.x < width && p.y < height) {
                final int cell = p.y * width + p.x;
                if (inside[cell]) {
                    it.getPixel(pixel);
                    for (int i = 0; i < bands.length; i++) {
                        if (bands[i] < numBands) {
                            values[i][cell] = pixel[bands[i]];
                        }
                    }
                }
            }
        }
        return new SampleTable(width, height, values, bytes, inside);
    }

    /**
     * Returns whether the center of each target cell is inside a cell of the given grid.
     * If the position of a row cannot be computed, the cells of that row are considered inside.
     */
    private boolean[] inside(final GridGeometry source) throws TransformException {
        final boolean[] inside = new boolean[width * height];
        final MathTransform toSource = target.createTransformTo(source, PixelInCell.CELL_CENTER);
        final GridExtent extent = target.getExtent();
        final GridExtent bounds = source.getExtent();
        final long x0 = extent.getLow(0);
        final double[] row = new double[2 * width];
        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                row[2*x]     = x0 + x;
                row[2*x + 1] = extent.getLow(1) + y;
            }
            try {
                toSource.transform(row, 0, row, 0, width);
            } catch (TransformException e) {
                Scene2D.LOGGER.log(Level.FINE, "Cannot locate a row of cells in the coverage.", e);
                Arrays.fill(inside, y * width, (y + 1) * width, true);
                continue;
            }
            for (int x=0; x<width; x++) {
                inside[y * width + x] = contains(bounds, 0, row[2*x]) && contains(bounds, 1, row[2*x + 1]);
            }
        }
        return inside;
    }

    /**
     * Returns whether the given grid coordinate, with integer values at cell centers, is inside the extent.
     */
    private boolean contains(final GridExtent bounds, final int dimension, final double coordinate) {
        final double index = Math.floor(coordinate + 0.5);
        return index >= bounds.getLow(dimension) && index <= bounds.getHigh(dimension);
    }
}
