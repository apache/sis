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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.charset.Charset;
import javax.xml.stream.Location;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;
import org.apache.sis.xml.XML;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.internal.storage.FeatureStore;
import org.apache.sis.internal.storage.Markable;
import org.apache.sis.internal.util.AbstractMap;
import org.apache.sis.storage.DataStoreClosedException;
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
     * The locale to use for locale-sensitive data (<strong>not</strong> for logging or warning messages),
     * or {@code null} if unspecified.
     *
     * @see OptionKey#LOCALE
     */
    protected final Locale locale;

    /**
     * The timezone to use when parsing or formatting dates and times without explicit timezone,
     * or {@code null} if unspecified.
     *
     * @see OptionKey#TIMEZONE
     */
    protected final TimeZone timezone;

    /**
     * The character encoding of the file content, or {@code null} if unspecified.
     * This is often (but not always) ignored at reading time, but taken in account at writing time.
     */
    protected final Charset encoding;

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
     * The storage object given by the user. May be {@link Path}, {@link java.net.URL}, {@link InputStream},
     * {@link java.io.OutputStream}, {@link java.io.Reader}, {@link java.io.Writer}, {@link XMLStreamReader},
     * {@link XMLStreamWriter}, {@link org.w3c.dom.Node} or some other types that the STAX framework can handle.
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
    private AutoCloseable stream;

    /**
     * Position of the first byte to read in the {@linkplain #stream}, or a negative value if unknown.
     * If the position is positive, then the stream should have been {@linkplain Markable#mark() marked}
     * at that position by the constructor.
     */
    private final long streamPosition;

    /**
     * The function in charge of producing a {@link XMLStreamReader} from the {@link #storage} or {@link #stream}.
     * This field is {@code null} if the XML file is write-only or if {@link #storage} is a {@link Path}.
     */
    private final InputType storageToReader;

    /**
     * The function in charge of producing a {@link XMLStreamWriter} for the {@link #storage} or {@link #stream}.
     * This field is {@code null} if the XML file is read-only or if {@link #storage} is a {@link Path}.
     */
    private final OutputType storageToWriter;

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
     * Whether the {@linkplain #stream} is currently in use by a {@link StaxStreamReader}.
     * Value can be one of {@link #READY}, {@link #IN_USE} or {@link #FINISHED} constants.
     */
    private byte state;

    /**
     * Possible states for the {@link #state} field.
     */
    private static final byte READY = 0, IN_USE = 1, FINISHED = 2;

    /**
     * Creates a new data store.
     * The {@code provider} is mandatory if the data store will use JAXB, otherwise it is optional.
     *
     * @param  provider   the provider of this data store, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @throws DataStoreException if the input or output type is not recognized.
     */
    protected StaxDataStore(final StaxDataStoreProvider provider, final StorageConnector connector) throws DataStoreException {
        super(provider, connector);
        name            = connector.getStorageName();
        storage         = connector.getStorage();
        locale          = connector.getOption(OptionKey.LOCALE);
        timezone        = connector.getOption(OptionKey.TIMEZONE);
        encoding        = connector.getOption(OptionKey.ENCODING);
        configuration   = new Config();
        storageToWriter = OutputType.forType(storage.getClass());
        storageToReader = InputType.forType(storage.getClass());
        if (storageToReader == null) {
            /*
             * We enter in this block if the storage type is not an input stream, DOM node, etc.
             * It may be a file name, a URL, etc. Those types are not handled by InputType in
             * order to give us a chance to use the existing InputStream instead than closing
             * the connection and reopening a new one on the same URL. Another reason is that
             * we will need to remember the stream created from the Path in order to close it.
             *
             * We ask for an InputStream, but StorageConnector implementation actually tries to create
             * a ChannelDataInput if possible, which will allow us to create a ChannelDataOutput later
             * if needed (and if the underlying channel is writable).
             */
            stream = connector.getStorageAs(InputStream.class);
        }
        if (stream == null && storage instanceof AutoCloseable) {
            stream = (AutoCloseable) storage;
        }
        connector.closeAllExcept(stream);
        /*
         * If possible, remember the position where data begin in the stream in order to allow reading
         * the same data many time. We do not use the InputStream.mark(int) and reset() methods because
         * we do not know which "read ahead limit" to use, and we do not know if the XMLStreamReader or
         * other code will set their own mark (which could cause our reset() call to move to the wrong
         * position).
         */
        if (stream instanceof Markable) try {
            final Markable m = (Markable) stream;
            streamPosition = m.getStreamPosition();
            m.mark();
        } catch (IOException e) {
            throw new DataStoreException(e);
        } else {
            streamPosition = -1;
        }
    }

    /**
     * Holds information that can be used for (un)marshallers configuration, and opportunistically
     * implement various listeners used by JAXB (actually the SIS wrappers) or STAX.
     */
    private final class Config extends AbstractMap<String,Object> implements XMLReporter, WarningListener<Object> {
        /**
         * Fetches configuration information from the given object.
         */
        Config() {
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
     * Returns the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     */
    final StaxDataStoreProvider getProvider() {
        return (StaxDataStoreProvider) provider;
    }

    /**
     * Returns the short name (abbreviation) of the format being read or written.
     * This is used for error messages.
     *
     * @return short name of format being read or written.
     */
    public abstract String getFormatName();

    /**
     * Returns the factory for STAX readers. The same instance is returned for all {@code StaxDataStore} lifetime.
     * Warnings emitted by readers created by this factory will be forwarded to the {@link #listeners}.
     *
     * <p>This method is indirectly invoked by {@link #createReader(StaxStreamReader)},
     * through a call to {@link InputType#create(StaxDataStore, Object)}.</p>
     */
    final XMLInputFactory inputFactory() {
        assert Thread.holdsLock(this);
        if (inputFactory == null) {
            inputFactory = XMLInputFactory.newInstance();
            inputFactory.setXMLReporter(configuration);
        }
        return inputFactory;
    }

    /**
     * Returns the factory for STAX writers. The same instance is returned for all {@code StaxDataStore} lifetime.
     *
     * <p>This method is indirectly invoked by {@link #createWriter(StaxStreamWriter)},
     * through a call to {@link OutputType#create(StaxDataStore, Object)}.</p>
     */
    final XMLOutputFactory outputFactory() {
        assert Thread.holdsLock(this);
        if (outputFactory == null) {
            outputFactory = XMLOutputFactory.newInstance();
        }
        return outputFactory;
    }

    /**
     * Creates a new XML stream reader for reading the document from its position at {@code StaxDataStore}
     * creation time. If another {@code XMLStreamReader} has already been created before this method call,
     * whether this method will succeed in creating a new reader depends on the storage type (e.g. file or
     * input stream) or on whether the previous reader has been closed.
     *
     * @param  target  the reader which will store the {@code XMLStreamReader} reference.
     * @return a new reader for reading the XML data.
     * @throws DataStoreException if the input type is not recognized or the data store is closed.
     * @throws XMLStreamException if an error occurred while opening the XML file.
     * @throws IOException if an error occurred while preparing the input stream.
     */
    @SuppressWarnings("fallthrough")
    final synchronized XMLStreamReader createReader(final StaxStreamReader target)
            throws DataStoreException, XMLStreamException, IOException
    {
        Object input = storage;
        if (input == null) {
            throw new DataStoreClosedException(errors().getString(Errors.Keys.ClosedReader_1, getFormatName()));
        }
        /*
         * If the storage given by the user was not one of InputStream, Reader or other type recognized
         * by InputType, then maybe that storage was a Path, File or URL, in which case the constructor
         * should have opened an InputStream for it. If not, then this was an unsupported storage type.
         */
        InputType type = storageToReader;
        if (type == null) {
            type = InputType.STREAM;
            if ((input = stream) == null) {
                throw new UnsupportedStorageException(errors().getString(Errors.Keys.IllegalInputTypeForReader_2,
                        getFormatName(), Classes.getClass(storage)));
            }
        }
        /*
         * If the stream has already been used by a previous read operation, then we need to rewind
         * it to the start position determined at construction time. It the stream does not support
         * mark, then we can not re-read the data.
         */
reset:  switch (state) {
            default: {
                throw new AssertionError(state);
            }
            case FINISHED: {
                if (streamPosition >= 0) {
                    final Markable m = (Markable) input;
                    long p;
                    while ((p = m.getStreamPosition()) >= streamPosition) {
                        if (p == streamPosition) {
                            break reset;
                        }
                        m.reset();
                    }
                }
                // Failed to reset the stream - fallthrough.
            }
            case IN_USE: {
                // TODO: create a new stream here if we can.
                throw new DataStoreException("Can not read twice.");
            }
            case READY: break;                      // Stream already at the data start; nothing to do.
        }
        final XMLStreamReader reader = type.create(this, input);
        target.stream = stream;
        state = IN_USE;
        return reader;
    }

    /**
     * Creates a new XML stream writer for writing the XML document.
     * If another {@code XMLStreamWriter} has already been created before this method call,
     * whether this method will succeed in creating a new writer depends on the storage type
     * (e.g. file or output stream).
     *
     * @param  target  the writer which will store the {@code XMLStreamWriter} reference.
     * @return a new writer for writing the XML data.
     * @throws DataStoreException if the output type is not recognized or the data store is closed.
     * @throws XMLStreamException if an error occurred while opening the XML file.
     * @throws IOException if an error occurred while preparing the output stream.
     */
    final synchronized XMLStreamWriter createWriter(final StaxStreamWriter target)
            throws DataStoreException, XMLStreamException, IOException
    {
        Object output = storage;
        if (output == null) {
            throw new DataStoreClosedException(errors().getString(Errors.Keys.ClosedWriter_1, getFormatName()));
        }
        /*
         * If the storage given by the user was not one of OutputStream, Writer or other type recognized
         * by OutputType, then maybe that storage was a Path, File or URL, in which case the constructor
         * should have opened an InputStream for it. If not, then this was an unsupported storage type.
         */
        OutputType type = storageToWriter;
        if (type == null) {
            // TODO
            throw new UnsupportedStorageException(errors().getString(Errors.Keys.IllegalOutputTypeForWriter_2,
                    getFormatName(), Classes.getClass(storage)));
        }
        final XMLStreamWriter writer = type.create(this, output);
        target.stream = stream;
        return writer;
    }

    /**
     * Invoked when {@link StaxStreamReader} finished to read XML document from the given stream.
     * This method returns {@code true} if the caller should invoke {@link AutoCloseable#close()},
     * or {@code false} if this {@code StaxDataStore} may reuse that stream.
     *
     * @param  finished  the stream that has been used for reading XML document.
     * @return whether the caller should invoke {@code finished.close()}.
     */
    final synchronized boolean canClose(final AutoCloseable finished) {
        if (finished != null && stream == finished) {
            state = FINISHED;
            return false;
        }
        return true;
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
        final AutoCloseable s = stream;
        stream        = null;
        storage       = null;
        inputFactory  = null;
        outputFactory = null;
        if (s != null) try {
            s.close();
        } catch (Exception e) {
            throw new DataStoreException(e);
        }
    }
}
