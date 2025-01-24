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

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.RasterFormatException;
import java.awt.image.RenderedImage;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.image.DataType;

// Specific to the main branch:
import org.opengis.geometry.MismatchedDimensionException;
import org.apache.sis.coverage.CannotEvaluateException;
import org.apache.sis.coverage.PointOutsideCoverageException;


/**
 * Basic access to grid data values backed by a <var>n</var>-dimensional {@link DataBuffer}.
 * Those data can be shown as an untiled {@link RenderedImage}.
 * Images are created when {@link #render(GridExtent)} is invoked instead of at construction time.
 * This delayed construction makes this class better suited to <var>n</var>-dimensional grids
 * because those grids cannot be wrapped into a single {@link RenderedImage}.
 *
 * <p>The number of bands is determined by the number of {@link SampleDimension}s specified at construction time.
 * The {@linkplain DataBuffer#getNumBanks() number of banks} is either 1 or the number of bands.</p>
 *
 * <ul class="verbose">
 *   <li>If the number of banks is 1, all data are packed in a single array with band indices varying fastest,
 *       then column indices (<var>x</var>), then row indices (<var>y</var>), then other dimensions.</li>
 *   <li>If the number of banks is greater than 1, then each band is stored in a separated array.
 *       In each array, sample values are stored with column indices (<var>x</var>) varying fastest,
 *       then row indices (<var>y</var>), then other dimensions.
 *       In the two-dimensional case, this layout is also known as <dfn>row-major</dfn>.</li>
 * </ul>
 *
 * The number of cells in each dimension is specified by the {@link GridExtent} of the geometry given at
 * construction time. By default the {@linkplain GridExtent#getSize(int) extent size} in the two first dimensions
 * will define the {@linkplain RenderedImage#getWidth() image width} and {@linkplain RenderedImage#getHeight() height},
 * but different dimensions may be used depending on which dimensions are identified as the
 * {@linkplain GridExtent#getSubspaceDimensions(int) subspace dimensions}.
 *
 * <h2>Restrictions</h2>
 * This class expects all data to reside in-memory and does not support tiling.
 * Pixels are stored in a row-major fashion with all bands in a single array <em>or</em> one array per band.
 * By contrast, {@link GridCoverage2D} allows more flexibility in data layout and supports tiling with data
 * loaded or computed on-the-fly, but is restricted to two-dimensional images
 * (which may be slices in a <var>n</var>-dimensional grid).
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.1
 */
public class BufferedGridCoverage extends GridCoverage {
    /**
     * The sample values, potentially multi-banded. The bands may be stored either in a single bank
     * ({@linkplain java.awt.image.PixelInterleavedSampleModel pixel interleaved} image) or in different banks
     * ({@linkplain java.awt.image.BandedSampleModel banded} image). This class detects automatically
     * which of those two sample models is used when {@link #render(GridExtent)} is invoked.
     *
     * <p>Sample values in this buffer shall not be {@linkplain java.awt.image.SinglePixelPackedSampleModel packed}.</p>
     */
    protected final DataBuffer data;

    /**
     * Cache of rendered images produced by calls to {@link #render(GridExtent)}.
     * Those images are cached because, even if they are cheap to create,
     * they may become the source of a chain of operations for statistics,
     * {@linkplain org.apache.sis.image.ResampledImage image resampling}, <i>etc.</i>
     * Caching the source image preserves not only the {@link RenderedImage} instance created by the
     * {@link #render(GridExtent)} method, but also the chain of operations potentially derived from that image.
     *
     * <h4>Usage</h4>
     * Implementation of {@link #render(GridExtent)} method can be like below:
     *
     * {@snippet lang="java" :
     *     @Override
     *     public RenderedImage render(GridExtent sliceExtent) throws CannotEvaluateException {
     *         if (sliceExtent == null) {
     *             sliceExtent = gridGeometry.getExtent();
     *         }
     *         // Do some other verification if needed…
     *         // … then get or compute the image.
     *         try {
     *             return cachedRenderings.computeIfAbsent(sliceExtent, (slice) -> {
     *                 val renderer = new ImageRenderer(this, slice);
     *                 renderer.setData(data);
     *                 return renderer.createImage();
     *             });
     *         } catch (IllegalGridGeometryException | MismatchedDimensionException e) {
     *             throw e;
     *         } catch (IllegalArgumentException | ArithmeticException | RasterFormatException e) {
     *             throw new CannotEvaluateException(e.getMessage(), e);
     *         }
     *     }
     * }
     */
    private final Cache<GridExtent,RenderedImage> cachedRenderings;

    /**
     * Constructs a grid coverage using the specified grid geometry, sample dimensions and data buffer.
     * This method stores the given buffer by reference (no copy). The bands in the given buffer can be
     * stored either in a single bank (pixel interleaved image) or in different banks (banded image).
     * This class detects automatically which of those two sample models is used
     * (see class javadoc for more information).
     *
     * <p>Note that {@link DataBuffer} does not contain any information about image size.
     * Consequently, {@link #render(GridExtent)} depends on the domain {@link GridExtent},
     * which must be accurate. If the extent size does not reflect accurately the image size,
     * then the image will not be rendered properly.</p>
     *
     * @param  domain  the grid extent, CRS and conversion from cell indices to CRS.
     * @param  range   sample dimensions for each image band.
     * @param  data    the sample values, potentially multi-banded.
     * @throws NullPointerException if an argument is {@code null}.
     * @throws IllegalArgumentException if the data buffer has an incompatible number of banks.
     * @throws IllegalGridGeometryException if the grid extent is larger than the data buffer capacity.
     * @throws ArithmeticException if the number of cells is larger than 64 bits integer capacity.
     */
    public BufferedGridCoverage(final GridGeometry domain, final List<? extends SampleDimension> range, final DataBuffer data) {
        super(domain, range);
        this.data = Objects.requireNonNull(data);
        /*
         * The buffer shall either contain all values in a single bank (pixel interleaved sample model)
         * or contain as many banks as bands (banded sample model).
         */
        final int numBands = range.size();
        final int numBanks = data.getNumBanks();
        if (numBanks != 1 && numBanks != numBands) {
            throw new IllegalArgumentException(Resources.format(Resources.Keys.MismatchedBandCount_2, numBanks, numBands));
        }
        /*
         * Verify that the buffer has enough elements for all cells in grid extent.
         * Note that the buffer may have all elements in a single bank.
         */
        final GridExtent extent = domain.getExtent();
        final long expectedSize = getSampleCount(extent, numBands);
        final long bufferSize = Math.multiplyFull(data.getSize(), numBanks);
        if (bufferSize < expectedSize) {
            final var b = new StringBuilder();
            for (int i=0; i < extent.getDimension(); i++) {
                if (i != 0) b.append(" × ");
                b.append(extent.getSize(i));
            }
            throw new IllegalGridGeometryException(Resources.format(
                    Resources.Keys.InsufficientBufferCapacity_3, b, numBands, expectedSize - bufferSize));
        }
        cachedRenderings = new Cache<>();
    }

    /**
     * Constructs a grid coverage using the specified grid geometry, sample dimensions and data type.
     * This constructor creates a single-bank {@link DataBuffer} (pixel interleaved sample model)
     * with all sample values initialized to zero.
     *
     * @param  grid      the grid extent, CRS and conversion from cell indices to CRS.
     * @param  bands     sample dimensions for each image band.
     * @param  dataType  one of {@code DataBuffer.TYPE_*} constants, the native data type used to store the coverage values.
     * @throws ArithmeticException if the grid size is too large.
     */
    public BufferedGridCoverage(final GridGeometry grid, final List<? extends SampleDimension> bands, final int dataType) {
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
        cachedRenderings = new Cache<>();
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
     * Returns the constant identifying the primitive type used for storing sample values.
     */
    @Override
    final DataType getBandType() {
        return DataType.forDataBufferType(data.getDataType());
    }

    /**
     * Creates a new function for computing or interpolating sample values at given locations.
     *
     * <h4>Multi-threading</h4>
     * {@code Evaluator}s are not thread-safe. For computing sample values concurrently,
     * a new {@link Evaluator} instance should be created for each thread.
     */
    @Override
    public Evaluator evaluator() {
        return new CellAccessor(this);
    }

    /**
     * Returns a two-dimensional slice of grid data as a rendered image.
     * This method returns a view; sample values are not copied.
     *
     * <p>The default implementation prepares an {@link ImageRenderer},
     * then invokes {@link #configure(ImageRenderer)} for allowing subclasses
     * to complete the renderer configuration before to create the image.</p>
     *
     * @return the grid slice as a rendered image.
     */
    @Override
    public RenderedImage render(GridExtent sliceExtent) {
        if (sliceExtent == null) {
            sliceExtent = gridGeometry.extent;
        }
        try {
            return cachedRenderings.computeIfAbsent(sliceExtent, (slice) -> {
                var renderer = new ImageRenderer(this, slice);
                renderer.setData(data);
                return renderer.createImage();
            });
        } catch (IllegalGridGeometryException | MismatchedDimensionException e) {
            throw e;
        } catch (IllegalArgumentException | ArithmeticException | RasterFormatException e) {
            throw new CannotEvaluateException(e.getMessage(), e);
        }
    }

    /**
     * Invoked by the default implementation of {@link #render(GridExtent)}
     * for completing the renderer configuration before to create an image.
     * The default implementation does nothing.
     *
     * <p>Some examples of methods that subclasses may want to use are:</p>
     * <ul>
     *   <li>{@link ImageRenderer#setCategoryColors(Function)}</li>
     *   <li>{@link ImageRenderer#setVisibleBand(int)}</li>
     * </ul>
     *
     * @param  renderer  the renderer to configure before to create an image.
     *
     * @since 1.3
     */
    protected void configure(final ImageRenderer renderer) {
    }

    /**
     * Implementation of evaluator returned by {@link #evaluator()}.
     */
    private static final class CellAccessor extends DefaultEvaluator {
        /**
         * A copy of {@link BufferedGridCoverage#data} reference.
         */
        private final DataBuffer data;

        /**
         * The grid lower values. Those values need to be subtracted to each
         * grid coordinate before to compute index in {@link #data} buffer.
         */
        private final long[] lower;

        /**
         * Grid size with shifted index. The size of dimension 0 is stored at index 1, the size of dimension 1
         * is stored in index 2, <i>etc.</i>. Element 0 contains the pixel stride. This layout is convenient
         * for computing index in {@link DataBuffer}.
         */
        private final long[] sizes;

        /**
         * {@code true} for banded sample model, or {@code false} for pixel interleaved sample model.
         */
        private final boolean banded;

        /**
         * Creates a new evaluator for the specified coverage.
         */
        CellAccessor(final BufferedGridCoverage coverage) {
            super(coverage);
            final GridExtent extent = coverage.getGridGeometry().getExtent();
            final int numBands = coverage.getSampleDimensions().size();
            data     = coverage.data;
            banded   = data.getNumBanks() > 1;
            values   = new double[numBands];
            lower    = new long[extent.getDimension()];
            sizes    = new long[lower.length + 1];
            sizes[0] = banded ? 1 : numBands;
            for (int i=0; i<lower.length; i++) {
                lower[i]   = extent.getLow(i);
                sizes[i+1] = extent.getSize(i);
            }
        }

        /**
         * Returns a sequence of double values for a given point in the coverage.
         * The CRS of the given point may be any coordinate reference system,
         * or {@code null} for the same CRS as the coverage.
         */
        @Override
        public double[] apply(final DirectPosition point) throws CannotEvaluateException {
            final int pos;
            try {
                final FractionalGridCoordinates gc = toGridPosition(point);
                int  i = lower.length;
                long s = sizes[i];
                long index = 0;
                while (--i >= 0) {
                    final long low = lower[i];
                    long p = gc.getCoordinateValue(i);
                    // (p - low) may overflow, so we must test (p < low) before.
                    if (p < low || (p -= low) >= s) {
                        if (isNullIfOutside()) {
                            return null;
                        }
                        throw new PointOutsideCoverageException(
                                gc.pointOutsideCoverage(getCoverage().gridGeometry.extent));
                    }
                    /*
                     * Following should never overflow, otherwise BufferedGridCoverage
                     * constructor would have failed. Should never be negative neither.
                     */
                    index = (index + p) * (s = sizes[i]);
                }
                /*
                 * Failure on next line would not be caused by a point outside coverage bounds.
                 * So it should not be rethrown as PointOutsideCoverageException.
                 */
                pos = Math.toIntExact(index);
            } catch (ArithmeticException | FactoryException | TransformException ex) {
                throw new CannotEvaluateException(ex.getMessage(), ex);
            }
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final double[] values = this.values;
            if (banded) {
                for (int i=0; i<values.length; i++) {
                    values[i] = data.getElemDouble(i, pos);
                }
            } else {
                for (int i=0; i<values.length; i++) {
                    values[i] = data.getElemDouble(i + pos);
                }
            }
            return values;
        }
    }
}
