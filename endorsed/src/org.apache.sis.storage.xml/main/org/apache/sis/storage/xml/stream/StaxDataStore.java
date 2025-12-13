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
package org.apache.sis.storage.xml.stream;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Filter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.charset.Charset;
import javax.xml.XMLConstants;
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
import org.apache.sis.storage.ConcurrentReadException;
import org.apache.sis.storage.ConcurrentWriteException;
import org.apache.sis.storage.DataStoreClosedException;
import org.apache.sis.storage.ForwardOnlyStorageException;
import org.apache.sis.storage.UnsupportedStorageException;
import org.apache.sis.storage.base.URIDataStore;
import org.apache.sis.io.InvalidSeekException;
import org.apache.sis.io.stream.ChannelFactory;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.io.stream.Markable;
import org.apache.sis.util.internal.shared.AbstractMap;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.io.wkt.WKTFormat;


/**
 * Base class of XML data stores based on the StAX framework.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class StaxDataStore extends URIDataStore {
    /**
     * Configuration information for JAXB (un)marshaller (actually the SIS wrappers) or for the StAX factories.
     * This object is a read-only map which may contain the following entries:
     *
     * <ul>
     *   <li>{@link XML#LOCALE}   — the locale to use for locale-sensitive data (<strong>not</strong> for logging or warning messages).</li>
     *   <li>{@link XML#TIMEZONE} — the timezone to use when parsing or formatting dates and times without explicit timezone.</li>
     * </ul>
     *
     * In addition, the {@link Config} class also implements various listener interfaces to be given to
     * JAXB (un)marshallers (actually the SIS wrappers) and StAX factories configuration.
     *
     * @see OptionKey#LOCALE
     * @see OptionKey#TIMEZONE
     */
    final Config configuration;

    /**
     * The storage object given by the user. May be {@link Path}, {@link java.net.URL}, {@link InputStream},
     * {@link java.io.OutputStream}, {@link java.io.Reader}, {@link java.io.Writer}, {@link XMLStreamReader},
     * {@link XMLStreamWriter}, {@link org.w3c.dom.Node} or some other types that the StAX framework can handle.
     *
     * <p>A {@code null} value means that this datastore has been {@linkplain #close() closed}.</p>
     *
     * @see StorageConnector#getStorage()
     */
    private Object storage;

    /**
     * The underlying stream to close when this {@code StaxDataStore} is closed, or {@code null} if none.
     * This is often the same reference as {@link #storage} if the latter is closeable, but not always.
     * For example if {@code storage} is a {@link java.nio.file.Path}, then {@code stream} will be some
     * stream or channel opened for that path.
     *
     * <p>We keep this reference as long as possible in order to use {@link #mark()} and {@link #reset()}
     * instead of creating new streams for re-reading the data. If we cannot reset the stream but can
     * create a new one, then this field will become a reference to the new stream. This change should be
     * done only in last resort, when there is no way to reuse the existing stream. This is because the
     * streams created by {@link ChannelFactory#inputStream(String, StoreListeners)} are not of the same
     * kind than the streams created by {@link StorageConnector}.</p>
     *
     * @see #close()
     */
    private volatile Closeable stream;

    /**
     * Position of the first byte to read in the {@linkplain #stream}, or a negative value if unknown.
     * If the position is positive, then the stream should have been {@linkplain Markable#mark() marked}
     * at that position by the constructor.
     *
     * @see #mark()
     * @see #reset()
     */
    private long streamPosition;

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
     * The StAX readers factory, created when first needed.
     *
     * @see #inputFactory()
     */
    private XMLInputFactory inputFactory;

    /**
     * The StAX writers factory, created when first needed.
     *
     * @see #outputFactory()
     */
    private XMLOutputFactory outputFactory;

    /**
     * Object to use for creating new input streams if we need to read the same data more than once.
     * This field is {@code null} if we cannot re-open new input streams.
     */
    private final ChannelFactory channelFactory;

    /**
     * The number of spaces to use in indentations, or -1 if the XML output should not be formatted.
     * This is ignored at reading time.
     */
    private final byte indentation;

    /**
     * Whether the {@linkplain #stream} is currently in use by a {@link StaxStreamIO}. Value can be
     * one of {@link #START}, {@link #READING}, {@link #WRITING} or {@link #FINISHED} constants.
     */
    private byte state;

    /**
     * Possible states for the {@link #state} field.
     */
    private static final byte START = 0, READING = 1, WRITING = 2, FINISHED = 3;

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
        final Integer indent;
        storage         = connector.getStorage();
        indent          = connector.getOption(OptionKey.INDENTATION);
        indentation     = (indent == null) ? Constants.DEFAULT_INDENTATION
                                           : (byte) Math.max(WKTFormat.SINGLE_LINE, Math.min(120, indent));
        configuration   = new Config();
        storageToWriter = OutputType.forType(storage.getClass());
        storageToReader = InputType.forType(storage.getClass());
        if (storageToReader == null) {
            /*
             * We enter in this block if the storage type is not an input stream, DOM node, etc.
             * It may be a file name, a URL, etc. Those types are not handled by InputType in
             * order to give us a chance to use the existing InputStream instead of closing
             * the connection and reopening a new one on the same URL. Another reason is that
             * we will need to remember the stream created from the Path in order to close it.
             *
             * We ask for an InputStream, but StorageConnector implementation actually tries to create
             * a ChannelDataInput if possible, which will allow us to create a ChannelDataOutput later
             * if needed (and if the underlying channel is writable).
             */
            stream = connector.getStorageAs(InputStream.class);
        }
        if (stream == null && storage instanceof Closeable) {
            stream = (Closeable) storage;
        }
        channelFactory = connector.getStorageAs(ChannelFactory.class);  // Must be last before `closeAllExcept(…)`.
        connector.closeAllExcept(stream);
        /*
         * If possible, remember the position where data begin in the stream in order to allow reading
         * the same data many time. We do not use the InputStream.mark(int) and reset() methods because
         * we do not know which "read ahead limit" to use, and we do not know if the XMLStreamReader or
         * other code will set their own mark (which could cause our reset() call to move to the wrong
         * position).
         */
        try {
            mark();
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
    }

    /**
     * Marks the current stream position. This method shall be invoked either at construction time,
     * or after a new stream has been created.
     */
    private void mark() throws IOException {
        streamPosition = -1;
        if (stream instanceof Markable) {
            final Markable m = (Markable) stream;
            streamPosition = m.getStreamPosition();
            m.mark();
        }
    }

    /**
     * Resets the stream position to the mark created at construction time,
     * then marks again the stream for allowing future resets.
     *
     * @return {@code true} of success, or {@code false} if the stream cannot be reset.
     * @throws IOException if an error occurred while resetting the stream.
     */
    private boolean reset() throws IOException {
        if (streamPosition >= 0) try {
            final Markable m = (Markable) stream;
            m.reset(streamPosition);
            m.mark();
            state = START;
            return true;
        } catch (InvalidSeekException e) {
            listeners.warning(e);
        }
        return false;
    }

    /**
     * Holds information that can be used for (un)marshallers configuration, and opportunistically
     * implement various listeners used by JAXB (actually the SIS wrappers) or StAX.
     */
    private final class Config extends AbstractMap<String,Object> implements XMLReporter, Filter {
        /**
         * Fetches configuration information from the given object.
         */
        Config() {
        }

        /**
         * Returns configuration associated to the given key, or {@code null} if none.
         *
         * @param  key  one of {@link XML#LOCALE}, {@link XML#TIMEZONE} or {@link XML#WARNING_FILTER}.
         * @return the configuration for the given key, or {@code null} if none or if the given key is invalid.
         */
        @Override
        @SuppressWarnings("ReturnOfCollectionOrArrayField")
        public Object get(final Object key) {
            if (key instanceof String) {
                switch ((String) key) {
                    case XML.LOCALE:         return getDataLocale();
                    case XML.TIMEZONE:       return timezone;
                    case XML.WARNING_FILTER: return this;
                }
            }
            return null;
        }

        /**
         * Returns an iterator over all entries in this map.
         */
        @Override
        protected EntryIterator<String, Object> entryIterator() {
            return new KeyIterator(XML.LOCALE, XML.TIMEZONE, XML.WARNING_FILTER);
        }

        /**
         * Forwards StAX warnings to {@link DataStore} listeners.
         * This method is invoked by {@link XMLStreamReader} when needed.
         *
         * @param message    the message to put in a logging record.
         * @param errorType  ignored.
         * @param info       ignored.
         * @param location   ignored.
         */
        @Override
        public void report(String message, String errorType, Object info, Location location) {
            final var record = new LogRecord(Level.WARNING, message);
            record.setSourceClassName(StaxDataStore.this.getClass().getCanonicalName());
            // record.setLoggerName(…) will be invoked by `listeners` with inferred name.
            listeners.warning(record);
        }

        /**
         * Reports a warning represented by the given log record.
         *
         * @param warning  the warning as a log record.
         */
        @Override
        public boolean isLoggable(final LogRecord warning) {
            warning.setLoggerName(null);        // For allowing `listeners` to use the provider's logger name.
            listeners.warning(warning);
            return false;
        }

        /**
         * Do not format all properties for avoiding a never-ending loop.
         */
        @Override
        public String toString() {
            return Strings.toString(getClass(), "dataLocale", dataLocale, "timezone", timezone);
        }
    }

    /**
     * Returns the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     *
     * @return the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     */
    @Override
    public final StaxDataStoreProvider getProvider() {
        return (StaxDataStoreProvider) provider;
    }

    /**
     * Returns the short name (abbreviation) of the format being read or written.
     * This is used for error messages.
     *
     * @return short name of format being read or written.
     *
     * @see StaxDataStoreProvider#getShortName()
     */
    public final String getFormatName() {
        return provider.getShortName();
    }

    /**
     * Returns the character encoding of the file content, or {@code null} if unspecified.
     * This is often (but not always) ignored at reading time, but taken in account at writing time.
     */
    final Charset getEncoding() {
        return encoding;
    }

    /**
     * Returns the locale to use for locale-sensitive data (<strong>not</strong> for logging or warning messages),
     * or {@code null} if unspecified.
     *
     * @see OptionKey#LOCALE
     */
    final Locale getDataLocale() {
        return dataLocale;
    }

    /**
     * Returns the factory for StAX readers. The same instance is returned for all {@code StaxDataStore} lifetime.
     * Warnings emitted by readers created by this factory will be forwarded to the {@link #listeners}.
     *
     * <p>This method is indirectly invoked by {@link #createReader(StaxStreamReader)},
     * through a call to {@link InputType#create(StaxDataStore, Object)}.</p>
     *
     * <h4>Security</h4>
     * Unless the user has configured the {@code javax.xml.accessExternalDTD} property to something else
     * than {@code "all"}, this class disallows external DTDs referenced by the {@code "file"} protocols.
     * Allowed protocols are <abbr>HTTP</abbr> and <abbr>HTTPS</abbr> (list may be expanded if needed).
     *
     * @see org.apache.sis.xml.internal.shared.InputFactory#FACTORY
     */
    final XMLInputFactory inputFactory() {
        assert Thread.holdsLock(this);
        XMLInputFactory factory = inputFactory;
        if (factory == null) {
            factory = XMLInputFactory.newInstance();
            if (factory.isPropertySupported(XMLConstants.FEATURE_SECURE_PROCESSING)) {
                factory.setProperty(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
            }
            if ("all".equals(factory.getProperty(XMLConstants.ACCESS_EXTERNAL_DTD))) {
                factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "http,https");
            }
            factory.setXMLReporter(configuration);
            inputFactory = factory;     // Set only on success.
        }
        return factory;
    }

    /**
     * Returns the factory for StAX writers. The same instance is returned for all {@code StaxDataStore} lifetime.
     *
     * <p>This method is indirectly invoked by {@link #createWriter(StaxStreamWriter, Object)},
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
     * @throws Exception if another kind of error occurred while closing a previous stream.
     */
    @SuppressWarnings("fallthrough")
    final synchronized XMLStreamReader createReader(final StaxStreamReader target) throws Exception {
        Object inputOrFile = storage;
        if (inputOrFile == null) {
            throw new DataStoreClosedException(getLocale(), getFormatName(), StandardOpenOption.READ);
        }
        Closeable input = stream;
        InputType type = storageToReader;
        /*
         * If the stream has already been used by a previous read operation, then we need to rewind
         * it to the start position determined at construction time. It the stream does not support
         * mark, then we cannot re-read the data unless we know how to create new input streams.
         */
        switch (state) {
            default:       throw new AssertionError(state);
            case WRITING:  throw new ConcurrentWriteException(getLocale(), getDisplayName());
            case START:    break;         // Stream already at the data start; nothing to do.
            case FINISHED: {
                if (reset()) break;       // If we can reuse existing stream, nothing more to do.
                if (input != null) {
                    stream = null;        // Cleared first in case of error during `close()` call.
                    input.close();
                    input = null;
                }
                // Fall through for trying to create a new input stream.
            }
            case READING: {
                /*
                 * If the input stream is in use, or if we finished to use it but were unable to reset its position,
                 * then we need to create a new input stream (except if the input was a DOM in memory, which we can
                 * share). The `target` StaxStreamReader will be in charge of closing that stream.
                 */
                if (type != InputType.NODE) {
                    final String name = getDisplayName();
                    if (channelFactory == null) {
                        throw new ForwardOnlyStorageException(getLocale(), name, StandardOpenOption.READ);
                    }
                    inputOrFile = input = channelFactory.inputStream(name, listeners);
                    type = InputType.STREAM;
                    if (stream == null) {
                        stream = input;
                        state  = START;
                        mark();
                    }
                }
                break;
            }
        }
        /*
         * At this point we verified there is no write operation in progress and that the input stream (if not null)
         * is available for our use. Now we need to build a XMLStreamReader from that input. This is InputType work,
         * but that type may be null if the storage given by the user was not an InputStream, Reader or other types
         * recognized by InputType. In such case there are two possibilities:
         *
         *   - It may be an OutputStream, Writer or other types recognized by OutputType.
         *   - It may be a Path, File, URL or URI, which are intentionally not handled by Input/OutputType.
         */
        if (type == null) {
            if (storageToWriter != null) {
                final Closeable snapshot = storageToWriter.snapshot(inputOrFile);
                if (snapshot != null) {
                    // Do not set state to READING since the input in this block is a copy of data.
                    final XMLStreamReader reader = storageToWriter.inputType.create(this, snapshot);
                    target.stream = snapshot;
                    return reader;
                }
            }
            /*
             * Maybe that storage was a Path, File or URL, in which case the constructor should have opened an
             * InputStream for it. If not, then this was an unsupported storage type. However, the input stream
             * may have been converted to an output stream during a write operation, in which case we need to
             * convert it back to an input stream.
             */
            type  = InputType.STREAM;
            input = IOUtilities.toInputStream(input);
            if (input == null) {
                throw new UnsupportedStorageException(getLocale(), getFormatName(), storage, StandardOpenOption.READ);
            }
            inputOrFile = input;
            if (input != stream) {
                stream = input;
                mark();
            }
        }
        final XMLStreamReader reader = type.create(this, inputOrFile);
        target.stream = input;
        state = READING;
        return reader;
    }

    /**
     * Creates a new XML stream writer for writing the XML document.
     * If another {@code XMLStreamWriter} has already been created before this method call,
     * whether this method will succeed in creating a new writer depends on the storage type
     * (e.g. file or output stream).
     *
     * @param  target     the writer which will store the {@code XMLStreamWriter} reference.
     * @param  temporary  the temporary stream where to write, or {@code null} for the main storage.
     * @return a new writer for writing the XML data.
     * @throws DataStoreException if the output type is not recognized or the data store is closed.
     * @throws XMLStreamException if an error occurred while opening the XML file.
     * @throws IOException if an error occurred while preparing the output stream.
     */
    final synchronized XMLStreamWriter createWriter(final StaxStreamWriter target, final OutputStream temporary)
            throws DataStoreException, XMLStreamException, IOException
    {
        Closeable output;
        Object outputOrFile;
        OutputType outputType;
        if (temporary == null) {
            output       = stream;
            outputOrFile = storage;
            outputType   = storageToWriter;
            if (outputOrFile == null) {
                throw new DataStoreClosedException(getLocale(), getFormatName(), StandardOpenOption.WRITE);
            }
            switch (state) {
                default:       throw new AssertionError(state);
                case READING:  throw new ConcurrentReadException (getLocale(), getDisplayName());
                case WRITING:  throw new ConcurrentWriteException(getLocale(), getDisplayName());
                case START:    break;         // Stream already at the data start; nothing to do.
                case FINISHED: {
                    if (reset()) break;
                    throw new ForwardOnlyStorageException(getLocale(), getDisplayName(), StandardOpenOption.WRITE);
                }
            }
            /*
             * If the storage given by the user was not one of OutputStream, Writer or other type recognized
             * by OutputType, then maybe that storage was a Path, File or URL, in which case the constructor
             * should have opened an InputStream (not an OutputStream) for it. In some cases (e.g. reading a
             * channel opened on a file), the input stream can be converted to an output stream.
             */
            if (outputType == null) {
                outputType = OutputType.STREAM;
                outputOrFile = output = IOUtilities.toOutputStream(output);
                if (output == null) {
                    throw new UnsupportedStorageException(getLocale(), getFormatName(), outputOrFile, StandardOpenOption.WRITE);
                }
                if (output != stream) {
                    stream = output;
                    mark();
                }
            }
        } else {
            outputType = OutputType.STREAM;
            outputOrFile = output = temporary;
        }
        XMLStreamWriter writer = outputType.create(this, outputOrFile);
        if (indentation >= 0) {
            writer = new FormattedWriter(writer, indentation);
        }
        target.stream = output;
        if (temporary == null) {
            state = WRITING;
        }
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
        if (finished == stream) {
            state = FINISHED;
            return false;
        }
        return true;
    }

    /**
     * Closes the input or output stream and releases any resources used by this XML data store.
     * This data store cannot be used anymore after this method has been invoked.
     *
     * <h4>Note for implementers</h4>
     * Implementations should invoke {@code listeners.close()} on their first line
     * before to clear their resources and to invoke {@code super.close()}.
     *
     * @throws DataStoreException if an error occurred while closing the input or output stream.
     */
    @Override
    public void close() throws DataStoreException {
        try {
            final AutoCloseable s = stream;
            if (s != null) s.close();
        } catch (DataStoreException e) {
            throw e;
        } catch (Exception e) {
            throw new DataStoreException(e);
        } finally {
            synchronized (this) {
                outputFactory = null;
                inputFactory  = null;
                storage       = null;
                stream        = null;
            }
        }
    }
}
