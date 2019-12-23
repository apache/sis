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
package org.apache.sis.internal.coverage.j2d;

import java.util.Collection;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.RasterFormatException;
import java.awt.image.RenderedImage;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.ImageRenderer;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;

// Branch-specific imports
import org.opengis.coverage.CannotEvaluateException;


/**
 * A {@link GridCoverage} with data stored in an in-memory Java2D buffer.
 * Those data can be shown as {@link RenderedImage}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.0
 * @module
 */
public class BufferedGridCoverage extends GridCoverage {
    /**
     * The sample values, potentially multi-banded. The bands may be stored either in a single bank (pixel interleaved image)
     * or in different banks (banded image). This class detects automatically which of those two sample models is used when
     * {@link #render(GridExtent)} is invoked.
     */
    protected final DataBuffer data;

    /**
     * Result of the call to {@link #forConvertedValues(boolean)}, created when first needed.
     */
    private GridCoverage converted;

    /**
     * Constructs a grid coverage using the specified grid geometry, sample dimensions and data buffer.
     * This method stores the given buffer by reference (no copy).
     *
     * @param grid   the grid extent, CRS and conversion from cell indices to CRS.
     * @param bands  sample dimensions for each image band.
     * @param data   the sample values, potentially multi-banded.
     */
    public BufferedGridCoverage(final GridGeometry grid, final Collection<? extends SampleDimension> bands, final DataBuffer data) {
        super(grid, bands);
        this.data = data;
        ArgumentChecks.ensureNonNull("data", data);
    }

    /**
     * Constructs a grid coverage using the specified grid geometry, sample dimensions and data type.
     * This constructor create a single-bank {@link DataBuffer} (pixel interleaved sample model) with
     * all sample values initialized to zero.
     *
     * @param  grid      the grid extent, CRS and conversion from cell indices to CRS.
     * @param  bands     sample dimensions for each image band.
     * @param  dataType  one of {@code DataBuffer.TYPE_*} constants, the native data type used to store the coverage values.
     * @throws ArithmeticException if the grid size is too large.
     */
    public BufferedGridCoverage(final GridGeometry grid, final Collection<? extends SampleDimension> bands, final int dataType) {
        super(grid, bands);
        long nbSamples = bands.size();
        final GridExtent extent = grid.getExtent();
        for (int i = grid.getDimension(); --i >= 0;) {
            nbSamples = Math.multiplyExact(nbSamples, extent.getSize(i));
        }
        final int n = Math.toIntExact(nbSamples);
        switch (dataType) {
            case DataBuffer.TYPE_BYTE:   data = new DataBufferByte  (n); break;
            case DataBuffer.TYPE_SHORT:  data = new DataBufferShort (n); break;
            case DataBuffer.TYPE_USHORT: data = new DataBufferUShort(n); break;
            case DataBuffer.TYPE_INT:    data = new DataBufferInt   (n); break;
            case DataBuffer.TYPE_FLOAT:  data = new DataBufferFloat (n); break;
            case DataBuffer.TYPE_DOUBLE: data = new DataBufferDouble(n); break;
            default: throw new IllegalArgumentException(Errors.format(Errors.Keys.UnknownType_1, dataType));
        }
    }

    /**
     * Returns a two-dimensional slice of grid data as a rendered image.
     * This method returns a view; sample values are not copied.
     *
     * @return the grid slice as a rendered image.
     */
    @Override
    public RenderedImage render(final GridExtent sliceExtent) {
        try {
            final ImageRenderer renderer = new ImageRenderer(this, sliceExtent);
            renderer.setData(data);
            return renderer.image();
        } catch (IllegalArgumentException | ArithmeticException | RasterFormatException e) {
            throw new CannotEvaluateException(e.getMessage(), e);
        }
    }

    /**
     * Returns a grid coverage that contains real values or sample values, depending if {@code converted} is {@code true}
     * or {@code false} respectively.
     *
     * If the given value is {@code true}, then the default implementation returns a grid coverage which produces
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
                    this.converted = ConvertedGridCoverage.createFromPacked(this);
                }
                return this.converted;
            }
        }
        return this;
    }
}
