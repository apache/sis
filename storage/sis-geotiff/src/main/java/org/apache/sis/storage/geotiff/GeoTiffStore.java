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
import java.util.List;
import java.util.Optional;
import java.util.logging.LogRecord;
import java.net.URI;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import org.opengis.util.NameSpace;
import org.opengis.util.NameFactory;
import org.opengis.util.GenericName;
import org.opengis.util.FactoryException;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.UnsupportedStorageException;
import org.apache.sis.storage.DataStoreClosedException;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.event.WarningEvent;
import org.apache.sis.internal.storage.io.ChannelDataInput;
import org.apache.sis.internal.storage.io.IOUtilities;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.internal.storage.StoreUtilities;
import org.apache.sis.internal.storage.URIDataStore;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.internal.util.ListOfUnknownSize;
import org.apache.sis.metadata.sql.MetadataStoreException;
import org.apache.sis.util.collection.BackingStoreException;
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
public class GeoTiffStore extends DataStore implements Aggregate {
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
     * This is used for information purpose only, not for actual reading operations.
     *
     * @see #getOpenParameters()
     */
    private final URI location;

    /**
     * The data store identifier created from the filename, or {@code null} if none.
     * Defined as a namespace for use as the scope of children resources (the images).
     */
    final NameSpace identifier;

    /**
     * The metadata, or {@code null} if not yet created.
     *
     * @see #getMetadata()
     */
    private Metadata metadata;

    /**
     * Description of images in this GeoTIFF files. This collection is created only when first needed.
     *
     * @see #components()
     */
    private List<GridCoverageResource> components;

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
        if (location != null) {
            final NameFactory f = reader.nameFactory;
            String filename = IOUtilities.filenameWithoutExtension(input.filename);
            if (Numerics.isUnsignedInteger(filename)) filename += ".tiff";
            identifier = f.createNameSpace(f.createLocalName(null, filename), null);
        } else {
            // Location not convertible to URI. The string representation is probably a class name, which is not useful.
            identifier = null;
        }
    }

    /**
     * Opens access to listeners for {@link ImageFileDirectory}.
     */
    final StoreListeners listeners() {
        return listeners;
    }

    /**
     * Returns the parameters used to open this GeoTIFF data store.
     * If non-null, the parameters are described by {@link GeoTiffStoreProvider#getOpenParameters()} and contains at
     * least a parameter named {@value org.apache.sis.storage.DataStoreProvider#LOCATION} with a {@link URI} value.
     * This method may return {@code null} if the storage input can not be described by a URI
     * (for example a GeoTIFF file reading directly from a {@link java.nio.channels.ReadableByteChannel}).
     *
     * @return parameters used for opening this data store, or {@code null} if not available.
     */
    @Override
    public ParameterValueGroup getOpenParameters() {
        return URIDataStore.parameters(provider, location);
    }

    /**
     * Returns an identifier constructed from the name of the TIFF file.
     * An identifier is available only if the storage input specified at construction time was something convertible to
     * {@link java.net.URI}, for example an {@link java.net.URL}, {@link java.io.File} or {@link java.nio.file.Path}.
     *
     * @return the identifier derived from the filename.
     * @throws DataStoreException if an error occurred while fetching the identifier.
     *
     * @since 1.0
     */
    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return (identifier != null) ? Optional.of(identifier.name()) : Optional.empty();
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
                builder.addFormatName(Constants.GEOTIFF);
                listeners.warning(e);
            }
            builder.addEncoding(encoding, MetadataBuilder.Scope.METADATA);
            builder.addResourceScope(ScopeCode.COVERAGE, null);
            final Locale locale = getLocale();
            int n = 0;
            try {
                ImageFileDirectory dir;
                while ((dir = reader.getImageFileDirectory(n++)) != null) {
                    dir.completeMetadata(builder, locale);
                }
            } catch (IOException e) {
                throw errorIO(e);
            } catch (FactoryException | ArithmeticException e) {
                throw new DataStoreContentException(getLocale(), Constants.GEOTIFF, reader.input.filename, null).initCause(e);
            }
            /*
             * Add the filename as an identifier only if the input was something convertible to URI (URL, File or Path),
             * otherwise reader.input.filename may not be useful; it may be just the InputStream classname. If the TIFF
             * file did not specified any ImageDescription tag, then we will had the filename as a title instead than an
             * identifier because the title is mandatory in ISO 19115 metadata.
             */
            getIdentifier().ifPresent((id) -> builder.addTitleOrIdentifier(id.toString(), MetadataBuilder.Scope.ALL));
            builder.setISOStandards(true);
            metadata = builder.build(true);
        }
        return metadata;
    }

    /**
     * Returns the exception to throw when an I/O error occurred.
     */
    private DataStoreException errorIO(final IOException e) {
        return new DataStoreException(errors().getString(Errors.Keys.CanNotRead_1, reader.input.filename), e);
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
     * Returns descriptions of all images in this GeoTIFF file.
     * Images are not immediately loaded.
     *
     * <p>If an error occurs during iteration in the returned collection,
     * an unchecked {@link BackingStoreException} will be thrown with a {@link DataStoreException} as its cause.</p>
     *
     * @return descriptions of all images in this GeoTIFF file.
     * @throws DataStoreException if an error occurred while fetching the image descriptions.
     *
     * @since 1.0
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<GridCoverageResource> components() throws DataStoreException {
        if (components == null) {
            components = new Components();
        }
        return components;
    }

    /**
     * The components returned by {@link #components}. Defined as a named class instead than an anonymous
     * class for more readable stack trace. This is especially useful since {@link BackingStoreException}
     * may happen in any method.
     */
    private final class Components extends ListOfUnknownSize<GridCoverageResource> {
        /** The collection size, cached when first computed. */
        private int size = -1;

        /** Returns the size or -1 if not yet known. */
        @Override protected int sizeIfKnown() {
            return size;
        }

        /** Returns the size, computing and caching it if needed. */
        @Override public int size() {
            if (size < 0) {
                size = super.size();
            }
            return size;
        }

        /** Returns whether the given index is valid. */
        @Override protected boolean exists(final int index) {
            return (index >= 0) && getImageFileDirectory(index) != null;
        }

        /** Returns element at the given index or throw {@link IndexOutOfBoundsException}. */
        @Override public GridCoverageResource get(final int index) {
            if (index >= 0) {
                GridCoverageResource image = getImageFileDirectory(index);
                if (image != null) return image;
            }
            throw new IndexOutOfBoundsException(errors().getString(Errors.Keys.IndexOutOfBounds_1, index));
        }

        /** Returns element at the given index or returns {@code null} if the index is invalid. */
        private GridCoverageResource getImageFileDirectory(final int index) {
            try {
                return reader().getImageFileDirectory(index);
            } catch (IOException e) {
                throw new BackingStoreException(errorIO(e));
            } catch (DataStoreException e) {
                throw new BackingStoreException(e);
            }
        }
    }

    /**
     * Returns the image at the given index. Images numbering starts at 1.
     *
     * @param  sequence  string representation of the image index, starting at 1.
     * @return image at the given index.
     * @throws DataStoreException if the requested image can not be obtained.
     */
    @Override
    public GridCoverageResource findResource(final String sequence) throws DataStoreException {
        Exception cause;
        int index;
        try {
            index = Integer.parseInt(sequence);
            cause = null;
        } catch (NumberFormatException e) {
            index = 0;
            cause = e;
        }
        if (index > 0) try {
            ImageFileDirectory image = reader().getImageFileDirectory(index - 1);
            if (image != null) return image;
        } catch (IOException e) {
            throw errorIO(e);
        }
        throw new IllegalNameException(StoreUtilities.resourceNotFound(this, sequence), cause);
    }

    /**
     * Registers a listener to notify when the specified kind of event occurs in this data store.
     * The current implementation of this data store can emit only {@link WarningEvent}s;
     * any listener specified for another kind of events will be ignored.
     */
    @Override
    public <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {
        // If an argument is null, we let the parent class throws (indirectly) NullArgumentException.
        if (listener == null || eventType == null || eventType.isAssignableFrom(WarningEvent.class)) {
            super.addListener(eventType, listener);
        }
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
