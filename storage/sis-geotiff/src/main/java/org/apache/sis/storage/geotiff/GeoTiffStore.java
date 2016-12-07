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
import java.nio.charset.Charset;
import java.util.logging.LogRecord;
import java.nio.charset.StandardCharsets;
import org.opengis.util.FactoryException;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.maintenance.ScopeCode;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.internal.storage.ChannelDataInput;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.metadata.sql.MetadataStoreException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Classes;


/**
 * A data store backed by GeoTIFF files.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public class GeoTiffStore extends DataStore {
    /**
     * The encoding of strings in the metadata. The string specification said that is shall be US-ASCII,
     * but Apache SIS nevertheless let the user specifies an alternative encoding if needed.
     */
    final Charset encoding;

    /**
     * The GeoTIFF reader implementation, or {@code null} if none.
     */
    private Reader reader;

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
     * @param  storage  information about the storage (URL, stream, <i>etc</i>).
     * @throws DataStoreException if an error occurred while opening the GeoTIFF file.
     */
    public GeoTiffStore(final StorageConnector storage) throws DataStoreException {
        ArgumentChecks.ensureNonNull("storage", storage);
        final Charset encoding = storage.getOption(OptionKey.ENCODING);
        this.encoding = (encoding != null) ? encoding : StandardCharsets.US_ASCII;
        final ChannelDataInput input = storage.getStorageAs(ChannelDataInput.class);
        if (input == null) {
            throw new DataStoreException(Errors.format(Errors.Keys.IllegalInputTypeForReader_2,
                    "TIFF", Classes.getClass(storage.getStorage())));
        }
        storage.closeAllExcept(input);
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
            final MetadataBuilder builder = reader.metadata;
            try {
                builder.setFormat("GeoTIFF");
            } catch (MetadataStoreException e) {
                warning(null, e);
            }
            builder.add(encoding);
            builder.add(ScopeCode.COVERAGE);
            final Locale locale = getLocale();
            int n = 0;
            try {
                ImageFileDirectory dir;
                while ((dir = reader.getImageFileDirectory(n++)) != null) {
                    dir.completeMetadata(builder, locale);
                }
                metadata = builder.build(true);
            } catch (IOException e) {
                throw new DataStoreException(reader.errors().getString(Errors.Keys.CanNotRead_1, reader.input.filename), e);
            } catch (FactoryException | ArithmeticException e) {
                throw new DataStoreContentException(reader.canNotDecode(), e);
            }
        }
        return metadata;
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
