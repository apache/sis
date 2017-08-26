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
package org.apache.sis.storage;

import java.util.Map;
import java.util.Queue;
import java.util.Iterator;
import java.util.Collections;
import java.util.LinkedList;
import java.util.IdentityHashMap;
import java.util.ConcurrentModificationException;
import java.io.Reader;
import java.io.DataInput;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.CorruptedObjectException;
import org.apache.sis.internal.storage.Resources;
import org.apache.sis.internal.storage.io.IOUtilities;
import org.apache.sis.internal.storage.io.ChannelFactory;
import org.apache.sis.internal.storage.io.ChannelDataInput;
import org.apache.sis.internal.storage.io.ChannelImageInputStream;
import org.apache.sis.internal.storage.io.InputStreamAdapter;
import org.apache.sis.setup.OptionKey;

// Branch-dependent imports
import java.util.function.Consumer;


/**
 * Information for creating a connection to a {@link DataStore} in read and/or write mode.
 * {@code StorageConnector} wraps an input {@link Object}, which can be any of the following types:
 *
 * <ul>
 *   <li>A {@link java.nio.file.Path} or a {@link java.io.File} for a file or a directory.</li>
 *   <li>A {@link java.net.URI} or a {@link java.net.URL} to a distant resource.</li>
 *   <li>A {@link CharSequence} interpreted as a filename or a URL.</li>
 *   <li>A {@link java.nio.channels.Channel}, {@link DataInput}, {@link InputStream} or {@link Reader}.</li>
 *   <li>A {@link DataSource} or a {@link Connection} to a JDBC database.</li>
 *   <li>Any other {@code DataStore}-specific object, for example {@link ucar.nc2.NetcdfFile}.</li>
 * </ul>
 *
 * The {@link #getStorageAs(Class)} method provides the storage as an object of the given type, opening
 * the input stream if necessary. This class tries to open the stream only once - subsequent invocation
 * of {@code getStorageAs(…)} may return the same input stream.
 *
 * <p>This class is used only for discovery of a {@code DataStore} implementation capable to handle the input.
 * Once a suitable {@code DataStore} has been found, the {@code StorageConnector} instance is typically
 * discarded since each data store implementation will use their own input/output objects.</p>
 *
 * <p>Instances of this class are serializable if the {@code storage} object given at construction time
 * is serializable.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.3
 * @module
 */
public class StorageConnector implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2524083964906593093L;

    /**
     * The default size of the {@link ByteBuffer} to be created.
     * Users can override this value by providing a value for {@link OptionKey#BYTE_BUFFER}.
     */
    static final int DEFAULT_BUFFER_SIZE = 4096;

    /**
     * The minimal size of the {@link ByteBuffer} to be created. This size is used only
     * for temporary buffers that are unlikely to be used for the actual reading process.
     */
    static final int MINIMAL_BUFFER_SIZE = 256;

    /**
     * Handler to {@code StorageConnector.createFoo()} methods associated to given storage types.
     * Each {@code createFoo()} method may be invoked once for opening an input stream, character
     * reader, database connection, <i>etc</i> from user-supplied path, URI, <i>etc</i>.
     *
     * @param  <T>  the type of input created by this {@code Opener} instance.
     */
    @FunctionalInterface
    private interface Opener<T> {
        /**
         * Invoked when first needed for creating an input of the requested type.
         * The enclosing {@link StorageConnector} is responsible for caching the result.
         */
        T open(StorageConnector c) throws Exception;
    }

    /** Helper method for {@link #OPENERS} static initialization. */
    private static <T> void add(final Class<T> type, final Opener<T> op) {
        if (OPENERS.put(type, op) != null) throw new AssertionError(type);
    }

    /**
     * List of types recognized by {@link #getStorageAs(Class)}, associated to the methods for opening stream
     * of those types. This map shall contain every types documented in {@link #getStorageAs(Class)} javadoc.
     */
    private static final Map<Class<?>, Opener<?>> OPENERS = new IdentityHashMap<>(8);
    static {
        add(String.class,           StorageConnector::createString);
        add(ByteBuffer.class,       StorageConnector::createByteBuffer);
        add(DataInput.class,        StorageConnector::createDataInput);
        add(ImageInputStream.class, StorageConnector::createImageInputStream);
        add(InputStream.class,      StorageConnector::createInputStream);
        add(Reader.class,           StorageConnector::createReader);
        add(Connection.class,       StorageConnector::createConnection);
        add(ChannelDataInput.class, (s) -> s.createChannelDataInput(false));    // Undocumented case (SIS internal)
    }

    /**
     * The input/output object given at construction time.
     *
     * @see #getStorage()
     */
    private final Object storage;

    /**
     * A name for the input/output object, or {@code null} if none.
     * This field is initialized only when first needed.
     */
    private transient String name;

    /**
     * The filename extension, or {@code null} if none.
     * This field is initialized only when first needed.
     */
    private transient String extension;

    /**
     * The options, created only when first needed.
     *
     * @see #getOption(OptionKey)
     * @see #setOption(OptionKey, Object)
     */
    private Map<OptionKey<?>, Object> options;

    /**
     * Views of {@link #storage} as some of the following supported types:
     *
     * <ul>
     *   <li>{@link ByteBuffer}:
     *       A read-only view of the buffer over the first bytes of the stream.</li>
     *
     *   <li>{@link DataInput}:
     *       The input as a data input stream. Unless the {@link #storage} is already an instance of {@link DataInput},
     *       this entry will be given an instance of {@link ChannelImageInputStream} if possible rather than an arbitrary
     *       stream. In particular, we invoke the {@link ImageIO#createImageInputStream(Object)} factory method only in
     *       last resort because some SIS data stores will want to access the channel and buffer directly.</li>
     *
     *   <li>{@link ImageInputStream}:
     *       Same as {@code DataInput} if it can be casted, or {@code null} otherwise.</li>
     *
     *   <li>{@link InputStream}:
     *       If not explicitely provided, this is a wrapper around the above {@link ImageInputStream}.</li>
     *
     *   <li>{@link Reader}:
     *       If not explicitely provided, this is a wrapper around the above {@link InputStream}.</li>
     *
     *   <li>{@link Connection}:
     *       The storage object as a JDBC connection.</li>
     * </ul>
     *
     * A non-existent entry means that the value has not yet been computed. A {@link Void#TYPE} value means the value
     * has been computed and we have determined that {@link #getStorageAs(Class)} shall returns {@code null} for that
     * type.
     *
     * @see #getStorageAs(Class)
     */
    private transient Map<Class<?>, Object> views;

    /**
     * The views that need to be synchronized if {@link #storage} is used independently. They are views
     * that may advance {@code storage} position, but not necessarily in same time than the view position
     * (typically because the view reads some bytes in advance and stores them in a buffer). This map may
     * be non-null when the storage is an {@link InputStream}, {@link java.io.OutputStream} or a
     * {@link java.nio.channels.Channel}. Those views can be:
     *
     * <ul>
     *   <li>{@link Reader} that are wrappers around {@code InputStream}.</li>
     *   <li>{@link ChannelDataInput} when the channel come from an {@code InputStream}.</li>
     *   <li>{@link ChannelDataInput} when the channel has been explicitely given to the constructor.</li>
     * </ul>
     *
     * Note that we do <strong>not</strong> include {@link InputStreamAdapter} because it does not use buffer;
     * {@code InputStreamAdapter} positions are synchronized with wrapped {@link ImageInputStream} positions.
     *
     * <p>Values are cleanup actions to execute after the {@link #storage} has been reseted to its original position.
     * A {@code null} value means that the view can not be synchronized and consequently should be discarded.</p>
     */
    private transient Map<Class<?>, Consumer<StorageConnector>> viewsToSync;

    /**
     * Objects which will need to be closed by the {@link #closeAllExcept(Object)} method.
     * For each (<var>key</var>, <var>value</var>) entry, if the object to close (the key)
     * is a wrapper around an other object (e.g. an {@link InputStreamReader} wrapping an
     * {@link InputStream}), then the value is the other object.
     *
     * @see #addViewToClose(Object, Object)
     * @see #closeAllExcept(Object)
     */
    private transient Map<Object, Object> viewsToClose;

    /**
     * The view returned by the last call to {@link #getStorageAs(Class)}.
     *
     * @see #storage
     */
    private transient Object lastView;

    /**
     * Creates a new data store connection wrapping the given input/output object.
     * The object can be of any type, but the class javadoc lists the most typical ones.
     *
     * @param storage  the input/output object as a URL, file, image input stream, <i>etc.</i>.
     */
    public StorageConnector(final Object storage) {
        ArgumentChecks.ensureNonNull("storage", storage);
        this.storage = storage;
    }

    /**
     * Returns the option value for the given key, or {@code null} if none.
     *
     * @param  <T>  the type of option value.
     * @param  key  the option for which to get the value.
     * @return the current value for the given option, or {@code null} if none.
     */
    public <T> T getOption(final OptionKey<T> key) {
        ArgumentChecks.ensureNonNull("key", key);
        return key.getValueFrom(options);
    }

    /**
     * Sets the option value for the given key. The default implementation recognizes the following options:
     *
     * <ul>
     *   <li>{@link OptionKey#ENCODING}     for decoding characters in an input stream, if needed.</li>
     *   <li>{@link OptionKey#URL_ENCODING} for converting URL to URI or filename, if needed.</li>
     *   <li>{@link OptionKey#OPEN_OPTIONS} for specifying whether the data store shall be read only or read/write.</li>
     *   <li>{@link OptionKey#BYTE_BUFFER}  for allowing users to control the byte buffer to be created.</li>
     * </ul>
     *
     * @param <T>    the type of option value.
     * @param key    the option for which to set the value.
     * @param value  the new value for the given option, or {@code null} for removing the value.
     */
    public <T> void setOption(final OptionKey<T> key, final T value) {
        ArgumentChecks.ensureNonNull("key", key);
        options = key.setValueInto(options, value);
    }

    /**
     * Returns the input/output object given at construction time.
     * The object can be of any type, but the class javadoc lists the most typical ones.
     *
     * @return the input/output object as a URL, file, image input stream, <i>etc.</i>.
     * @throws DataStoreException if an error occurred while reseting the input stream or channel to its original position.
     *
     * @see #getStorageAs(Class)
     */
    public Object getStorage() throws DataStoreException {
        if (viewsToSync != null && storage != lastView) {
            resetStorage();
        }
        lastView = storage;
        return storage;
    }

    /**
     * Returns a short name of the input/output object. The default implementation performs
     * the following choices based on the type of the {@linkplain #getStorage() storage} object:
     *
     * <ul>
     *   <li>For {@link java.nio.file.Path}, {@link java.io.File}, {@link java.net.URI} or {@link java.net.URL}
     *       instances, this method uses dedicated API like {@link java.nio.file.Path#getFileName()}.</li>
     *   <li>For {@link CharSequence} instances, this method gets a string representation of the storage object
     *       and returns the part after the last {@code '/'} character or platform-dependent name separator.</li>
     *   <li>For instances of unknown type, this method builds a string representation using the class name.
     *       Note that the string representation of unknown types may change in any future SIS version.</li>
     * </ul>
     *
     * @return a short name of the storage object.
     */
    public String getStorageName() {
        if (name == null) {
            name = IOUtilities.filename(storage);
            if (name == null) {
                name = Classes.getShortClassName(storage);
            }
        }
        return name;
    }

    /**
     * Returns the filename extension of the input/output object. The default implementation performs
     * the following choices based on the type of the {@linkplain #getStorage() storage} object:
     *
     * <ul>
     *   <li>For {@link java.nio.file.Path}, {@link java.io.File}, {@link java.net.URI}, {@link java.net.URL} or
     *       {@link CharSequence} instances, this method returns the string after the last {@code '.'} character
     *       in the filename, provided that the {@code '.'} is not the first filename character. This may be an
     *       empty string if the filename has no extension, but never {@code null}.</li>
     *   <li>For instances of unknown type, this method returns {@code null}.</li>
     * </ul>
     *
     * @return the filename extension, or an empty string if none,
     *         or {@code null} if the storage is an object of unknown type.
     */
    public String getFileExtension() {
        if (extension == null) {
            extension = IOUtilities.extension(storage);
        }
        return extension;
    }

    /**
     * Returns the storage as a view of the given type if possible, or {@code null} otherwise.
     * The default implementation accepts the following types:
     *
     * <ul>
     *   <li>{@link String}:
     *     <ul>
     *       <li>If the {@linkplain #getStorage() storage} object is an instance of the {@link java.nio.file.Path},
     *           {@link java.io.File}, {@link java.net.URL}, {@link java.net.URI} or {@link CharSequence} types,
     *           returns the string representation of their path.</li>
     *
     *       <li>Otherwise this method returns {@code null}.</li>
     *     </ul>
     *   </li>
     *   <li>{@link ByteBuffer}:
     *     <ul>
     *       <li>If the {@linkplain #getStorage() storage} object can be obtained as described in bullet 2 of the
     *           {@code DataInput} section below, then this method returns the associated byte buffer.</li>
     *
     *       <li>Otherwise this method returns {@code null}.</li>
     *     </ul>
     *   </li>
     *   <li>{@link DataInput}:
     *     <ul>
     *       <li>If the {@linkplain #getStorage() storage} object is already an instance of {@code DataInput}
     *           (including the {@link ImageInputStream} and {@link javax.imageio.stream.ImageOutputStream} types),
     *           then it is returned unchanged.</li>
     *
     *       <li>Otherwise if the input is an instance of {@link java.nio.file.Path}, {@link java.io.File},
     *           {@link java.net.URI}, {@link java.net.URL}, {@link CharSequence}, {@link InputStream} or
     *           {@link java.nio.channels.ReadableByteChannel}, then an {@link ImageInputStream} backed by a
     *           {@link ByteBuffer} is created when first needed and returned.</li>
     *
     *       <li>Otherwise if {@link ImageIO#createImageInputStream(Object)} returns a non-null value,
     *           then this value is cached and returned.</li>
     *
     *       <li>Otherwise this method returns {@code null}.</li>
     *     </ul>
     *   </li>
     *   <li>{@link ImageInputStream}:
     *     <ul>
     *       <li>If the above {@code DataInput} can be created and casted to {@code ImageInputStream}, returns it.</li>
     *
     *       <li>Otherwise this method returns {@code null}.</li>
     *     </ul>
     *   </li>
     *   <li>{@link InputStream}:
     *     <ul>
     *       <li>If the {@linkplain #getStorage() storage} object is already an instance of {@link InputStream},
     *           then it is returned unchanged.</li>
     *
     *       <li>Otherwise if the above {@code ImageInputStream} can be created,
     *           returns a wrapper around that stream.</li>
     *
     *       <li>Otherwise this method returns {@code null}.</li>
     *     </ul>
     *   </li>
     *   <li>{@link Reader}:
     *     <ul>
     *       <li>If the {@linkplain #getStorage() storage} object is already an instance of {@link Reader},
     *           then it is returned unchanged.</li>
     *
     *       <li>Otherwise if the above {@code InputStream} can be created, returns an {@link InputStreamReader}
     *           using the encoding specified by {@link OptionKey#ENCODING} if any, or using the system default
     *           encoding otherwise.</li>
     *
     *       <li>Otherwise this method returns {@code null}.</li>
     *     </ul>
     *   </li>
     *   <li>{@link Connection}:
     *     <ul>
     *       <li>If the {@linkplain #getStorage() storage} object is already an instance of {@link Connection},
     *           then it is returned unchanged.</li>
     *
     *       <li>Otherwise if the storage is an instance of {@link DataSource}, then a connection is obtained
     *           when first needed and returned.</li>
     *
     *       <li>Otherwise this method returns {@code null}.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * Multiple invocations of this method on the same {@code StorageConnector} instance will try
     * to return the same instance on a <cite>best effort</cite> basis. Consequently, implementations of
     * {@link DataStoreProvider#probeContent(StorageConnector)} methods shall not close the stream or
     * database connection returned by this method. In addition, those {@code probeContent(StorageConnector)}
     * methods are responsible for restoring the stream or byte buffer to its original position on return.
     *
     * @param  <T>   the compile-time type of the {@code type} argument.
     * @param  type  the desired type as one of {@code ByteBuffer}, {@code DataInput}, {@code Connection}
     *               class or other type supported by {@code StorageConnector} subclasses.
     * @return the storage as a view of the given type, or {@code null} if no view can be created for the given type.
     * @throws IllegalArgumentException if the given {@code type} argument is not a supported type.
     * @throws DataStoreException if an error occurred while opening a stream or database connection.
     *
     * @see #getStorage()
     * @see #closeAllExcept(Object)
     */
    public <T> T getStorageAs(final Class<T> type) throws IllegalArgumentException, DataStoreException {
        ArgumentChecks.ensureNonNull("type", type);
        /*
         * Verify if the cache contains an instance created by a previous invocation of this method.
         * Note that InputStream may need to be reset if it has been used indirectly by other kind
         * of stream (for example a java.io.Reader); this will be done at the end of this method.
         */
        Object value;
        final boolean cache;
        if (views != null && (value = views.get(type)) != null) {
            if (value == Void.TYPE) return null;
            cache = false;
        } else {
            cache = true;
            value = storage;
            if (!type.isInstance(value)) {
                /*
                 * No instance has been created previously for the requested type. Open the stream now,
                 * then cache it for future reuse. Note that we may cache 'null' value if no stream of
                 * the given type can be created.
                 */
                final Opener<?> method = OPENERS.get(type);
                if (method != null) try {
                    value = method.open(this);
                } catch (RuntimeException | DataStoreException e) {
                    throw e;
                } catch (Exception e) {
                    throw new DataStoreException(Errors.format(Errors.Keys.CanNotOpen_1, getStorageName()), e);
                } else if (type == ChannelFactory.class) {                 // Undocumented case (SIS internal).
                    /*
                     * ChannelFactory may have been created as a side effect of creating a ReadableByteChannel.
                     * Caller should have asked for another type (e.g. InputStream) before to ask for this type.
                     */
                    return null;                    // Do not cache since the instance may be created later.
                } else {
                    /*
                     * If the type is not one of the types listed in OPENERS, we delegate to ObjectConverter.
                     * It will throw UnconvertibleObjectException (an IllegalArgumentException subtype) if
                     * the given type is unrecognized.
                     */
                    value = ObjectConverters.convert(storage, type);
                }
            }
        }
        /*
         * If the user asked an InputStream, we may return the storage as-is if it was already an InputStream.
         * However before doing so, we may need to reset the InputStream position if the stream has been used
         * by a ChannelDataInput or an InputStreamReader.
         */
        final T view = type.cast(value);
        if (viewsToSync != null && view != lastView && (view == storage || viewsToSync.containsKey(type))) {
            resetStorage();
        }
        if (cache) {
            addView(type, view);                // Shall be after 'resetStorage()'.
        }
        lastView = view;
        return view;
    }

    /**
     * Mark the storage position before to create a view that may be a wrapper around that storage.
     */
    private void markStorage() {
        if (storage instanceof InputStream) {
            ((InputStream) storage).mark(DEFAULT_BUFFER_SIZE);
        }
    }

    /**
     * Assuming that {@link #storage} is an instance of {@link InputStream}, {@link ReadableByteChannel} or other
     * objects that may be affected by views operations, resets the storage position. This method is the converse
     * of {@link #markStorage()}.
     *
     * <div class="note"><b>Rational:</b>
     * {@link DataStoreProvider#probeContent(StorageConnector)} contract requires that implementors reset the
     * input stream themselves. However if {@link ChannelDataInput} or {@link InputStreamReader} has been used,
     * then the user performed a call to {@link ChannelDataInput#reset()} (for instance), which did not reseted
     * the underlying input stream. So we need to perform the missing {@link InputStream#reset()} here, then
     * synchronize the {@code ChannelDataInput} position accordingly.</div>
     */
    private <T> void resetStorage() throws DataStoreException {
        if (lastView != null) {
            /*
             * We must reset InputStream or ReadableChannel position before to run cleanup code.
             * Note on InputStream.reset() behavior documented in java.io:
             *
             *  - It does not discard the mark, so it is okay if reset() is invoked twice.
             *  - If mark is unsupported, may either throw IOException or reset the stream
             *    to an implementation-dependent fixed state.
             */
            boolean isReset = false;
            IOException cause = null;
            try {
                if (storage instanceof InputStream) {
                    ((InputStream) storage).reset();
                    isReset = true;
                } else if (storage instanceof SeekableByteChannel) {
                    ((SeekableByteChannel) storage).position(getView(ChannelDataInput.class).channelOffset);
                    isReset = true;
                }
            } catch (IOException e) {
                cause = e;
            }
            if (!isReset) {
                throw new ForwardOnlyStorageException(Resources.format(
                        Resources.Keys.StreamIsReadOnce_1, getStorageName()), cause);
            }
            /*
             * At this point the InputStream or ReadableChannel has been reset.
             * Now reset or remove any view that depend on it.
             */
            for (final Map.Entry<Class<?>, Consumer<StorageConnector>> entry : viewsToSync.entrySet()) {
                final Consumer<StorageConnector> sync = entry.getValue();
                if (sync != null) {
                    sync.accept(this);
                } else {
                    removeView(entry.getKey());             // Reader will need to be recreated from scratch.
                }
            }
        }
    }

    /**
     * Resets {@link ChannelDataInput} after the {@link InputStream} has been reseted.
     * This method is registered in {@link #viewsToSync} when a {@link ChannelDataInput} is created.
     */
    private void resetChannelDataInput() {
        ChannelDataInput channel = getView(ChannelDataInput.class);     // Should never be null.
        channel.buffer.limit(0);                                        // Must be after storage.reset().
        channel.setStreamPosition(0);                                   // Must be after buffer.limit(0).
    }

    /**
     * Gets or creates a view for the input as a {@link ChannelDataInput} if possible. If {@code ChannelDataInput}
     * instance already exists, then this method returns it as-is (this method does <strong>not</strong> verify if
     * the {@code ChannelDataInput} instance is an image input stream). Otherwise a new {@code ChannelDataInput}
     * is created (if possible), cached and returned.
     *
     * @param  asImageInputStream  whether new {@code ChannelDataInput} should be {@link ChannelImageInputStream}.
     *         This argument is ignored if a {@code ChannelDataInput} instance already exists.
     * @return the existing or new {@code ChannelDataInput}, or {@code null} if none can be created.
     */
    private ChannelDataInput getChannelDataInput(final boolean asImageInputStream) throws IOException, DataStoreException {
        if (views != null) {
            final Object view = views.get(ChannelDataInput.class);
            if (view != null) {
                return (view != Void.TYPE) ? (ChannelDataInput) view : null;
            }
        }
        final ChannelDataInput view = createChannelDataInput(asImageInputStream);       // May be null.
        addView(ChannelDataInput.class, view);                                          // Cache even if null.
        return view;
    }

    /**
     * Creates a view for the input as a {@link ChannelDataInput} if possible.
     * This is also a starting point for {@link #createDataInput()} and {@link #createByteBuffer()}.
     * This method is one of the {@link #OPENERS} methods and should be invoked at most once per
     * {@code StorageConnector} instance.
     *
     * @param  asImageInputStream  whether the {@code ChannelDataInput} needs to be {@link ChannelImageInputStream} subclass.
     * @throws IOException if an error occurred while opening a channel for the input.
     */
    private ChannelDataInput createChannelDataInput(final boolean asImageInputStream) throws IOException, DataStoreException {
        /*
         * Before to try to wrap an InputStream, mark its position so we can rewind if the user asks for
         * the InputStream directly. We need to reset because ChannelDataInput may have read some bytes.
         * Note that if mark is unsupported, the default InputStream.mark() implementation does nothing.
         * See above 'resetStorage()' method.
         */
        markStorage();
        /*
         * Following method call recognizes ReadableByteChannel, InputStream (with special case for FileInputStream),
         * URL, URI, File, Path or other types that may be added in future Apache SIS versions.
         */
        final ChannelFactory factory = ChannelFactory.prepare(storage,
                getOption(OptionKey.URL_ENCODING), false, getOption(OptionKey.OPEN_OPTIONS));
        if (factory == null) {
            return null;
        }
        /*
         * ChannelDataInput depends on ReadableByteChannel, which itself depends on storage
         * (potentially an InputStream). We need to remember this chain in 'viewsToClose' map.
         */
        final String name = getStorageName();
        final ReadableByteChannel channel = factory.reader(name);
        addViewToClose(channel, storage);
        ByteBuffer buffer = getOption(OptionKey.BYTE_BUFFER);       // User-supplied buffer.
        if (buffer == null) {
            buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);      // Default buffer if user did not specified any.
        }
        final ChannelDataInput asDataInput;
        if (asImageInputStream) {
            asDataInput = new ChannelImageInputStream(name, channel, buffer, false);
        } else {
            asDataInput = new ChannelDataInput(name, channel, buffer, false);
        }
        addViewToClose(asDataInput, channel);
        /*
         * Following is an undocumented mechanism for allowing some Apache SIS implementations of DataStore
         * to re-open the same channel or input stream another time, typically for re-reading the same data.
         */
        if (factory.canOpen()) {
            addView(ChannelFactory.class, factory);
        }
        /*
         * If the channels to be created by ChannelFactory are wrappers around InputStream or any other object
         * that may be affected when read operations will occur, we need to remember that fact in order to keep
         * the storage synchronized with the view.
         */
        if (factory.isCoupled()) {
            addViewToSync(ChannelDataInput.class, StorageConnector::resetChannelDataInput);
        }
        return asDataInput;
    }

    /**
     * Creates a view for the input as a {@link DataInput} if possible. This method performs the choice
     * documented in the {@link #getStorageAs(Class)} method for the {@code DataInput} case. Opening the
     * data input may imply creating a {@link ByteBuffer}, in which case the buffer will be stored under
     * the {@code ByteBuffer.class} key together with the {@code DataInput.class} case.
     *
     * <p>This method is one of the {@link #OPENERS} methods and should be invoked at most once per
     * {@code StorageConnector} instance.</p>
     *
     * @throws IOException if an error occurred while opening a stream for the input.
     */
    private DataInput createDataInput() throws IOException, DataStoreException {
        /*
         * Creates a ChannelImageInputStream instance. We really need that specific type because some
         * SIS data stores will want to access directly the channel and the buffer. We will fallback
         * on the ImageIO.createImageInputStream(Object) method only in last resort.
         */
        final ChannelDataInput c = getChannelDataInput(true);
        final DataInput asDataInput;
        if (c == null) {
            asDataInput = ImageIO.createImageInputStream(storage);
            addViewToClose(asDataInput, storage);
            /*
             * Note: Java Image I/O wrappers for Input/OutputStream do NOT close the underlying streams.
             * This is a complication for us. We could mitigate the problem by subclassing the standard
             * FileCacheImageInputStream and related classes, but we don't do that for now because this
             * code should never be executed for InputStream storage. Instead getChannelDataInput(true)
             * should have created a ChannelImageInputStream or ChannelDataInput.
             */
        } else if (c instanceof DataInput) {
            asDataInput = (DataInput) c;
            // No call to 'addViewToClose' because it has already be done by createChannelDataInput(…).
        } else {
            asDataInput = new ChannelImageInputStream(c);                       // Upgrade existing instance.
            if (views.put(ChannelDataInput.class, asDataInput) != c) {          // Replace the previous instance.
                throw new ConcurrentModificationException();
            }
            addViewToClose(asDataInput, c.channel);
            if (viewsToClose.remove(c) != c.channel) {                          // Shall be after 'addViewToClose'.
                throw new CorruptedObjectException();
            }
        }
        return asDataInput;
    }

    /**
     * Creates a {@link ByteBuffer} from the {@link ChannelDataInput} if possible, or from the
     * {@link ImageInputStream} otherwise. The buffer will be initialized with an arbitrary amount
     * of bytes read from the input. If this amount is not sufficient, it can be increased by a call
     * to {@link #prefetch()}.
     *
     * <p>This method is one of the {@link #OPENERS} methods and should be invoked at most once per
     * {@code StorageConnector} instance.</p>
     *
     * @throws IOException if an error occurred while opening a stream for the input.
     */
    private ByteBuffer createByteBuffer() throws IOException, DataStoreException {
        /*
         * First, try to create the ChannelDataInput if it does not already exists.
         * If successful, this will create a ByteBuffer companion as a side effect.
         */
        final ChannelDataInput c = getChannelDataInput(false);
        if (c != null) {
            return c.buffer.asReadOnlyBuffer();
        }
        /*
         * If no ChannelDataInput has been create by the above code, get the input as an ImageInputStream and
         * read an arbitrary amount of bytes. Read only a small amount of bytes because, at the contrary of the
         * buffer created in createChannelDataInput(boolean), the buffer created here is unlikely to be used for
         * the reading process after the recognition of the file format.
         */
        final ImageInputStream in = getStorageAs(ImageInputStream.class);
        if (in != null) {
            in.mark();
            final byte[] buffer = new byte[MINIMAL_BUFFER_SIZE];
            final int n = in.read(buffer);
            in.reset();
            if (n >= 1) {
                final ByteBuffer asByteBuffer = ByteBuffer.wrap(buffer).order(in.getByteOrder());
                asByteBuffer.limit(n);
                return asByteBuffer;
                // Can not invoke asReadOnly() because 'prefetch()' need to be able to write in it.
            }
        }
        return null;
    }

    /**
     * Transfers more bytes from the {@link DataInput} to the {@link ByteBuffer}, if possible.
     * This method returns {@code true} on success, or {@code false} if input is not a readable
     * channel or stream, we have reached the end of stream, or the buffer is full.
     *
     * <p>This method is invoked when the amount of bytes in the buffer appears to be insufficient
     * for {@link DataStoreProvider#probeContent(StorageConnector)} purpose.</p>
     *
     * @return {@code true} on success.
     * @throws DataStoreException if an error occurred while reading more bytes.
     */
    final boolean prefetch() throws DataStoreException {
        try {
            final ChannelDataInput c = getView(ChannelDataInput.class);
            if (c != null) {
                return c.prefetch() > 0;
            }
            /*
             * The above code is the usual case. The code below this point is the fallback used when only
             * an ImageInputStream was available. In such case, the ByteBuffer can only be the one created
             * by the above createByteBuffer() method, which is known to be backed by a writable array.
             */
            final ImageInputStream input = getView(ImageInputStream.class);
            if (input != null) {
                final ByteBuffer buffer = getView(ByteBuffer.class);
                if (buffer != null) {
                    final int p = buffer.limit();
                    final long mark = input.getStreamPosition();
                    input.seek(Math.addExact(mark, p));
                    final int n = input.read(buffer.array(), p, buffer.capacity() - p);
                    input.seek(mark);
                    if (n > 0) {
                        buffer.limit(p + n);
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            throw new DataStoreException(Errors.format(Errors.Keys.CanNotRead_1, getStorageName()), e);
        }
        return false;
    }

    /**
     * Creates an {@link ImageInputStream} from the {@link DataInput} if possible. This method simply
     * casts {@code DataInput} is such cast is allowed. Since {@link #createDataInput()} instantiates
     * {@link ChannelImageInputStream}, this cast is usually possible.
     *
     * <p>This method is one of the {@link #OPENERS} methods and should be invoked at most once per
     * {@code StorageConnector} instance.</p>
     */
    private ImageInputStream createImageInputStream() throws DataStoreException {
        final DataInput input = getStorageAs(DataInput.class);
        return (input instanceof ImageInputStream) ? (ImageInputStream) input : null;
    }

    /**
     * Creates an input stream from {@link ReadableByteChannel} if possible, or from {@link ImageInputStream}
     * otherwise.
     *
     * <p>This method is one of the {@link #OPENERS} methods and should be invoked at most once per
     * {@code StorageConnector} instance.</p>
     */
    private InputStream createInputStream() throws IOException, DataStoreException {
        final DataInput input = getStorageAs(DataInput.class);
        if (input instanceof InputStream) {
            return (InputStream) input;
        }
        if (input instanceof ImageInputStream) {
            final InputStream c = new InputStreamAdapter((ImageInputStream) input);
            addViewToClose(c, input);
            return c;
        }
        return null;
    }

    /**
     * Creates a character reader if possible.
     *
     * <p>This method is one of the {@link #OPENERS} methods and should be invoked at most once per
     * {@code StorageConnector} instance.</p>
     */
    private Reader createReader() throws DataStoreException {
        final InputStream input = getStorageAs(InputStream.class);
        if (input == null) {
            return null;
        }
        markStorage();
        final Charset encoding = getOption(OptionKey.ENCODING);
        final Reader c = (encoding != null) ? new InputStreamReader(input, encoding)
                                            : new InputStreamReader(input);
        /*
         * Current implementation does not wrap the above Reader in a BufferedReader because:
         *
         * 1) InputStreamReader already uses a buffer internally.
         * 2) InputStreamReader does not support mark/reset, which is a desired limitation for now.
         *    This is because reseting the Reader would not reset the underlying InputStream, which
         *    would cause other DataStoreProvider.probeContent(…) methods to fail if they try to use
         *    the InputStream. For now we let the InputStreamReader.mark() to throw an IOException,
         *    but we may need to provide our own subclass of BufferedReader in a future SIS version
         *    if mark/reset support is needed here.
         */
        addViewToClose(c, input);
        addViewToSync(Reader.class, null);
        return c;
    }

    /**
     * Creates a database connection if possible.
     *
     * <p>This method is one of the {@link #OPENERS} methods and should be invoked at most once per
     * {@code StorageConnector} instance.</p>
     */
    private Connection createConnection() throws SQLException {
        if (storage instanceof DataSource) {
            final Connection c = ((DataSource) storage).getConnection();
            addViewToClose(c, storage);
            return c;
        }
        return null;
    }

    /**
     * Returns the storage as a path if possible, or {@code null} otherwise.
     *
     * <p>This method is one of the {@link #OPENERS} methods and should be invoked at most once per
     * {@code StorageConnector} instance.</p>
     */
    private String createString() {
        return IOUtilities.toString(storage);
    }

    /**
     * Adds the given view in the cache.
     *
     * @param  <T>   the compile-time type of the {@code type} argument.
     * @param  type  the view type.
     * @param  view  the view, or {@code null} if none.
     */
    private <T> void addView(final Class<T> type, final T view) {
        if (views == null) {
            views = new IdentityHashMap<>();
        }
        if (views.put(type, (view != null) ? view : Void.TYPE) != null) {
            // Should never happen, unless someone used this StorageConnector in another thread.
            throw new ConcurrentModificationException();
        }
    }

    /**
     * Returns the view for the given type from the cache.
     *
     * @param  <T>   the compile-time type of the {@code type} argument.
     * @param  type  the view type.
     * @return the view, or {@code null} if none.
     */
    private <T> T getView(final Class<T> type) {
        // Note: this method is always invoked in a context where 'views' can not be null.
        final Object view = views.get(type);
        return (view != Void.TYPE) ? type.cast(view) : null;
    }

    /**
     * Removes the given view from the cache.
     * This method is invoked for forcing the view to be recreated if requested again.
     *
     * @param type  the view type to remove.
     */
    private void removeView(final Class<?> type) {
        if (views.remove(type) != null) {
            viewsToClose.remove(type);
        }
    }

    /**
     * Declares that the view of the given type is coupled with {@link #storage}.
     * A change of view position will change storage position, and vis-versa.
     * See {@link #viewsToSync} for more information.
     *
     * @param  sync  action to execute after {@link #storage} has been reset,
     *               or {@code null} if the view should be removed.
     */
    private void addViewToSync(final Class<?> type, final Consumer<StorageConnector> sync) {
        if (viewsToSync == null) {
            viewsToSync = new IdentityHashMap<>(4);
        }
        if (viewsToSync.put(type, sync) != null) {
            // Should never happen, unless someone used this StorageConnector in another thread.
            throw new ConcurrentModificationException();
        }
    }

    /**
     * Declares that the given {@code input} will need to be closed by the {@link #closeAllExcept(Object)} method.
     * The {@code input} argument is always a new instance wrapping, directly or indirectly, the {@link #storage}.
     * Callers must specify the wrapped object in the {@code delegate} argument.
     *
     * @param  input     the newly created object which will need to be closed.
     * @param  delegate  the object wrapped by the given {@code input}.
     */
    private void addViewToClose(final Object input, final Object delegate) {
        if (viewsToClose == null) {
            viewsToClose = new IdentityHashMap<>(4);
        }
        if (viewsToClose.put(input, delegate) != null) {
            // Should never happen, unless someone used this StorageConnector in another thread.
            throw new ConcurrentModificationException();
        }
    }

    /**
     * Closes all streams and connections created by this {@code StorageConnector} except the given view.
     * This method closes all objects created by the {@link #getStorageAs(Class)} method except the given {@code view}.
     * If {@code view} is {@code null}, then this method closes everything including the {@linkplain #getStorage()
     * storage} if it is closeable.
     *
     * <p>This method is invoked when a suitable {@link DataStore} has been found - in which case the view used
     * by the data store is given in argument to this method - or when no suitable {@code DataStore} has been
     * found - in which case the {@code view} argument is null.</p>
     *
     * <p>This {@code StorageConnector} instance shall not be used anymore after invocation of this method.</p>
     *
     * @param  view  the view to leave open, or {@code null} if none.
     * @throws DataStoreException if an error occurred while closing the stream or database connection.
     *
     * @see #getStorageAs(Class)
     * @see DataStoreProvider#open(StorageConnector)
     */
    public void closeAllExcept(final Object view) throws DataStoreException {
        final Map<Object,Object> toClose = viewsToClose;
        viewsToClose = Collections.emptyMap();
        views        = Collections.emptyMap();
        if (toClose == null) {
            if (storage != view && storage instanceof AutoCloseable) try {
                ((AutoCloseable) storage).close();
            } catch (Exception e) {
                throw new DataStoreException(e);
            }
            return;
        }
        /*
         * The "AutoCloseable.close() is not indempotent" problem
         * ------------------------------------------------------
         * We will need a set of objects to close without duplicated values. For example the values associated to the
         * 'ImageInputStream.class' and 'DataInput.class' keys are often the same instance.  We must avoid duplicated
         * values because 'ImageInputStream.close()' is not indempotent,  i.e.  invoking their 'close()' method twice
         * will thrown an IOException.
         *
         * Generally speaking, all AutoCloseable instances are not guaranteed to be indempotent because this is not
         * required by the interface contract. Consequently we must be careful to not invoke the close() method on
         * the same instance twice (indirectly or indirectly).
         *
         * The set of objects to close will be the keys of the 'viewsToClose' map. It can not be the values of the
         * 'views' map.
         */
        toClose.put(storage, null);
        if (view != null) {
            /*
             * If there is a view to not close, search for all views that are wrapper for the given view.
             * Those wrappers shall not be closed. For example if the caller does not want to close the
             * InputStream view, then we shall not close the InputStreamReader wrapper neither.
             */
            final Queue<Object> deferred = new LinkedList<>();
            Object doNotClose = view;
            do {
                final Iterator<Map.Entry<Object,Object>> it = toClose.entrySet().iterator();
                while (it.hasNext()) {
                    final Map.Entry<Object,Object> entry = it.next();
                    if (entry.getValue() == doNotClose) {
                        deferred.add(entry.getKey());
                        it.remove();
                    }
                }
                doNotClose = deferred.poll();
            } while (doNotClose != null);
        }
        /*
         * Remove the view to not close. If that view is a wrapper for an other object, do not close the
         * wrapped object neither. Proceed the dependency chain up to the original 'storage' object.
         */
        for (Object doNotClose = view; doNotClose != null;) {
            doNotClose = toClose.remove(doNotClose);
        }
        /*
         * Remove all wrapped objects. After this loop, only the "top level" objects should remain
         * (typically only one object). This block is needed because of the "AutoCloseable.close()
         * is not idempotent" issue, otherwise we could have omitted it.
         */
        for (final Object delegate : toClose.values().toArray()) { // 'toArray()' is for avoiding ConcurrentModificationException.
            toClose.remove(delegate);
        }
        /*
         * Now close all remaining items. If an exception occurs, we will propagate it only after we are
         * done closing all items.
         */
        DataStoreException failure = null;
        for (final Object c : toClose.keySet()) {
            if (c instanceof AutoCloseable) try {
                ((AutoCloseable) c).close();
            } catch (Exception e) {
                if (failure == null) {
                    failure = new DataStoreException(e);
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    /**
     * Returns a string representation of this {@code StorageConnector} for debugging purpose.
     */
    @Debug
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(40);
        buffer.append(Classes.getShortClassName(this)).append("[“").append(getStorageName()).append('”');
        if (options != null) {
            buffer.append(", options=").append(options);
        }
        return buffer.append(']').toString();
    }
}
