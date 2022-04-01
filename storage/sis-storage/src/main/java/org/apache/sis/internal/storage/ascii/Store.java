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
package org.apache.sis.internal.storage.ascii;

import java.util.Map;
import java.util.List;
import java.util.Collections;
import java.util.Optional;
import java.util.StringJoiner;
import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.awt.image.DataBufferDouble;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.datum.PixelInCell;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreClosedException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.internal.storage.PRJDataStore;
import org.apache.sis.internal.storage.RangeArgument;
import org.apache.sis.internal.storage.io.ChannelDataInput;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.sql.MetadataStoreException;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;


/**
 * A data store which creates grid coverages from an ESRI ASCII grid file.
 * The header contains (<var>key</var> <var>value</var>) pairs,
 * one pair per line and using spaces as separator between keys and values.
 * The package javadoc lists the recognized keywords.
 *
 * If we allow subclasses in a future version,
 * subclasses can add their own (<var>key</var>, <var>value</var>) pairs or modify
 * the existing ones by overriding the {@link #processHeader(Map)} method.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
final class Store extends PRJDataStore implements GridCoverageResource {
    /**
     * The object to use for reading data, or {@code null} if this store has been closed.
     */
    private CharactersView input;

    /**
     * The {@code NCOLS} and {@code NROWS} attributes read from the header.
     * Those values are valid only if {@link #gridGeometry} is non-null.
     */
    private int width, height;

    /**
     * The optional {@code NODATA_VALUE} attribute, or {@code NaN} if none.
     * This value is valid only if {@link #gridGeometry} is non-null.
     */
    private double fillValue;

    /**
     * The {@link #fillValue} as a text. This is useful when the fill value
     * can not be parsed as a {@code double} value, for example {@code "N/A"}.
     */
    private String fillText;

    /**
     * The image size together with the "grid to CRS" transform.
     * This is also used as a flag for checking whether the
     * {@code "*.prj"} file and the header have been read.
     */
    private GridGeometry gridGeometry;

    /**
     * Description of the single band contained in the ASCII Grid file.
     */
    private SampleDimension band;

    /**
     * The metadata object, or {@code null} if not yet created.
     */
    private DefaultMetadata metadata;

    /**
     * The full coverage, read when first requested then cached.
     * We cache the full coverage on the assumption that the
     * ASCII Grid format is not used for very large images.
     */
    private GridCoverage coverage;

    /**
     * Creates a new ASCII Grid store from the given file, URL or stream.
     *
     * @param  provider   the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @throws DataStoreException if an error occurred while opening the stream.
     */
    public Store(final StoreProvider provider, final StorageConnector connector) throws DataStoreException {
        super(provider, connector);
        fillValue = Double.NaN;
        input = new CharactersView(connector.commit(ChannelDataInput.class, StoreProvider.NAME));
        listeners.useWarningEventsOnly();
    }

    /**
     * Reads the {@code "*.prj"} file and the header if not already done.
     * This method does nothing if the data store is already initialized.
     * After a successful return, {@link #gridGeometry} is guaranteed non-null.
     *
     * <p>Note: we don't do this initialization in the constructor
     * for giving a chance for users to register listeners first.</p>
     */
    private void readHeader() throws DataStoreException {
        if (gridGeometry == null) try {
            final Map<String,String> header = input().readHeader();
            final Matrix3 gridToCRS = new Matrix3();
            PixelInCell anchor = PixelInCell.CELL_CORNER;
            String key = null;      // Used for error message if an exception is thrown.
            try {
                width  = Integer.parseInt(headerValue(header, key = "NCOLS"));
                height = Integer.parseInt(headerValue(header, key = "NROWS"));
                /*
                 * The ESRI ASCII Grid format has only a "CELLSIZE" property for both axes.
                 * The "DX" and "DY" properties are GDAL extensions and considered optional.
                 * If the de-facto standard "CELLSIZE" property exists, "DX" and "DY" will
                 * be considered unexpected.
                 */
                String value = header.remove(key = "CELLSIZE");
                if (value != null) {
                    gridToCRS.m00 = gridToCRS.m11 = Double.parseDouble(value);
                } else {
                    int def = 0;
                    value = header.remove(key = "DX"); if (value != null) {gridToCRS.m00 = Double.parseDouble(value); def |= 1;}
                    value = header.remove(key = "DY"); if (value != null) {gridToCRS.m11 = Double.parseDouble(value); def |= 2;}
                    if (def != 3) {
                        // Report "CELLSIZE" as the missing property because it is the de-facto standard one.
                        throw new DataStoreContentException(illegalValue(Errors.Keys.MissingValueForProperty_2, "CELLSIZE"));
                    }
                }
                /*
                 * Lower-left coordinates is specified either by CENTER or CORNER property.
                 * If both are missing, the error message reports that CORNER is missing.
                 */
                value = header.remove(key = "XLLCENTER");
                final boolean xCenter = (value != null);
                if (!xCenter) {
                    value = headerValue(header, key = "XLLCORNER");
                }
                gridToCRS.m02 = Double.parseDouble(value);
                value = header.remove(key = "YLLCENTER");
                final boolean yCenter = (value != null);
                if (!yCenter) {
                    value = headerValue(header, key = "YLLCORNER");
                }
                gridToCRS.m12 = Double.parseDouble(value);
                if (xCenter & yCenter) {
                    anchor = PixelInCell.CELL_CENTER;
                } else if (xCenter != yCenter) {
                    gridToCRS.convertBefore(xCenter ? 0 : 1, null, 0.5);
                }
                /*
                 * "No data" value is an optional property. Default value is NaN.
                 * This reader accepts a value specified as text.
                 */
                fillText = header.remove(key = "NODATA_VALUE");
                if (fillText != null) try {
                    fillValue = Double.parseDouble(fillText);
                } catch (NumberFormatException e) {
                    listeners.warning(illegalValue(Errors.Keys.IllegalValueForProperty_2, key), e);
                }
            } catch (NumberFormatException e) {
                throw new DataStoreContentException(illegalValue(Errors.Keys.IllegalValueForProperty_2, key), e);
            }
            /*
             * Read the auxiliary PRJ file after we finished parsing the header file.
             * A future version could skip this step if we add a non-standard "CRS" property in the header.
             */
            readPRJ();
            gridGeometry = new GridGeometry(new GridExtent(width, height), anchor, MathTransforms.linear(gridToCRS), crs);
            /*
             * If there is any unprocessed properties, log warnings about them.
             */
            if (!header.isEmpty()) {
                final StringJoiner joiner = new StringJoiner(", ");
                header.keySet().forEach(joiner::add);
                listeners.warning(Errors.getResources(getLocale()).getString(
                        Errors.Keys.UnexpectedProperty_2, input.input.filename, joiner.toString()));
            }
        } catch (DataStoreException e) {
            closeOnError(e);
            throw e;
        } catch (Exception e) {
            closeOnError(e);
            throw new DataStoreException(e);
        }
    }

    /**
     * Returns the error message for an exception or log record.
     *
     * @param  rk   {@link Errors.Keys#IllegalValueForProperty_2} or {@link Errors.Keys#MissingValueForProperty_2}.
     * @param  key  key of the header property which was requested.
     * @return the message to use in the exception to be thrown or the warning to be logged.
     */
    private String illegalValue(final short rk, final String key) {
        return Errors.getResources(getLocale()).getString(rk, input.input.filename, key);
    }

    /**
     * Gets a value from the header map and ensures that it is non-null.
     *
     * @param  header  map of (key, value) pair from the header.
     * @param  key     the name of the properties to get.
     * @return the value, guaranteed to be non-null.
     * @throws DataStoreException if the value was null.
     */
    private String headerValue(final Map<String,String> header, final String key) throws DataStoreException {
        final String value = header.remove(key);
        if (value == null) {
            throw new DataStoreContentException(illegalValue(Errors.Keys.MissingValueForProperty_2, key));
        }
        return value;
    }

    /**
     * Returns the metadata associated to the ASII grid file, or {@code null} if none.
     *
     * @return the metadata associated to the CSV file, or {@code null} if none.
     * @throws DataStoreException if an error occurred during the parsing process.
     */
    @Override
    public synchronized Metadata getMetadata() throws DataStoreException {
        if (metadata == null) {
            readHeader();
            final MetadataBuilder builder = new MetadataBuilder();
            try {
                builder.setFormat("ASCGRD");
            } catch (MetadataStoreException e) {
                builder.addFormatName(StoreProvider.NAME);
                listeners.warning(e);
            }
            builder.addEncoding(encoding, MetadataBuilder.Scope.METADATA);
            builder.addResourceScope(ScopeCode.COVERAGE, null);
            try {
                builder.addExtent(gridGeometry.getEnvelope());
            } catch (TransformException e) {
                throw new DataStoreReferencingException(getLocale(), StoreProvider.NAME, getDisplayName(), null).initCause(e);
            }
            addTitleOrIdentifier(builder);
            builder.setISOStandards(false);
            metadata = builder.buildAndFreeze();
        }
        return metadata;
    }

    /**
     * Returns the spatiotemporal extent of CSV data in coordinate reference system of the CSV file.
     *
     * @return the spatiotemporal resource extent.
     * @throws DataStoreException if an error occurred while computing the envelope.
     */
    @Override
    public Optional<Envelope> getEnvelope() throws DataStoreException {
        return Optional.ofNullable(getGridGeometry().getEnvelope());
    }

    /**
     * Returns the valid extent of grid coordinates together with the conversion from those grid
     * coordinates to real world coordinates.
     *
     * @return extent of grid coordinates together with their mapping to "real world" coordinates.
     * @throws DataStoreException if an error occurred while reading definitions from the underlying data store.
     */
    @Override
    public synchronized GridGeometry getGridGeometry() throws DataStoreException {
        readHeader();
        return gridGeometry;
    }

    /**
     * Returns the ranges of sample values together with the conversion from samples to real values.
     * ASCII Grid files always contain a single band.
     *
     * @return ranges of sample values together with their mapping to "real values".
     * @throws DataStoreException if an error occurred while reading definitions from the underlying data store.
     */
    @Override
    public synchronized List<SampleDimension> getSampleDimensions() throws DataStoreException {
        readHeader();
        if (band == null) {
            read(null, null);
        }
        return Collections.singletonList(band);
    }

    /**
     * Loads the data. If a non-null grid geometry is specified, then this method may return a sub-sampled image.
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  range   shall be either 0 or an containing only 0.
     * @return the grid coverage for the specified domain.
     * @throws DataStoreException if an error occurred while reading the grid coverage data.
     */
    @Override
    public synchronized GridCoverage read(final GridGeometry domain, final int... range) throws DataStoreException {
        RangeArgument.validate(1, range, listeners);
        if (coverage == null) try {
            readHeader();
            final CharactersView input = input();
            final double[] data = new double[width * height];
            double minimum = Double.POSITIVE_INFINITY;
            double maximum = Double.NEGATIVE_INFINITY;
            for (int i=0; i < data.length; i++) {
                final String token = input.readToken();
                double value;
                try {
                    value = Double.parseDouble(token);
                    if (value == fillValue) {
                        value = Double.NaN;
                    } else {
                        if (value < minimum) minimum = value;
                        if (value > maximum) maximum = value;
                    }
                } catch (NumberFormatException e) {
                    if (token.equals(fillText)) {
                        value = Double.NaN;
                    } else {
                        throw new DataStoreContentException(e);
                    }
                }
                data[i] = value;
            }
            if (!(minimum <= maximum)) {
                minimum = 0;
                maximum = 1;
            }
            final SampleDimension.Builder b = new SampleDimension.Builder();
            if (!Double.isNaN(fillValue)) {
                b.setBackground(null, fillValue);
            }
            b.addQuantitative(Vocabulary.formatInternational(Vocabulary.Keys.Values), minimum, maximum, null);
            band = b.build();
            coverage = new GridCoverageBuilder()
                    .addRange(band)
                    .setDomain(gridGeometry)
                    .setValues(new DataBufferDouble(data, data.length), null)
                    .build();
        } catch (IOException e) {
            closeOnError(e);
            throw new DataStoreException(e);
        }
        return coverage;
    }

    /**
     * Returns the input if it has not been closed.
     */
    private CharactersView input() throws DataStoreException {
        final CharactersView in = input;
        if (in == null) {
            throw new DataStoreClosedException(getLocale(), StoreProvider.NAME, StandardOpenOption.READ);
        }
        return in;
    }

    /**
     * Closes this data store and releases any underlying resources.
     *
     * @throws DataStoreException if an error occurred while closing this data store.
     */
    @Override
    public synchronized void close() throws DataStoreException {
        final CharactersView view = input;
        input = null;       // Cleared first in case of failure.
        if (view != null) try {
            view.input.channel.close();
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
    }

    /**
     * Closes this data store after an unrecoverable error occurred.
     * The caller is expected to throw the given exception after this method call.
     */
    private void closeOnError(final Throwable e) {
        try {
            close();
        } catch (Throwable s) {
            e.addSuppressed(s);
        }
    }
}
