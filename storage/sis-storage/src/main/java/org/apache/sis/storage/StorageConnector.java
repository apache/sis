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
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.sql.Connection;
import javax.sql.DataSource;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.storage.IOUtilities;
import org.apache.sis.internal.storage.ChannelDataInput;
import org.apache.sis.internal.storage.ChannelImageInputStream;
import org.apache.sis.setup.OptionKey;

// Related to JDK7
import org.apache.sis.internal.jdk7.JDK7;


/**
 * Information for creating a connection to a {@link DataStore} in read and/or write mode.
 * {@code StorageConnector} wraps an input {@link Object}, which can be any of the following types:
 *
 * <ul>
 *   <li>A {@link java.io.File} for a file or a directory.</li>
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
 * @since   0.3
 * @version 0.3
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
     * The options, created only when first needed.
     *
     * @see #getOption(OptionKey)
     * @see #setOption(OptionKey, Object)
     */
    private transient Map<OptionKey<?>, Object> options;

    /**
     * Creates a new data store connection wrapping the given input/output object.
     * The object can be of any type, but the class javadoc lists the most typical ones.
     *
     * @param storage The input/output object as a URL, file, image input stream, <i>etc.</i>.
     */
    public StorageConnector(final Object storage) {
        ArgumentChecks.ensureNonNull("storage", storage);
        this.storage = storage;
    }

    /**
     * Returns the option value for the given key, or {@code null} if none.
     *
     * @param  <T> The type of option value.
     * @param  key The option for which to get the value.
     * @return The current value for the given option, or {@code null} if none.
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
     * @param <T>   The type of option value.
     * @param key   The option for which to set the value.
     * @param value The new value for the given option, or {@code null} for removing the value.
     */
    public <T> void setOption(final OptionKey<T> key, final T value) {
        ArgumentChecks.ensureNonNull("key", key);
        options = key.setValueInto(options, value);
    }

    /**
     * Returns the input/output object given at construction time.
     * The object can be of any type, but the class javadoc lists the most typical ones.
     *
     * @return The input/output object as a URL, file, image input stream, <i>etc.</i>.
     *
     * @see #getStorageAs(Class)
     */
    public Object getStorage() {
        return storage;
    }

    /**
     * Returns a short name of the input/output object. The default implementation performs
     * the following choices based on the type of the {@linkplain #getStorage() storage} object:
     *
     * <ul>
     *   <li>For {@link java.io.File}, {@link java.net.URI} or {@link java.net.URL}
     *       instances, this method uses dedicated API.</li>
     *   <li>For {@link CharSequence} instances, this method gets a string representation of the storage object
     *       and returns the part after the last {@code '/'} character or platform-dependent name separator.</li>
     *   <li>For instances of unknown type, this method builds a string representation using the class name.
     *       Note that the string representation of unknown types may change in any future SIS version.</li>
     * </ul>
     *
     * @return A short name of the storage object.
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
     *   <li>For {@link java.io.File}, {@link java.net.URI}, {@link java.net.URL} or
     *       {@link CharSequence} instances, this method returns the string after the last {@code '.'} character
     *       in the filename, provided that the {@code '.'} is not the first filename character. This may be an
     *       empty string if the filename has no extension, but never {@code null}.</li>
     *   <li>For instances of unknown type, this method returns {@code null}.</li>
     * </ul>
     *
     * @return The filename extension, or an empty string if none, or {@code null} if the storage
     *         is an object of unknown type.
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
     *       <li>If the {@linkplain #getStorage() storage} object is an instance of the
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
     *       <li>Otherwise if the input is an instance of {@link java.io.File},
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
     * @param  <T>  The compile-time type of the {@code type} argument.
     * @param  type The desired type as one of {@code ByteBuffer}, {@code DataInput}, {@code Connection}
     *         class or other type supported by {@code StorageConnector} subclasses.
     * @return The storage as a view of the given type, or {@code null} if no view can be created for the given type.
     * @throws IllegalArgumentException if the given {@code type} argument is not a supported type.
     * @throws DataStoreException if an error occurred while opening a stream or database connection.
     *
     * @see #getStorage()
     * @see #closeAllExcept(Object)
     */
    public <T> T getStorageAs(final Class<T> type) throws IllegalArgumentException, DataStoreException {
        ArgumentChecks.ensureNonNull("type", type);
        if (views != null) {
            final Object view = views.get(type);
            if (view != null) {
                if (view == storage && view instanceof InputStream) try {
                    resetInputStream();
                } catch (IOException e) {
                    throw new DataStoreException(Errors.format(Errors.Keys.CanNotRead_1, getStorageName()), e);
                }
                return (view != Void.TYPE) ? type.cast(view) : null;
            }
        } else {
            views = new IdentityHashMap<Class<?>, Object>();
        }
        /*
         * Special case for DataInput and ByteBuffer, because those values are created together.
         * In addition, ImageInputStream creation assigns a value to the 'streamOrigin' field.
         * The ChannelDataInput case is an undocumented (SIS internal) type for avoiding the
         * potential call to ImageIO.createImageInputStream(…) when we do not need it.
         */
        boolean done = false;
        try {
            if (type == ByteBuffer.class) {
                createByteBuffer();
                done = true;
            } else if (type == DataInput.class) {
                createDataInput();
                done = true;
            } else if (type == ChannelDataInput.class) {                // Undocumented case (SIS internal)
                createChannelDataInput(false);
                done = true;
            }
        } catch (IOException e) {
            throw new DataStoreException(Errors.format(Errors.Keys.CanNotOpen_1, getStorageName()), e);
        }
        if (done) {
            // Want to exit this method even if the value is null.
            return getView(type);
        }
        /*
         * All other cases.
         */
        final Object value;
        try {
            value = createView(type);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new DataStoreException(Errors.format(Errors.Keys.CanNotOpen_1, getStorageName()), e);
        }
        final T view = type.cast(value);
        addView(type, view);
        return view;
    }

    /**
     * Assuming that {@link #storage} is an instance of {@link InputStream}, resets its position. This method
     * is the converse of the marks performed at the beginning of {@link #createChannelDataInput(boolean)}.
     */
    private void resetInputStream() throws IOException {
        final ChannelDataInput channel = getView(ChannelDataInput.class);
        if (channel != null) {
            ((InputStream) storage).reset();        // May throw an exception if mark is unsupported.
            channel.buffer.limit(0);                // Must be after storage.reset().
            channel.setStreamPosition(0);           // Must be after buffer.limit(0).
        }
    }

    /**
     * Creates a view for the input as a {@link ChannelDataInput} if possible.
     * If the view can not be created, remember that fact in order to avoid new attempts.
     *
     * @param  asImageInputStream If the {@code ChannelDataInput} needs to be {@link ChannelImageInputStream} subclass.
     * @throws IOException if an error occurred while opening a channel for the input.
     */
    private void createChannelDataInput(final boolean asImageInputStream) throws IOException {
        /*
         * Before to try to open an InputStream, mark its position so we can rewind if the user asks for
         * the InputStream directly. We need to reset because ChannelDataInput may have read some bytes.
         * Note that if mark is unsupported, the default InputStream.mark() implementation does nothing.
         * See above 'resetInputStream()' method.
         */
        if (storage instanceof InputStream) {
            ((InputStream) storage).mark(DEFAULT_BUFFER_SIZE);
        }
        /*
         * Following method call recognizes ReadableByteChannel, InputStream (with special case for FileInputStream),
         * URL, URI, File, Path or other types that may be added in future SIS versions.
         */
        final ReadableByteChannel channel = IOUtilities.open(storage,
                getOption(OptionKey.URL_ENCODING), getOption(OptionKey.OPEN_OPTIONS));

        ChannelDataInput asDataInput = null;
        if (channel != null) {
            addViewToClose(channel, storage);
            ByteBuffer buffer = getOption(OptionKey.BYTE_BUFFER);
            if (buffer == null) {
                buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
                // TODO: we do not create direct buffer yet, but this is something
                // we may want to consider in a future SIS version.
            }
            final String name = getStorageName();
            if (asImageInputStream) {
                asDataInput = new ChannelImageInputStream(name, channel, buffer, false);
            } else {
                asDataInput = new ChannelDataInput(name, channel, buffer, false);
            }
            addViewToClose(asDataInput, channel);
        }
        addView(ChannelDataInput.class, asDataInput);
    }

    /**
     * Creates a view for the input as a {@link DataInput} if possible. This method performs the choice
     * documented in the {@link #getStorageAs(Class)} method for the {@code DataInput} case. Opening the
     * data input may imply creating a {@link ByteBuffer}, in which case the buffer will be stored under
     * the {@code ByteBuffer.class} key together with the {@code DataInput.class} case.
     *
     * @throws IOException if an error occurred while opening a stream for the input.
     */
    private void createDataInput() throws IOException {
        final DataInput asDataInput;
        if (storage instanceof DataInput) {
            asDataInput = (DataInput) storage;
        } else {
            /*
             * Creates a ChannelImageInputStream instance. We really need that specific type because some
             * SIS data stores will want to access directly the channel and the buffer. We will fallback
             * on the ImageIO.createImageInputStream(Object) method only in last resort.
             */
            if (!views.containsKey(ChannelDataInput.class)) {
                createChannelDataInput(true);
            }
            final ChannelDataInput c = getView(ChannelDataInput.class);
            if (c == null) {
                asDataInput = ImageIO.createImageInputStream(storage);
                addViewToClose(asDataInput, storage);
            } else if (c instanceof DataInput) {
                asDataInput = (DataInput) c;
                // No call to 'addViewToClose' because the instance already exists.
            } else {
                asDataInput = new ChannelImageInputStream(c);
                if (views.put(ChannelDataInput.class, asDataInput) != c) {          // Replace the previous instance.
                    throw new ConcurrentModificationException();
                }
                addViewToClose(asDataInput, c.channel);
            }
        }
        addView(DataInput.class, asDataInput);
    }

    /**
     * Creates a {@link ByteBuffer} from the {@link ChannelDataInput} if possible, or from the
     * {@link ImageInputStream} otherwise. The buffer will be initialized with an arbitrary amount
     * of bytes read from the input. This amount is not sufficient, it can be increased by a call
     * to {@link #prefetch()}.
     *
     * @throws IOException if an error occurred while opening a stream for the input.
     */
    private void createByteBuffer() throws IOException, DataStoreException {
        /*
         * First, try to create the ChannelDataInput if it does not already exists.
         * If successful, this will create a ByteBuffer companion as a side effect.
         */
        if (!views.containsKey(ChannelDataInput.class)) {
            createChannelDataInput(false);
        }
        ByteBuffer asByteBuffer = null;
        final ChannelDataInput c = getView(ChannelDataInput.class);
        if (c != null) {
            asByteBuffer = c.buffer.asReadOnlyBuffer();
        } else {
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
                    asByteBuffer = ByteBuffer.wrap(buffer).order(in.getByteOrder());
                    asByteBuffer.limit(n);
                    // Can't invoke asReadOnly() because 'prefetch()' need to be able to write in it.
                }
            }
        }
        addView(ByteBuffer.class, asByteBuffer);
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
                return c.prefetch() >= 0;
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
                    final int n = input.read(buffer.array(), p, buffer.capacity() - p);
                    if (n >= 0) {
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
     * Creates a storage view of the given type if possible, or returns {@code null} otherwise.
     * This method is invoked by {@link #getStorageAs(Class)} when first needed, and the result is cached.
     *
     * @param  type The type of the view to create.
     * @return The storage as a view of the given type, or {@code null} if no view can be created for the given type.
     * @throws IllegalArgumentException if the given {@code type} argument is not a supported type.
     * @throws Exception if an error occurred while opening a stream or database connection.
     */
    private Object createView(final Class<?> type) throws IllegalArgumentException, Exception {
        if (type == String.class) {
            return IOUtilities.toString(storage);
        }
        if (type == Connection.class) {
            if (storage instanceof Connection) {
                return storage;
            }
            if (storage instanceof DataSource) {
                final Connection c = ((DataSource) storage).getConnection();
                addViewToClose(c, storage);
                return c;
            }
            return null;
        }
        if (type == ImageInputStream.class) {
            final DataInput input = getStorageAs(DataInput.class);
            return (input instanceof ImageInputStream) ? input : null;
        }
        /*
         * If the user asked an InputStream, we may return the storage as-is if it was already an InputStream.
         * However before doing so, we may need to reset the InputStream position if the stream has been used
         * by a ChannelDataInput.
         */
        if (type == InputStream.class) {
            if (storage instanceof InputStream) {
                resetInputStream();
                return storage;
            }
            final DataInput input = getStorageAs(DataInput.class);
            if (input instanceof InputStream) {
                return input;
            }
            if (input instanceof ImageInputStream) {
                final InputStream c = new InputStreamAdapter((ImageInputStream) input);
                addViewToClose(c, input);
                return c;
            }
            return null;
        }
        if (type == Reader.class) {
            if (storage instanceof Reader) {
                return storage;
            }
            final InputStream input = getStorageAs(InputStream.class);
            if (input != null) {
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
                 *    the InputStream. For now we let the InputStreamReader.mark() to throws an IOException,
                 *    but we may need to provide our own subclass of BufferedReader in a future SIS version
                 *    if mark/reset support is needed here.
                 */
                addViewToClose(c, input);
                return c;
            }
            return null;
        }
        return ObjectConverters.convert(storage, type);
    }

    /**
     * Adds the given view in the cache.
     *
     * @param <T>   The compile-time type of the {@code type} argument.
     * @param type  The view type.
     * @param view  The view, or {@code null} if none.
     */
    private <T> void addView(final Class<T> type, final T view) {
        if (views.put(type, (view != null) ? view : Void.TYPE) != null) {
            throw new ConcurrentModificationException();
        }
    }

    /**
     * Returns the view for the given type from the cache.
     *
     * @param <T>   The compile-time type of the {@code type} argument.
     * @param type  The view type.
     * @return      The view, or {@code null} if none.
     */
    private <T> T getView(final Class<T> type) {
        final Object view = views.get(type);
        return (view != Void.TYPE) ? type.cast(view) : null;
    }

    /**
     * Declares that the given {@code input} will need to be closed by the {@link #closeAllExcept(Object)} method.
     * The {@code input} argument is always a new instance wrapping, directly or indirectly, the {@link #storage}.
     * Callers must specify the wrapped object in the {@code delegate} argument.
     *
     * @param input    The newly created object which will need to be closed.
     * @param delegate The object wrapped by the given {@code input}.
     */
    private void addViewToClose(final Object input, final Object delegate) {
        if (viewsToClose == null) {
            viewsToClose = new IdentityHashMap<Object,Object>(4);
        }
        if (viewsToClose.put(input, delegate) != null) {
            throw new AssertionError(input);
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
     * @param  view The view to leave open, or {@code null} if none.
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
            if (storage != view && JDK7.isAutoCloseable(storage)) try {
                JDK7.close(storage);
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
            final Queue<Object> deferred = new LinkedList<Object>();
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
            if (JDK7.isAutoCloseable(c)) try {
                JDK7.close(c);
            } catch (Exception e) {
                if (failure == null) {
                    failure = new DataStoreException(e);
                } else {
                    // failure.addSuppressed(e) on the JDK7 branch.
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
