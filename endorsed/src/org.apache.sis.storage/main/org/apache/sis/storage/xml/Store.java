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
package org.apache.sis.storage.xml;

import java.util.Map;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.io.Closeable;
import java.io.Reader;
import java.io.InputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import javax.xml.transform.stream.StreamSource;
import jakarta.xml.bind.JAXBException;
import org.opengis.metadata.Metadata;
import org.opengis.util.FactoryException;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.xml.XML;
import org.apache.sis.xml.internal.shared.URISource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.UnsupportedStorageException;
import org.apache.sis.storage.base.URIDataStore;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.event.WarningEvent;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.system.Loggers;
import org.apache.sis.referencing.internal.shared.DefinitionVerifier;
import org.apache.sis.setup.OptionKey;


/**
 * A data store which creates data objects from a XML file.
 * This {@code DataStore} implementation is basically a facade for the {@link XML#unmarshal(Source, Map)} method.
 * The current implementation recognizes the following objects:
 *
 * <ul>
 *   <li>{@link Metadata}, typically built from the {@code <mdb:MD_Metadata>} XML element.</li>
 *   <li>{@link ReferenceSystem}, accessible by {@link Metadata#getReferenceSystemInfo()}.</li>
 * </ul>
 *
 * The above list may be extended in any future SIS version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class Store extends URIDataStore implements Filter {
    /**
     * The input stream or reader, set by the constructor and cleared when no longer needed.
     */
    private volatile StreamSource source;

    /**
     * The unmarshalled object, initialized only when first needed.
     * May still be {@code null} if the unmarshalling failed.
     */
    private Object object;

    /**
     * The metadata object, determined when first needed.
     */
    private Metadata metadata;

    /**
     * Creates a new XML store from the given file, URL or stream.
     *
     * @param  provider   the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @throws URISyntaxException if an error occurred while normalizing the URI.
     * @throws DataStoreException if an error occurred while opening the stream.
     */
    public Store(final StoreProvider provider, final StorageConnector connector)
            throws URISyntaxException, DataStoreException
    {
        super(provider, connector);
        final InputStream in = connector.getStorageAs(InputStream.class);
        if (in != null) {
            source = URISource.create(in, location);
        } else {
            final Reader reader = connector.getStorageAs(Reader.class);
            if (reader != null) {
                var s = URISource.create(null, location);
                s.setReader(reader);
                source = s;
            }
        }
        final Closeable c = input(source);
        connector.closeAllExcept(c);
        if (c == null) {
            throw new UnsupportedStorageException(super.getLocale(), StoreProvider.NAME,
                    connector.getStorage(), connector.getOption(OptionKey.OPEN_OPTIONS));
        }
        listeners.useReadOnlyEvents();
    }

    /**
     * Returns the input stream or reader set in the given source, or {@code null} if none.
     */
    private static Closeable input(final StreamSource source) {
        Closeable in = null;
        if (source != null) {
            in = source.getInputStream();
            if (in == null) {
                in = source.getReader();
            }
        }
        return in;
    }

    /**
     * Returns the properties to give to the (un)marshaller.
     */
    private Map<String,?> properties() {
        if (listeners.hasListeners(WarningEvent.class)) {
            return Map.of(XML.WARNING_FILTER, this);
        }
        return null;
    }

    /**
     * Intercepts warnings produced during the (un)marshalling process and redirect them to the listeners.
     * This method is public as an implementation convenience for {@link #properties()} method;
     * it should not be invoked directly.
     *
     * @param  warning  the warning that occurred during (un)marshalling.
     * @return always {@code false} since logging will be handled by {@code listeners}.
     */
    @Override
    public boolean isLoggable(final LogRecord warning) {
        warning.setLoggerName(Loggers.XML);
        listeners.warning(warning);
        return false;
    }

    /**
     * Unmarshal the object, if not already done. Note that {@link #object} may still be null
     * if an exception has been thrown at this invocation time or in previous invocation.
     *
     * @throws DataStoreException if an error occurred during the unmarshalling process.
     */
    private void unmarshal() throws DataStoreException {
        final StreamSource s = source;
        final Closeable in = input(s);
        if (in != null) try {
            try {
                object = XML.unmarshal(s, properties());
            } finally {
                source = null;
                in.close();
            }
        } catch (JAXBException | IOException e) {
            throw new DataStoreException(Errors.format(Errors.Keys.CanNotRead_1, getDisplayName()), e);
        }
        if (object instanceof CoordinateReferenceSystem) try {
            final DefinitionVerifier v = DefinitionVerifier.withAuthority(
                    (CoordinateReferenceSystem) object, null, false, getLocale());
            if (v != null) {
                log(v.warning(false));
            }
        } catch (FactoryException e) {
            listeners.warning(e);
        }
    }

    /**
     * Reports a warning, if non-null.
     */
    private void log(final LogRecord record) {
        if (record != null) {
            record.setSourceClassName(Store.class.getName());
            record.setSourceMethodName("getMetadata");          // Public facade for the parse() method.
            record.setLoggerName(Loggers.XML);
            listeners.warning(record);
        }
    }

    /**
     * Returns the metadata associated to the unmarshalled object, or {@code null} if none.
     * The current implementation performs the following choice:
     *
     * <ul>
     *   <li>If the unmarshalled object implements the {@link Metadata} interface, then it is returned directly.</li>
     *   <li>Otherwise if the unmarshalled object implements {@link ReferenceSystem}, then it is wrapped in the
     *       <q>reference system info</q> property of a new {@link DefaultMetadata} instance.</li>
     * </ul>
     *
     * Other cases may be added in any future SIS version.
     *
     * @return the metadata associated to the unmarshalled object, or {@code null} if none.
     * @throws DataStoreException if an error occurred during the unmarshalling process.
     */
    @Override
    public synchronized Metadata getMetadata() throws DataStoreException {
        if (metadata == null) {
            unmarshal();
            if (object instanceof Metadata) {
                metadata = (Metadata) object;
            } else if (object instanceof ReferenceSystem) {
                final MetadataBuilder builder = new MetadataBuilder();
                builder.addReferenceSystem((ReferenceSystem) object);
                builder.addTitle(getDisplayName());
                mergeAuxiliaryMetadata(Store.class, builder);
                metadata = builder.buildAndFreeze();
            }
        }
        return metadata;
    }

    /**
     * Closes this data store and releases any underlying resources.
     * This method can be invoked asynchronously for interrupting a long reading process.
     *
     * @throws DataStoreException if an error occurred while closing this data store.
     */
    @Override
    public void close() throws DataStoreException {
        try {
            listeners.close();                      // Should never fail.
            final Closeable in = input(source);
            if (in != null) in.close();
        } catch (IOException e) {
            throw new DataStoreException(e);
        } finally {
            synchronized (this) {
                object = null;
                source = null;
            }
        }
    }
}
