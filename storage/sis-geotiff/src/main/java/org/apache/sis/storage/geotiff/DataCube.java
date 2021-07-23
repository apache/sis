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
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.BandedSampleModel;
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
     * Returns the Java2D sample model describing pixel type and layout.
     * The raster size is the tile size as stored in the GeoTIFF file.
     *
     * <h4>Multi-dimensional data cube</h4>
     * If this resource has more than 2 dimensions, then this model is for the two first ones (usually horizontal).
     * The images for all levels in additional dimensions shall use the same sample model.
     *
     * @throws DataStoreContentException if the type is not recognized.
     */
    protected abstract SampleModel getSampleModel() throws DataStoreException;

    /**
     * Returns the Java2D color model for rendering images.
     *
     * @throws DataStoreContentException if the type is not recognized.
     */
    protected abstract ColorModel getColorModel() throws DataStoreException;

    /**
     * Returns the value to use for filling empty spaces in rasters,
     * or {@code null} if none, not different than zero or not valid for the target data type.
     * This value is used if a tile contains less pixels than expected.
     * The zero value is excluded because tiles are already initialized to zero by default.
     */
    protected abstract Number fillValue();

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
        final GridCoverage coverage;
        try {
            synchronized (reader.store) {
                final Subset subset = new Subset(this, domain, range);
                final Compression compression = getCompression();
                if (compression == null) {
                    throw new DataStoreContentException(reader.resources().getString(
                            Resources.Keys.MissingValue_2, Tags.name(Tags.Compression)));
                }
                switch (compression) {
                    case NONE: {
                        if (subset.hasSubsampling(0) && isInterleaved()) {
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
            }
        } catch (RuntimeException e) {
            throw new DataStoreException(reader.errors().getString(Errors.Keys.CanNotRead_1, filename()), e);
        }
        logReadOperation(reader.store.path, coverage.getGridGeometry(), startTime);
        return coverage;
    }
}
