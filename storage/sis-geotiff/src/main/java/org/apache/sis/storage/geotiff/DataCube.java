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

import java.nio.file.Path;
import java.awt.image.SampleModel;
import java.awt.image.BandedSampleModel;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.internal.geotiff.Resources;
import org.apache.sis.internal.storage.TiledGridResource;
import org.apache.sis.internal.storage.ResourceOnFileSystem;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.math.Vector;


/**
 * One or many GeoTIFF images packaged as a single resource.
 * This is typically a single two-dimensional image represented as a {@link ImageFileDirectory}.
 * But it can also be a stack of images organized in a <var>n</var>-dimensional data cube,
 * or a pyramid of images with their overviews used when low resolution images is requested.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
abstract class DataCube extends TiledGridResource implements ResourceOnFileSystem {
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
     * Returns the object on which to perform all synchronizations for thread-safety.
     */
    @Override
    protected final DataStore getSynchronizationLock() {
        return reader.store;
    }

    /**
     * Shortcut for a frequently requested information.
     */
    final String filename() {
        return reader.input.filename;
    }

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
     * Returns the compression method, or {@code null} if unspecified.
     */
    abstract Compression getCompression();

    /**
     * Returns {@code true} if the sample model interleaves 2 or more sample values per pixel.
     */
    private boolean isInterleaved() throws DataStoreException {
        final SampleModel model = getSampleModel();
        return model.getNumBands() != 1 && !(model instanceof BandedSampleModel);
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
                final Subset subset = new Subset(domain, range, false);
                final Compression compression = getCompression();
                if (compression == null) {
                    throw new DataStoreContentException(reader.resources().getString(
                            Resources.Keys.MissingValue_2, Tags.name(Tags.Compression)));
                }
                switch (compression) {
                    case NONE: {
                        if (subset.hasBandSubset() || (subset.hasSubsampling(0) && isInterleaved())) {
                            coverage = new CompressedSubset(this, subset);
                        } else {
                            coverage = new DataSubset(this, subset);
                        }
                        break;
                    }
                    default: {
                        throw new DataStoreContentException(reader.resources().getString(
                                Resources.Keys.UnsupportedCompressionMethod_1, compression));
                    }
                }
                coverage = preload(coverage);
            }
        } catch (RuntimeException e) {
            throw new DataStoreException(reader.errors().getString(Errors.Keys.CanNotRead_1, filename()), e);
        }
        logReadOperation(reader.store.path, coverage.getGridGeometry(), startTime);
        return coverage;
    }
}
