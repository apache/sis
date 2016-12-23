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
package org.apache.sis.internal.xml;

import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import javax.xml.stream.Location;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import org.apache.sis.xml.XML;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.internal.storage.FeatureStore;
import org.apache.sis.internal.util.AbstractMap;
import org.apache.sis.storage.UnsupportedStorageException;
import org.apache.sis.util.logging.WarningListener;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Classes;


/**
 * Base class of XML data stores based on the STAX framework.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public abstract class StaxDataStore extends FeatureStore {
    /**
     * The store name (typically filename) for formatting error messages.
     */
    protected final String name;

    /**
     * Configuration information for JAXB (un)marshaller (actually the SIS wrappers) or for the STAX factories.
     * This object is a read-only map which may contain the following entries:
     *
     * <ul>
     *   <li>{@link XML#LOCALE}   — the locale to use for locale-sensitive data (<strong>not</strong> for logging or warning messages).</li>
     *   <li>{@link XML#TIMEZONE} — the timezone to use when parsing or formatting dates and times without explicit timezone.</li>
     * </ul>
     *
     * In addition, the {@link Config} class also implements various listener interfaces to be given to
     * JAXB (un)marshallers (actually the SIS wrappers) and STAX factories configuration.
     *
     * @see OptionKey#LOCALE
     * @see OptionKey#TIMEZONE
     */
    final Config configuration;

    /**
     * The character encoding, or {@code null} if unspecified.
     * This is often (but not always) ignored at reading time, but taken in account at writing time.
     */
    final Charset encoding;

    /**
     * The storage object has given by the user. Can be {@link java.nio.file.Path}, {@link java.net.URL},
     * {@link java.io.InputStream}, {@link java.io.Reader}, {@link javax.xml.stream.XMLStreamReader},
     * {@link org.w3c.dom.Node} or some other types including the writer variants of above list.
     *
     * <p>A {@code null} value means that this datastore has been {@linkplain #close() closed}.</p>
     *
     * @see StorageConnector#getStorage()
     */
    private Object storage;

    /**
     * The underlying stream to close when this {@code StaxDataStore} is closed, or {@code null} if none.
     * This is often the same reference than {@link #storage} if the later is closeable, but not always.
     * For example if {@code storage} is a {@link java.nio.file.Path}, then {@code stream} will be some
     * stream or channel opened for that path.
     *
     * @see #close()
     */
    private Closeable stream;

    /**
     * The function in charge of producing a {@link XMLStreamReader} from the {@link #storage} or {@link #stream}.
     * This field is {@code null} if the XML file is write only.
     */
    private final InputType storageToReader;

    /**
     * The STAX readers factory, created when first needed.
     *
     * @see #inputFactory()
     */
    private XMLInputFactory inputFactory;

    /**
     * The STAX writers factory, created when first needed.
     *
     * @see #outputFactory()
     */
    private XMLOutputFactory outputFactory;

    /**
     * Creates a new data store.
     *
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @throws DataStoreException if the input or output type is not recognized.
     */
    protected StaxDataStore(final StorageConnector connector) throws DataStoreException {
        super(connector);
        name          = connector.getStorageName();
        storage       = connector.getStorage();
        encoding      = connector.getOption(OptionKey.ENCODING);
        configuration = new Config(connector);
        InputType storageToReader = InputType.forType(storage.getClass());
        if (storageToReader == null) {
            stream = connector.getStorageAs(InputStream.class);
            if (stream != null) {
                storageToReader = InputType.STREAM;
            }
        } else if (storage instanceof Closeable) {
            stream = (Closeable) storage;
        }
        this.storageToReader = storageToReader;
        connector.closeAllExcept(stream);
    }

    /**
     * Holds information that can be used for (un)marshallers configuration, and opportunistically
     * implement various listeners used by JAXB (actually the SIS wrappers) or STAX.
     */
    private final class Config extends AbstractMap<String,Object> implements XMLReporter, WarningListener<Object> {
        /**
         * The locale to use for locale-sensitive data (<strong>not</strong> for logging or warning messages),
         * or {@code null} if unspecified.
         *
         * @see OptionKey#LOCALE
         */
        private final Locale locale;

        /**
         * The timezone to use when parsing or formatting dates and times without explicit timezone,
         * or {@code null} if unspecified.
         *
         * @see OptionKey#TIMEZONE
         */
        private final TimeZone timezone;

        /**
         * Fetches configuration information from the given object.
         */
        Config(final StorageConnector connector) {
            locale   = connector.getOption(OptionKey.LOCALE);
            timezone = connector.getOption(OptionKey.TIMEZONE);
        }

        /**
         * Returns configuration associated to the given key, or {@code null} if none.
         *
         * @param  key  one of {@link XML#LOCALE}, {@link XML#TIMEZONE} or {@link XML#WARNING_LISTENER}.
         * @return the configuration for the given key, or {@code null} if none or if the given key is invalid.
         */
        @Override
        @SuppressWarnings("ReturnOfCollectionOrArrayField")
        public Object get(final Object key) {
            if (key instanceof String) {
                switch ((String) key) {
                    case XML.LOCALE:           return locale;
                    case XML.TIMEZONE:         return timezone;
                    case XML.WARNING_LISTENER: return this;
                }
            }
            return null;
        }

        /**
         * Returns an iterator over all entries in this map.
         */
        @Override
        protected EntryIterator<String, Object> entryIterator() {
            return new KeyIterator(XML.LOCALE, XML.TIMEZONE, XML.WARNING_LISTENER);
        }

        /**
         * Forwards STAX warnings to {@link DataStore} listeners.
         * This method is invoked by {@link XMLStreamReader} when needed.
         *
         * @param message    the message to put in a logging record.
         * @param errorType  ignored.
         * @param info       ignored.
         * @param location   ignored.
         */
        @Override
        public void report(String message, String errorType, Object info, Location location) {
            final LogRecord record = new LogRecord(Level.WARNING, message);
            record.setSourceClassName(getClass().getCanonicalName());
            listeners.warning(record);
        }

        /**
         * Reports a warning represented by the given log record.
         *
         * @param source   ignored (typically a JAXB object being unmarshalled). Can be {@code null}.
         * @param warning  the warning as a log record.
         */
        @Override
        public void warningOccured(final Object source, final LogRecord warning) {
            listeners.warning(warning);
        }

        /**
         * Returns the type of objects that emit warnings of interest for this listener.
         * Fixed to {@code Object.class} as required by {@link org.apache.sis.xml.XML#WARNING_LISTENER} documentation.
         */
        @Override
        public final Class<Object> getSourceClass() {
            return Object.class;
        }
    }

    /**
     * Returns the short name (abbreviation) of the format being read or written.
     * This is used for error messages.
     *
     * @return short name of format being read or written.
     */
    public abstract String getFormatName();

    /**
     * Returns the factory for STAX readers.
     * This method is invoked by {@link InputType#create(StaxDataStore, Object)}.
     */
    final synchronized XMLInputFactory inputFactory() {
        if (inputFactory == null) {
            inputFactory = XMLInputFactory.newInstance();
            inputFactory.setXMLReporter(configuration);
        }
        return inputFactory;
    }

    /**
     * Returns the factory for STAX writers.
     * This method is invoked by {@link OutputType#create(StaxDataStore, Object)}.
     */
    final synchronized XMLOutputFactory outputFactory() {
        if (outputFactory == null) {
            outputFactory = XMLOutputFactory.newInstance();
            outputFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
        }
        return outputFactory;
    }

    protected final XMLStreamReader createReader() throws DataStoreException, XMLStreamException {
        if (storageToReader == null) {
            throw new UnsupportedStorageException(errors().getString(Errors.Keys.IllegalInputTypeForReader_2,
                    getFormatName(), Classes.getClass(storage)));
        }
        // TODO: mark the stream
        return storageToReader.create(this, stream != null ? stream : storage);
    }

    /**
     * Returns the error resources in the current locale.
     */
    private Errors errors() {
        return Errors.getResources(getLocale());
    }

    /**
     * Closes the input or output stream and releases any resources used by this XML data store.
     * This data store can not be used anymore after this method has been invoked.
     *
     * @throws DataStoreException if an error occurred while closing the input or output stream.
     */
    @Override
    public synchronized void close() throws DataStoreException {
        final Closeable s = stream;
        stream        = null;
        storage       = null;
        inputFactory  = null;
        outputFactory = null;
        if (s != null) try {
            s.close();
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
    }
}
