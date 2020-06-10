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
package org.apache.sis.coverage.grid;

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
import org.apache.sis.internal.feature.Resources;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;

// Branch-specific imports
import org.apache.sis.internal.jdk9.JDK9;
import org.opengis.coverage.CannotEvaluateException;


/**
 * A {@link GridCoverage} with data stored in an in-memory Java2D buffer.
 * Those data can be shown as {@link RenderedImage}.
 * Images are created when {@link #render(GridExtent)} is invoked instead than at construction time.
 * This delayed construction makes this class better suited to <var>n</var>-dimensional grids since
 * those grids can not be wrapped into a single {@link RenderedImage}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
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
     * Constructs a grid coverage using the specified grid geometry, sample dimensions and data buffer.
     * This method stores the given buffer by reference (no copy). The bands in the given buffer can be
     * stored either in a single bank (pixel interleaved image) or in different banks (banded image).
     * This class detects automatically which of those two sample models is used.
     *
     * <p>Note that {@link DataBuffer} does not contain any information about image size.
     * Consequently {@link #render(GridExtent)} depends on the domain {@link GridExtent},
     * which must be accurate. If the extent size does not reflect accurately the image size,
     * then the image will not be rendered properly.</p>
     *
     * @param  domain  the grid extent, CRS and conversion from cell indices to CRS.
     * @param  range   sample dimensions for each image band.
     * @param  data    the sample values, potentially multi-banded.
     * @throws NullPointerException if an argument is {@code null}.
     * @throws IllegalArgumentException if the data buffer has an incompatible number of banks.
     * @throws IllegalGridGeometryException if the grid extent is larger than the data buffer capacity.
     * @throws ArithmeticException if the grid extent is larger than 64 bits integer capacity.
     */
    public BufferedGridCoverage(final GridGeometry domain, final Collection<? extends SampleDimension> range, final DataBuffer data) {
        super(domain, range);
        this.data = data;
        ArgumentChecks.ensureNonNull("data", data);
        /*
         * The buffer shall either contain all values in a single bank (pixel interleaved sample model)
         * or contain as many banks as bands (banded sample model).
         */
        final int numBands = range.size();
        final int numBanks = data.getNumBanks();
        if (numBanks != 1 && numBands != numBands) {
            throw new IllegalArgumentException(Resources.format(Resources.Keys.MismatchedBandCount_2, numBands, numBands));
        }
        /*
         * Verify that the buffer has enough elements for all cells in grid extent.
         * Note that the buffer may have all elements in a single bank.
         */
        final GridExtent extent = domain.getExtent();
        final long expectedSize = getSampleCount(extent, numBands);
        final long bufferSize = JDK9.multiplyFull(data.getSize(), numBanks);
        if (bufferSize < expectedSize) {
            final StringBuilder b = new StringBuilder();
            for (int i=0; i < extent.getDimension(); i++) {
                if (i != 0) b.append(" × ");
                b.append(extent.getSize(i));
            }
            throw new IllegalGridGeometryException(Resources.format(
                    Resources.Keys.InsufficientBufferCapacity_3, b, numBands, expectedSize - bufferSize));
        }
    }

    /**
     * Constructs a grid coverage using the specified grid geometry, sample dimensions and data type.
     * This constructor creates a single-bank {@link DataBuffer} (pixel interleaved sample model) with
     * all sample values initialized to zero.
     *
     * @param  grid      the grid extent, CRS and conversion from cell indices to CRS.
     * @param  bands     sample dimensions for each image band.
     * @param  dataType  one of {@code DataBuffer.TYPE_*} constants, the native data type used to store the coverage values.
     * @throws ArithmeticException if the grid size is too large.
     */
    public BufferedGridCoverage(final GridGeometry grid, final Collection<? extends SampleDimension> bands, final int dataType) {
        super(grid, bands);
        final int n = Math.toIntExact(getSampleCount(grid.getExtent(), bands.size()));
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
     * Returns the number of cells in the given extent multiplied by the number of bands.
     *
     * @param  extent     the extent for which to get the number of cells.
     * @param  nbSamples  number of bands.
     * @return number of cells multiplied by the number of bands.
     * @throws ArithmeticException if the number of samples exceeds 64-bits integer capacity.
     */
    private static long getSampleCount(final GridExtent extent, long nbSamples) {
        for (int i = extent.getDimension(); --i >= 0;) {
            nbSamples = Math.multiplyExact(nbSamples, extent.getSize(i));
        }
        return nbSamples;
    }

    /**
     * Returns a two-dimensional slice of grid data as a rendered image.
     * This method returns a view; sample values are not copied.
     *
     * @return the grid slice as a rendered image.
     */
    @Override
    public RenderedImage render(final GridExtent sliceExtent) {
        final ImageRenderer renderer = new ImageRenderer(this, sliceExtent);
        try {
            renderer.setData(data);
            return renderer.image();
        } catch (IllegalArgumentException | ArithmeticException | RasterFormatException e) {
            throw new CannotEvaluateException(e.getMessage(), e);
        }
    }
}
