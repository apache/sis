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
package org.apache.sis.storage.esri;

import java.io.IOException;
import java.nio.Buffer;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.BandedSampleModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.WritableRaster;
import static java.lang.Math.floorDiv;
import static java.lang.Math.addExact;
import static java.lang.Math.multiplyExact;
import static java.lang.Math.multiplyFull;
import static java.lang.Math.incrementExact;
import org.apache.sis.image.DataType;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridDerivation;
import org.apache.sis.coverage.privy.RangeArgument;
import org.apache.sis.image.privy.RasterFactory;
import org.apache.sis.image.privy.ImageUtilities;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.io.stream.HyperRectangleReader;
import org.apache.sis.io.stream.Region;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.util.ArraysExt;
import static org.apache.sis.util.privy.Numerics.wholeDiv;
import static org.apache.sis.pending.jdk.JDK18.ceilDiv;


/**
 * Helper class for reading a raw raster. The layout of data to read is defined by the {@link SampleModel}.
 * This class does not manage sample dimensions or color model; it is about sample values only.
 *
 * <p>This class is not thread-safe. Synchronization, if needed, shall be done by the caller.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class RawRasterReader extends HyperRectangleReader {
    /**
     * For identifying place in the code that are restricted to the two-dimensional case.
     */
    private static final int BIDIMENSIONAL = 2;

    /**
     * Index of a two-dimensional dimension selected by this reader.
     * Note that the (<var>x</var>,<var>y</var>) axis order also appears implicitly in some method calls;
     * modifying the values of those constants is not sufficient if different axis indices were wanted.
     */
    private static final int X_DIMENSION = 0, Y_DIMENSION = 1;

    /**
     * The full image size together with the "grid to CRS" transform.
     */
    final GridGeometry gridGeometry;

    /**
     * The enumeration value that represents the type of sample values.
     * Shall be consistent with {@code layout.getDataType()}.
     */
    private final DataType dataType;

    /**
     * Image layout, which describes also the layout of data to read.
     */
    final SampleModel layout;

    /**
     * Number of bytes to skip between band. This information is <em>not</em> stored
     * in the {@link SampleModel} and needs to be handled at reading time instead.
     * This is used with {@link BandedSampleModel} only.
     */
    private final int bandGapBytes;

    /**
     * Domain of the raster returned by the last {@code read(…)} operation.
     *
     * @see #getEffectiveDomain()
     */
    private GridGeometry effectiveDomain;

    /**
     * Creates a new reader for the given input.
     *
     * @param  gridGeometry  the full image size together with the "grid to CRS" transform.
     * @param  dataType      the type of sample value. Shall be consistent with {@code layout}.
     * @param  layout        the image layout, which describes also the layout of data to read.
     * @param  bandGapBytes  Number of bytes to skip between band. Used with {@link BandedSampleModel} only.
     * @param  input         the channel from which to read the values, together with a buffer for transferring data.
     * @throws DataStoreContentException if the given {@code dataType} is not one of the supported values.
     */
    public RawRasterReader(final GridGeometry gridGeometry, final DataType dataType, final SampleModel layout,
            final int bandGapBytes, final ChannelDataInput input) throws DataStoreContentException
    {
        super(ImageUtilities.toNumberEnum(dataType), input);
        this.gridGeometry = gridGeometry;
        this.dataType     = dataType;
        this.layout       = layout;
        this.bandGapBytes = bandGapBytes;
    }

    /**
     * Loads the data. After successful completion,
     * the domain effectively used can be obtained by {@link #getEffectiveDomain()}
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  range   indices of bands to load.
     * @return the raster for the specified domain.
     * @throws DataStoreException if an error occurred while reading the raster data.
     * @throws IOException if an I/O error occurred.
     */
    public WritableRaster read(GridGeometry domain, final RangeArgument range) throws DataStoreException, IOException {
        /*
         * The `fullSize`, `regionLower`, `regionUpper` and `subsampling` variables will be given (indirectly)
         * to the `HyperRectangleReader` for specifying the region to read. Their values depend not only on the
         * domain requested by the caller, but also on the image layout as defined by the sample model.
         */
        final int    width  = layout.getWidth();
        final int    height = layout.getHeight();
        final int    scanlineStride;        // Number of sample values per row.
        final int    pixelStrideNumerator;  // Numerator of the number of sample values per pixel.
        final int    pixelStrideDivisor;    // Value by which to divide `pixelStrideNumerator`.
        final long[] fullSize;              // The number of sample values along each dimension.
        final long[] regionLower;           // Indices of the first value to read along each dimension.
        final long[] regionUpper;           // Indices after the last value to read along each dimension.
        final long[] subsampling;           // Subsampling along each dimension. Shall be greater than zero.
        if (layout instanceof ComponentSampleModel) {
            final ComponentSampleModel cm = (ComponentSampleModel) layout;
            scanlineStride       = cm.getScanlineStride();
            pixelStrideNumerator = cm.getPixelStride();
            pixelStrideDivisor   = 1;
        } else {
            // This is the only other kind of sample model created by `RawRasterStore`.
            final MultiPixelPackedSampleModel cm = (MultiPixelPackedSampleModel) layout;
            scanlineStride       = cm.getScanlineStride();
            pixelStrideNumerator = cm.getPixelBitStride();
            pixelStrideDivisor   = DataBuffer.getDataTypeSize(layout.getDataType());
        }
        fullSize    = new long[] {scanlineStride, height};
        regionLower = new long[BIDIMENSIONAL];
        regionUpper = fullSize.clone();
        if (domain == null) {
            domain = gridGeometry;
            subsampling = new long[] {1, 1};
        } else {
            /*
             * Take in account the requested domain with the following restrictions:
             *
             * (1) If sample values are stored on 1, 2 or 4 bits, force the sub-region
             *     to be aligned on an integer number of sample values.
             * (2) If there is more than one band and those bands are stored in pixel
             *     interleaved fashion, we cannot apply subsampling on the X axis.
             * (3) If the layout is BIL, we do not support sub-region and subsampling
             *     on the X axis in current version.
             */
            int firstModifiableDimension = X_DIMENSION;
            final GridDerivation gd = gridGeometry.derive();
            if (pixelStrideDivisor > 1) {                                       // Restriction #1
                gd.chunkSize(pixelStrideDivisor / pixelStrideNumerator);
            }
            if (pixelStrideNumerator != pixelStrideDivisor) {                   // Restriction #2
                gd.maximumSubsampling(1);
            }
            if (layout.getClass() == ComponentSampleModel.class) {              // Restriction #3
                firstModifiableDimension = Y_DIMENSION;
                gd.chunkSize(scanlineStride);
                gd.maximumSubsampling(1);
            }
            final GridExtent ex = gd.subgrid(domain).getIntersection();
            for (int i=firstModifiableDimension; i<BIDIMENSIONAL; i++) {
                regionLower[i] = ex.getLow(i);
                regionUpper[i] = incrementExact(ex.getHigh(i));
            }
            if (X_DIMENSION >= firstModifiableDimension) {
                regionLower[X_DIMENSION] = floorDiv(multiplyExact(regionLower[X_DIMENSION], pixelStrideNumerator), pixelStrideDivisor);
                regionUpper[X_DIMENSION] =  ceilDiv(multiplyExact(regionUpper[X_DIMENSION], pixelStrideNumerator), pixelStrideDivisor);
            }
            subsampling = gd.getSubsampling();
            domain      = gd.build();
        }
        final Region region = new Region(fullSize, regionLower, regionUpper, subsampling);
        int   regionWidth   = region.getTargetSize(X_DIMENSION);
        int   regionHeight  = region.getTargetSize(Y_DIMENSION);
        /*
         * Now perform the actual reading of sample values. In the BSQ (Band sequential) case,
         * bands are read in the order they appear in the file (not necessarily the order requested by the caller).
         */
        final Buffer[] buffer;
        SampleModel sm = layout;
        boolean bandSubsetApplied = range.isIdentity();
        if (sm instanceof BandedSampleModel) {
            final var cm = (BandedSampleModel) sm;
            if (!(ArraysExt.allEquals(cm.getBandOffsets(), 0)) && ArraysExt.isRange(0, cm.getBankIndices())) {
                throw new DataStoreException("Not yet supported.");
            }
            final int   numBands    = range.getNumBands();
            final int[] bankIndices = ArraysExt.range(0, numBands);
            final int[] bandOffsets = new int[numBands];
            final long  bandStride  = addExact(multiplyFull(width, height), bandGapBytes);
            final long  origin      = getOrigin();
            buffer = new Buffer[numBands];
            try {
                for (int i=0; i<numBands; i++) {
                    final int band = range.getSourceIndex(i);
                    setOrigin(addExact(origin, multiplyExact(bandStride, band)));
                    buffer[range.getTargetIndex(i)] = readAsBuffer(region, 0);
                }
            } finally {
                setOrigin(origin);
            }
            if (!bandSubsetApplied) {
                sm = new BandedSampleModel(cm.getDataType(), width, height, scanlineStride, bankIndices, bandOffsets);
                bandSubsetApplied = true;
            }
        } else {
            /*
             * For all layout other than `BandedSampleModel` the current implementation read all bands
             * even if the user asked only a subset of them. The subseting is applied after the reading.
             */
            buffer = new Buffer[] {
                readAsBuffer(region, 0)
            };
            regionWidth = wholeDiv(regionWidth, sm.getNumBands());
        }
        /*
         * AT this point the data have been read. Adjust the sample model to the new data size (if different),
         * build the raster then apply band subseting if it was not done at reading time.
         */
        if (regionWidth != width || regionHeight != height) {
            sm = sm.createCompatibleSampleModel(regionWidth, regionHeight);
        }
        final DataBuffer data = RasterFactory.wrap(dataType, buffer);
        WritableRaster raster = WritableRaster.createWritableRaster(sm, data, null);
        if (!bandSubsetApplied) {
            raster = raster.createWritableChild(0, 0, raster.getWidth(), raster.getHeight(), 0, 0, range.getSelectedBands());
        }
        effectiveDomain = domain;
        return raster;
    }

    /**
     * Returns the domain of the raster returned by the last {@code read(…)} operation.
     * This method should be invoked only once per read operation.
     */
    final GridGeometry getEffectiveDomain() {
        final GridGeometry domain = effectiveDomain;
        effectiveDomain = null;
        return domain;
    }
}
