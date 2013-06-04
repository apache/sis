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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.ConcurrentModificationException;
import java.io.DataInput;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.sql.Connection;
import javax.sql.DataSource;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.storage.IOUtilities;
import org.apache.sis.internal.storage.ChannelImageInputStream;
import org.apache.sis.setup.OptionKey;


/**
 * Information for creating a connection to a {@link DataStore} in read and/or write mode.
 * {@code DataStoreConnection} wraps an input {@link Object}, which can be any of the following types:
 *
 * <ul>
 *   <li>A {@link java.nio.file.Path} or a {@link java.io.File} or file or a directory in a file system.</li>
 *   <li>A {@link java.net.URI} or a {@link java.net.URL} to a distant resource.</li>
 *   <li>A {@link CharSequence} interpreted as a filename or a URL.</li>
 *   <li>A {@link java.nio.channels.Channel} or a {@link DataInput}.</li>
 *   <li>A {@link DataSource} or a {@link Connection} to a JDBC database.</li>
 *   <li>Any other {@code DataStore}-specific object, for example {@link ucar.nc2.NetcdfFile}.</li>
 * </ul>
 *
 * The {@link #getStorageAs(Class)} method provides the storage as an object of the given type, opening
 * the input stream if necessary. This class tries to open the stream only once - subsequent invocation
 * of {@code getStorageAs(…)} may return the same input stream.
 *
 * <p>This class is used only for discovery of a {@code DataStore} implementation capable to handle the input.
 * Once a suitable {@code DataStore} has been found, the {@code DataStoreConnection} instance is typically
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
public class DataStoreConnection implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2524083964906593093L;

    /**
     * The default size of the {@link ByteBuffer} to be created.
     * Users can override this value by providing a value for {@link OptionKey#BYTE_BUFFER}.
     */
    private static final int DEFAULT_BUFFER_SIZE = 4096;

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
     *   <li>{@link Connection}:
     *       The input/output object as a JDBC connection.</li>
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
    public DataStoreConnection(final Object storage) {
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
     * Sets the option value for the given key. The default implementation recognizes the given options:
     *
     * <ul>
     *   <li>{@link OptionKey#URL_ENCODING} for converting URL to URI or filename, if needed.</li>
     *   <li>{@link OptionKey#BYTE_BUFFER} for allowing users to control the byte buffer to be created.</li>
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
     *   <li>For {@link java.nio.file.Path}, {@link java.io.File}, {@link java.net.URI} or {@link java.net.URL}
     *       instances, this method uses dedicated API like {@link Path#getFileName()}.</li>
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
     *   <li>For {@link java.nio.file.Path}, {@link java.io.File}, {@link java.net.URI}, {@link java.net.URL} or
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
     *       <li>If the {@linkplain #getStorage() storage} object is already an instance of {@link DataInput}
     *           (including the {@link ImageInputStream} and {@link javax.imageio.stream.ImageOutputStream} types),
     *           then it is returned unchanged.</li>
     *
     *       <li>Otherwise if the input is an instance of {@link java.nio.file.Path}, {@link java.io.File},
     *           {@link java.net.URI}, {@link java.net.URL}, {@link CharSequence}, {@link java.io.InputStream} or
     *           {@link java.nio.channels.ReadableByteChannel}, then an {@link ImageInputStream} backed by a
     *           {@link ByteBuffer} is created when first needed and returned.</li>
     *
     *       <li>Otherwise if {@link ImageIO#createImageInputStream(Object)} returns a non-null value,
     *           then this value is cached and returned.</li>
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
     * Multiple invocations of this method on the same {@code DataStoreConnection} instance will try
     * to return the same instance on a <cite>best effort</cite> basis. Consequently, implementations
     * of {@link DataStoreProvider#canRead(DataStoreConnection)} methods shall not close the stream or
     * database connection returned by this method. In addition, those {@code canRead(DataStoreConnection)}
     * methods are responsible for restoring the stream or byte buffer to its original position on return.
     *
     * @param  <T>  The compile-time type of the {@code type} argument.
     * @param  type The desired type as one of {@code ByteBuffer}, {@code DataInput}, {@code Connection}
     *         class or other type supported by {@code DataStoreConnection} subclasses.
     * @return The storage as a view of the given type, or {@code null} if no view can be created for the given type.
     * @throws IllegalArgumentException If the given {@code type} argument is not a known type.
     * @throws DataStoreException If an error occurred while opening a stream or database connection.
     *
     * @see #getStorage()
     * @see #closeAllExcept(Object)
     */
    public <T> T getStorageAs(final Class<T> type) throws IllegalArgumentException, DataStoreException {
        ArgumentChecks.ensureNonNull("type", type);
        if (views != null) {
            final Object view = views.get(type);
            if (view != null) {
                return (view != Void.TYPE) ? type.cast(view) : null;
            }
        } else {
            views = new IdentityHashMap<>();
        }
        /*
         * Special case for DataInput and ByteBuffer, because those values are created together.
         * In addition, ImageInputStream creation assigns a value to the 'streamOrigin' field.
         */
        if (type == DataInput.class || type == ByteBuffer.class) {
            try {
                createDataInput();
            } catch (IOException e) {
                throw new DataStoreException(Errors.format(Errors.Keys.CanNotOpen_1, getStorageName()), e);
            }
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
     * Creates a view for the input as a {@link DataInput} if possible. This method performs the choice
     * documented in the {@link #getStorageAs(Class)} method for the {@code DataInput} case. Opening the
     * data input may imply creating a {@link ByteBuffer}, in which case the buffer will be stored under
     * the {@code ByteBuffer.class} key together with the {@code DataInput.class} case.
     *
     * @throws IOException If an error occurred while opening a stream for the input.
     */
    private void createDataInput() throws IOException {
        final DataInput asDataInput;
        ByteBuffer asByteBuffer = null;
        if (storage instanceof DataInput) {
            asDataInput = (DataInput) storage;
        } else {
            /*
             * Creates a ChannelImageInputStream instance. We really need that specific type because some
             * SIS data stores will want to access directly the channel and the buffer. We will fallback
             * on the ImageIO.createImageInputStream(Object) method only in last resort.
             */
            final ReadableByteChannel channel = IOUtilities.open(storage, getOption(OptionKey.URL_ENCODING));
            if (channel != null) {
                ByteBuffer buffer = getOption(OptionKey.BYTE_BUFFER);
                if (buffer == null) {
                    buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
                    // TODO: we do not create direct buffer yet, but this is something
                    // we may want to consider in a future SIS version.
                }
                asDataInput = new ChannelImageInputStream(getStorageName(), channel, buffer, false);
                asByteBuffer = buffer.asReadOnlyBuffer();
            } else {
                asDataInput = ImageIO.createImageInputStream(storage);
            }
        }
        /*
         * If an ImageInputStream has been created without buffer, read an arbitrary amount of bytes now
         * so we can provide a ByteBuffer in case some user wants it. This may be a unnecessary overhead
         * since maybe no user will want to look at a ByteBuffer,  but this case is presumed rare enough
         * for not being worth a more complex "lazy instantiation" mechanism. We read only a small amount
         * of bytes because, at the contrary of the buffer created in the above block, the buffer created
         * here is unlikely to be used for the reading process after the recognition of the file format.
         */
        if (asByteBuffer == null && asDataInput instanceof ImageInputStream) {
            final ImageInputStream in = (ImageInputStream) asDataInput;
            in.mark();
            final byte[] buffer = new byte[256];
            final int n = in.read(buffer);
            in.reset();
            if (n >= 1) {
                asByteBuffer = ByteBuffer.wrap(ArraysExt.resize(buffer, n))
                        .asReadOnlyBuffer().order(in.getByteOrder());
            }
        }
        addView(ByteBuffer.class, asByteBuffer);
        addView(DataInput.class,  asDataInput);
    }

    /**
     * Creates a storage view of the given type if possible, or returns {@code null} otherwise.
     * This method is invoked by {@link #getStorageAs(Class)} when first needed, and the result is cached.
     *
     * @param  <T>  The compile-time type of the {@code type} argument.
     * @param  type The type of the view to create.
     * @return The storage as a view of the given type, or {@code null} if no view can be created for the given type.
     * @throws IllegalArgumentException If the given {@code type} argument is not a known type.
     * @throws Exception If an error occurred while opening a stream or database connection.
     */
    private Object createView(final Class<?> type) throws IllegalArgumentException, Exception {
        if (type == Connection.class) {
            if (storage instanceof Connection) {
                return storage;
            } else if (storage instanceof DataSource) {
                return ((DataSource) storage).getConnection();
            }
        }
        throw new IllegalArgumentException(Errors.format(Errors.Keys.UnknownType_1, type));
    }

    /**
     * Adds the given view in the caches.
     *
     * @param <T>   The compile-time type of the {@code type} argument.
     * @param type  The view type.
     * @param view  The view, or {@code null} if none.
     */
    private <T> void addView(final Class<T> type, final T view) {
        if (views.put(type, (view != null) ? view : Void.TYPE) != null) {
            throw new ConcurrentModificationException();
        }
    }

    /**
     * Returns the view for the given type from the caches.
     *
     * @param <T>   The compile-time type of the {@code type} argument.
     * @param type  The view type.
     * @return      The view, or {@code null} if none.
     */
    private <T> T getView(final Class<T> type) {
        final Object view = views.get(type);
        return (view != Void.TYPE) ? type.cast(view) : null;
    }

    /**
     * Closes all streams and connections created by this {@code DataStoreConnection} except the given view.
     * This method closes all objects created by the {@link #getStorageAs(Class)} method except the given {@code view}.
     * If {@code view} is {@code null}, then this method closes everything including the {@linkplain #getStorage()
     * storage} if it is closeable.
     *
     * <p>This method is invoked when a suitable {@link DataStore} has been found - in which case the view used
     * by the data store is given in argument to this method - or when no suitable {@code DataStore} has been
     * found - in which case the {@code view} argument is null.</p>
     *
     * <p>This {@code DataStoreConnection} instance shall not be used anymore after invocation of this method.</p>
     *
     * @param  view The view to leave open, or {@code null} if none.
     * @throws DataStoreException If an error occurred while closing the stream or database connection.
     *
     * @see #getStorageAs(Class)
     */
    public void closeAllExcept(final Object view) throws DataStoreException {
        boolean close = (view == null);
        try {
            for (final Object value : views.values()) {
                if (value != view) {
                    if (value instanceof AutoCloseable) {
                        ((AutoCloseable) value).close();
                        close = false;
                    }
                }
                /*
                 * On JDK6, ImageInputStream does not extend Closeable and must
                 * be checked explicitely. On JDK7, this is not needed anymore.
                 * Likewise, Connection extends AutoCloseable only in JDK7.
                 */
            }
            if (close && storage instanceof AutoCloseable) {
                /*
                 * Close only if we didn't closed a view because closing an input stream view
                 * automatically close the 'storage' if the former is a wrapper for the later.
                 * Since AutoCloseable.close() is not guaranteed to be indempotent, we should
                 * avoid to call it (indirectly) twice.
                 */
                ((AutoCloseable) storage).close();
            }
        } catch (Exception e) {
            throw new DataStoreException(e);
        } finally {
            views = Collections.emptyMap();
        }
    }

    /**
     * Returns a string representation of this {@code DataStoreConnection} for debugging purpose.
     */
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
