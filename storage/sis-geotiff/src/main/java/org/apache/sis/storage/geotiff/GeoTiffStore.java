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
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.logging.LogRecord;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import org.opengis.util.FactoryException;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.UnsupportedStorageException;
import org.apache.sis.storage.DataStoreClosedException;
import org.apache.sis.storage.event.ChangeEvent;
import org.apache.sis.storage.event.ChangeListener;
import org.apache.sis.internal.storage.io.ChannelDataInput;
import org.apache.sis.internal.storage.io.IOUtilities;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.internal.storage.URIDataStore;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.metadata.sql.MetadataStoreException;
import org.apache.sis.util.resources.Errors;


/**
 * A data store backed by GeoTIFF files.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Thi Phuong Hao Nguyen (VNSC)
 * @version 1.0
 * @since   0.8
 * @module
 */
public class GeoTiffStore extends DataStore {
    /**
     * The encoding of strings in the metadata. The string specification said that is shall be US-ASCII,
     * but Apache SIS nevertheless let the user specifies an alternative encoding if needed.
     */
    final Charset encoding;

    /**
     * The GeoTIFF reader implementation, or {@code null} if the store has been closed.
     */
    private Reader reader;

    /**
     * The {@link GeoTiffStoreProvider#LOCATION} parameter value, or {@code null} if none.
     */
    private final URI location;

    /**
     * The metadata, or {@code null} if not yet created.
     *
     * @see #getMetadata()
     */
    private Metadata metadata;

    /**
     * Creates a new GeoTIFF store from the given file, URL or stream object.
     * This constructor invokes {@link StorageConnector#closeAllExcept(Object)},
     * keeping open only the needed resource.
     *
     * @param  provider   the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @throws DataStoreException if an error occurred while opening the GeoTIFF file.
     */
    public GeoTiffStore(final GeoTiffStoreProvider provider, final StorageConnector connector) throws DataStoreException {
        super(provider, connector);
        final Charset encoding = connector.getOption(OptionKey.ENCODING);
        this.encoding = (encoding != null) ? encoding : StandardCharsets.US_ASCII;
        final ChannelDataInput input = connector.getStorageAs(ChannelDataInput.class);
        if (input == null) {
            throw new UnsupportedStorageException(super.getLocale(), Constants.GEOTIFF,
                    connector.getStorage(), connector.getOption(OptionKey.OPEN_OPTIONS));
        }
        location = connector.getStorageAs(URI.class);
        connector.closeAllExcept(input);
        try {
            reader = new Reader(this, input);
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
    }

    /**
     * Returns information about the dataset as a whole. The returned metadata object can contain information
     * such as the spatiotemporal extent of the dataset, contact information about the creator or distributor,
     * data quality, usage constraints and more.
     *
     * @return information about the dataset.
     * @throws DataStoreException if an error occurred while reading the data.
     */
    @Override
    public synchronized Metadata getMetadata() throws DataStoreException {
        if (metadata == null) {
            final Reader reader = reader();
            final MetadataBuilder builder = reader.metadata;
            try {
                builder.setFormat(Constants.GEOTIFF);
            } catch (MetadataStoreException e) {
                warning(null, e);
            }
            builder.addEncoding(encoding, MetadataBuilder.Scope.METADATA);
            builder.addResourceScope(ScopeCode.valueOf("COVERAGE"), null);
            final Locale locale = getLocale();
            int n = 0;
            try {
                ImageFileDirectory dir;
                while ((dir = reader.getImageFileDirectory(n++)) != null) {
                    dir.completeMetadata(builder, locale);
                }
            } catch (IOException e) {
                throw new DataStoreException(errors().getString(Errors.Keys.CanNotRead_1, reader.input.filename), e);
            } catch (FactoryException | ArithmeticException e) {
                throw new DataStoreContentException(getLocale(), Constants.GEOTIFF, reader.input.filename, null).initCause(e);
            }
            /*
             * Add the filename as an identifier only if the input was something convertible to URI (URL, File or Path),
             * otherwise reader.input.filename may not be useful; it may be just the InputStream classname. If the TIFF
             * file did not specified any ImageDescription tag, then we will had the filename as a title instead than an
             * identifier because the title is mandatory in ISO 19115 metadata.
             */
            if (location != null) {
                builder.addTitleOrIdentifier(IOUtilities.filenameWithoutExtension(reader.input.filename), MetadataBuilder.Scope.ALL);
            }
            metadata = builder.build(true);
        }
        return metadata;
    }

    /**
     * Returns the parameters used to open this GeoTIFF data store.
     * If non-null, the parameters are described by {@link GeoTiffStoreProvider#getOpenParameters()} and contains at
     * least a parameter named {@value org.apache.sis.storage.DataStoreProvider#LOCATION} with a {@link URI} value.
     * This method may return {@code null} if the storage input can not be described by a URI
     * (for example a GeoTIFF file reading directly from a {@link java.nio.channels.ReadableByteChannel}).
     *
     * @return parameters used for opening this data store, or {@code null} if not available.
     *
     * @since 0.8
     */
    @Override
    public ParameterValueGroup getOpenParameters() {
        return URIDataStore.parameters(provider, location);
    }

    /**
     * Returns the reader if it is not closed, or thrown an exception otherwise.
     */
    private Reader reader() throws DataStoreException {
        final Reader r = reader;
        if (r == null) {
            throw new DataStoreClosedException(getLocale(), Constants.GEOTIFF, StandardOpenOption.READ);
        }
        return r;
    }

    /**
     * Ignored in current implementation, since this resource produces no events.
     *
     * @param  <T>        {@inheritDoc}
     * @param  listener   {@inheritDoc}
     * @param  eventType  {@inheritDoc}
     */
    @Override
    public <T extends ChangeEvent> void addListener(ChangeListener<? super T> listener, Class<T> eventType) {
    }

    /**
     * Ignored in current implementation, since this resource produces no events.
     *
     * @param  <T>        {@inheritDoc}
     * @param  listener   {@inheritDoc}
     * @param  eventType  {@inheritDoc}
     */
    @Override
    public <T extends ChangeEvent> void removeListener(ChangeListener<? super T> listener, Class<T> eventType) {
    }

    /**
     * Closes this GeoTIFF store and releases any underlying resources.
     *
     * @throws DataStoreException if an error occurred while closing the GeoTIFF file.
     */
    @Override
    public synchronized void close() throws DataStoreException {
        final Reader r = reader;
        reader = null;
        if (r != null) try {
            r.close();
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
    }

    /**
     * Returns the error resources in the current locale.
     */
    final Errors errors() {
        return Errors.getResources(getLocale());
    }

    /**
     * Reports a warning represented by the given message and exception.
     * At least one of {@code message} and {@code exception} shall be non-null.
     *
     * @param message    the message to log, or {@code null} if none.
     * @param exception  the exception to log, or {@code null} if none.
     */
    final void warning(final String message, final Exception exception) {
        listeners.warning(message, exception);
    }

    /**
     * Reports a warning contained in the given {@link LogRecord}.
     * Note that the given record will not necessarily be sent to the logging framework;
     * if the user as registered at least one listener, then the record will be sent to the listeners instead.
     *
     * <p>This method sets the {@linkplain LogRecord#setSourceClassName(String) source class name} and
     * {@linkplain LogRecord#setSourceMethodName(String) source method name} to hard-coded values.
     * Those values assume that the warnings occurred indirectly from a call to {@link #getMetadata()}
     * in this class. We do not report private classes or methods as the source of warnings.</p>
     *
     * @param  record  the warning to report.
     */
    final void warning(final LogRecord record) {
        // Logger name will be set by listeners.warning(record).
        record.setSourceClassName(GeoTiffStore.class.getName());
        record.setSourceMethodName("getMetadata");
        listeners.warning(record);
    }
}
