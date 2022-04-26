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

import java.util.Locale;
import java.util.Optional;
import java.nio.file.Path;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.BandedSampleModel;
import org.opengis.util.GenericName;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.internal.geotiff.Resources;
import org.apache.sis.internal.geotiff.Predictor;
import org.apache.sis.internal.geotiff.Compression;
import org.apache.sis.internal.storage.TiledGridResource;
import org.apache.sis.internal.storage.ResourceOnFileSystem;
import org.apache.sis.internal.storage.StoreResource;
import org.apache.sis.math.Vector;


/**
 * One or many GeoTIFF images packaged as a single resource.
 * This is typically a single two-dimensional image represented as a {@link ImageFileDirectory}.
 * But it can also be a stack of images organized in a <var>n</var>-dimensional data cube,
 * or a pyramid of images with their overviews used when low resolution images is requested.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
abstract class DataCube extends TiledGridResource implements ResourceOnFileSystem, StoreResource {
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
        super(reader.store.listeners());
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
     * Returns the locale for warnings and error messages.
     *
     * <p><b>Warning:</b> do not implement {@link org.apache.sis.util.Localized},
     * as it may cause an infinite loop in {@code listeners.getLocale()} call.</p>
     */
    final Locale getLocale() {
        return listeners.getLocale();
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
     */
    @Override
    public abstract Optional<GenericName> getIdentifier();

    /**
     * Gets the paths to files used by this resource, or an empty array if unknown.
     */
    @Override
    public final Path[] getComponentFiles() {
        final Path location = reader.store.path;
        return (location != null) ? new Path[] {location} : new Path[0];
    }

    /**
     * Returns the number of components per pixel in the image stored in GeoTIFF file.
     * This the same value than the one returned by {@code getSampleModel().getNumBands()},
     * and is also the size of the collection returned by {@link #getSampleDimensions()}.
     *
     * @see #getSampleModel()
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
     *       is a multiple of the number of sample values in a "bank element". For example for a bilevel image
     *       storing 8 pixels in each single {@code byte}, we could return {@code true} if the region width is
     *       a multiple of 8.
     */
    private boolean canReadDirect(final Subset subset) throws DataStoreException {
        final SampleModel model = getSampleModel();
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
     * @param  range   0-based index of sample dimensions to read, or an empty sequence for reading all ranges.
     * @return the grid coverage for the specified domain and range.
     * @throws DataStoreException if an error occurred while reading the grid coverage data.
     */
    @Override
    public final GridCoverage read(final GridGeometry domain, final int... range) throws DataStoreException {
        final long startTime = System.nanoTime();
        GridCoverage coverage;
        try {
            synchronized (getSynchronizationLock()) {
                final Subset subset = new Subset(domain, range);
                final Compression compression = getCompression();
                if (compression == null) {
                    throw new DataStoreContentException(reader.resources().getString(
                            Resources.Keys.MissingValue_2, Tags.name(Tags.Compression)));
                }
                /*
                 * The `DataSubset` parent class is the most efficient but has many limitations
                 * documented in the javadoc of its `readSlice(…)` method. If any pre-condition
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
            throw canNotRead(reader.input.filename, domain, e);
        }
        logReadOperation(reader.store.path, coverage.getGridGeometry(), startTime);
        return coverage;
    }
}
