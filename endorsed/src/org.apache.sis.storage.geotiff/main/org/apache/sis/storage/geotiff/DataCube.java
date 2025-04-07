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
package org.apache.sis.storage.geotiff;

import java.util.Optional;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.BandedSampleModel;
import static javax.imageio.plugins.tiff.BaselineTIFFTagSet.TAG_COMPRESSION;
import org.opengis.util.GenericName;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.geotiff.base.Tags;
import org.apache.sis.storage.geotiff.base.Resources;
import org.apache.sis.storage.geotiff.base.Predictor;
import org.apache.sis.storage.geotiff.base.Compression;
import org.apache.sis.storage.base.TiledGridResource;
import org.apache.sis.storage.base.StoreResource;
import org.apache.sis.math.Vector;


/**
 * One or many GeoTIFF images packaged as a single resource.
 * This is typically a single two-dimensional image represented as a {@link ImageFileDirectory}.
 * But it can also be a stack of images organized in a <var>n</var>-dimensional data cube,
 * or a pyramid of images with their overviews used when low resolution images is requested.
 *
 * <p><b>Warning:</b> do not implement {@link org.apache.sis.util.Localized},
 * as it may cause an infinite loop in {@code listeners.getLocale()} call.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class DataCube extends TiledGridResource implements StoreResource {
    /**
     * The GeoTIFF reader which contain this {@code DataCube}.
     * Used for fetching information like the input channel and where to report warnings.
     */
    final Reader reader;

    /**
     * Creates a new data cube.
     *
     * @param  reader  information about the input stream to read, the metadata and the character encoding.
     */
    DataCube(final Reader reader) {
        super(reader.store);
        this.reader = reader;
    }

    /**
     * Returns the data store that produced this resource.
     */
    @Override
    public final DataStore getOriginator() {
        return reader.store;
    }

    /**
     * Returns the object on which to perform all synchronizations for thread-safety.
     */
    @Override
    protected final Object getSynchronizationLock() {
        return reader.store;
    }

    /**
     * Access to the protected {@link #listeners} field.
     */
    final StoreListeners listeners() {
        return listeners;
    }

    /**
     * Shortcut for a frequently requested information.
     */
    final String filename() {
        return reader.input.filename;
    }

    /**
     * Returns an human-readable identification of this coverage.
     * The namespace should be the {@linkplain #filename() filename}
     * and the tip can be an image index, citation, or overview level.
     *
     * <p>The returned value should never be empty. An empty value would be a failure
     * to {@linkplain ImageFileDirectory#setOverviewIdentifier initialize overviews}.</p>
     *
     * @return a persistent identifier unique within the data store.
     * @throws DataStoreException if an error occurred while computing an identifier.
     */
    @Override
    public abstract Optional<GenericName> getIdentifier() throws DataStoreException;

    /**
     * Gets the paths to files used by this resource, or an empty value if unknown.
     */
    @Override
    public final Optional<FileSet> getFileSet() throws DataStoreException {
        return reader.store.getFileSet();
    }

    /**
     * Returns the number of components per pixel in the image stored in GeoTIFF file.
     * This the same value as the one returned by {@code getSampleModel(null).getNumBands()},
     * and is also the size of the collection returned by {@link #getSampleDimensions()}.
     *
     * @see #getSampleModel(int[])
     * @see SampleModel#getNumBands()
     */
    abstract int getNumBands();

    /**
     * Returns the total number of tiles. This is used for computing the stride between a
     * band and the next band in {@link #tileOffsets} and {@link #tileByteCounts} vectors.
     */
    abstract long getNumTiles();

    /**
     * Gets the stream position and the length in bytes of compressed tile arrays in the GeoTIFF file.
     * Values in the returned vector are {@code long} primitive type.
     *
     * @return stream position (relative to file beginning) and length of compressed tile arrays, in bytes.
     */
    abstract Vector[] getTileArrayInfo();

    /**
     * Returns {@code true} if {@link Integer#reverseBytes(int)} should be invoked on each byte read.
     * This mode is very rare and should apply only to uncompressed image or CCITT 1D/2D compressions.
     */
    abstract boolean isBitOrderReversed();

    /**
     * Returns the compression method, or {@code null} if unspecified.
     */
    abstract Compression getCompression();

    /**
     * Returns the mathematical operator that is applied to the image data before an encoding scheme is applied.
     * Should never be null; the default value is {@link Predictor#NONE}.
     */
    abstract Predictor getPredictor();

    /**
     * Returns the fill value used in image of floating point type before replacement by NaN.
     * If no value is declared or if the image type is an integer type, returns {@code null}.
     * If non-null, this value will be replaced by {@link Float#NaN} at reading time.
     *
     * <div class="note"><b>Rational:</b>
     * the intent is to handle the image as if it was already converted to the units of measurement.
     * Our netCDF reader does the same thing, and we want a consistent behavior of coverage readers.
     * </div>
     *
     * If this method returns a non-null value, then {@link #getFillValues(int[])} should return
     * an array of NaN.
     *
     * @return value to be replaced by NaN at reading time, or {@code null} if none.
     *
     * @see #getFillValues(int[])
     */
    abstract Number getReplaceableFillValue();

    /**
     * Allows the reading of truncated tiles in the <var>y</var> dimension.
     * Since there is no data after that dimension, it is safe to stop the
     * reading process as soon as possible along that axis of each tile.
     */
    @Override
    protected final boolean canReadTruncatedTiles(int dim, boolean suggested) {
        return suggested | (dim >= 1);      // Y_DIMENSION.
    }

    /**
     * Returns {@code true} if the image can be read with the {@link DataSubset} base class,
     * or {@code false} if the more sophisticated {@link CompressedSubset} sub-class is needed.
     * The {@link DataSubset#readSlice readSlice(…)} implementation in {@link DataSubset} base
     * class is more efficient but can be used only if all following conditions hold:
     *
     * <ul class="verbose">
     *   <li>The sample model stores each band in its own bank
     *       (this condition is relaxed if there is no band subset and no subsampling on the <var>x</var> axis).
     *       The reason for this restriction is because otherwise, the space skipped between values to read may
     *       be of irregular sizes, or the number of values to read between spaces may be greater than 1.</li>
     *   <li>There is only one sample value per bank element (i.e. no multi-pixels packed in single elements).</li>
     * </ul>
     *
     * If above conditions do not hold, then the less direct {@link CompressedSubset} subclass must be used
     * even if there is no compression.
     *
     * @todo The second restriction could be relaxed if the image width (or the width of the subregion to read)
     *       is a multiple of the number of sample values in a "bank element". For example, for a bilevel image
     *       storing 8 pixels in each single {@code byte}, we could return {@code true} if the region width is
     *       a multiple of 8.
     */
    private boolean canReadDirect(final Subset subset) throws DataStoreException {
        final SampleModel model = getSampleModel(null);
        int b = model.getNumBands();
        if (b != 1 && !(model instanceof BandedSampleModel)) {              // First condition (see Javadoc).
            if (!subset.isXContiguous()) {                                  // Exception to first consition.
                return false;
            }
        }
        final int dataSize = DataBuffer.getDataTypeSize(model.getDataType());
        do if (model.getSampleSize(--b) != dataSize) {                      // Second condition (see Javadoc).
            return false;
        } while (b != 0);
        return true;
    }

    /**
     * Creates a {@link GridCoverage} which will load pixel data in the given domain.
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  ranges  0-based index of sample dimensions to read, or an empty sequence for reading all ranges.
     * @return the grid coverage for the specified domain and ranges.
     * @throws DataStoreException if an error occurred while reading the grid coverage data.
     */
    @Override
    public final GridCoverage read(final GridGeometry domain, final int... ranges) throws DataStoreException {
        final long startTime = System.nanoTime();
        GridCoverage coverage;
        try {
            synchronized (getSynchronizationLock()) {
                final Subset subset = new Subset(domain, ranges);
                final Compression compression = getCompression();
                if (compression == null) {
                    throw new DataStoreContentException(reader.resources().getString(
                            Resources.Keys.MissingValue_2, Tags.name((short) TAG_COMPRESSION)));
                }
                /*
                 * The `DataSubset` parent class is the most efficient but has many limitations
                 * documented in the javadoc of its `readSlice(…)` method. If any precondition
                 * is not met, we need to fallback on the less direct `CompressedSubset` class.
                 */
                if (compression == Compression.NONE && getPredictor() == Predictor.NONE && canReadDirect(subset)) {
                    coverage = new DataSubset(this, subset);
                } else {
                    coverage = new CompressedSubset(this, subset);
                }
                coverage = preload(coverage);
            }
        } catch (RuntimeException e) {
            throw canNotRead(filename(), domain, e);
        }
        logReadOperation(reader.store.path, coverage.getGridGeometry(), startTime);
        return coverage;
    }
}
