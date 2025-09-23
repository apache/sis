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
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.net.URI;
import java.net.URL;
import java.io.File;
import java.io.Reader;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.OpenOption;
import java.nio.file.NoSuchFileException;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.DefaultTreeTable;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.storage.internal.InputStreamAdapter;
import org.apache.sis.storage.internal.RewindableLineReader;
import org.apache.sis.storage.base.StoreUtilities;
import org.apache.sis.io.InvalidSeekException;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.io.stream.ChannelFactory;
import org.apache.sis.io.stream.ChannelData;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.io.stream.ChannelDataOutput;
import org.apache.sis.io.stream.ChannelImageInputStream;
import org.apache.sis.io.stream.ChannelImageOutputStream;
import org.apache.sis.io.stream.InternalOptionKey;
import org.apache.sis.system.Configuration;
import org.apache.sis.setup.OptionKey;


/**
 * Information for creating a connection to a {@link DataStore} in read and/or write mode.
 * {@code StorageConnector} wraps an input {@link Object}, which can be any of the following types:
 *
 * <ul>
 *   <li>A {@link Path} or a {@link File} for a file or a directory.</li>
 *   <li>A {@link URI} or a {@link URL} to a distant resource.</li>
 *   <li>A {@link CharSequence} interpreted as a filename or a URL.</li>
 *   <li>A {@link Channel}, {@link DataInput}, {@link InputStream} or {@link Reader}.</li>
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
 * <h2>Limitations</h2>
 * This class is not thread-safe.
 * Not only {@code StorageConnector} should be used by a single thread,
 * but the objects returned by {@link #getStorageAs(Class)} should also be used by the same thread.
 *
 * <p>Instances of this class are serializable if the {@code storage} object given at construction time
 * is serializable.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.5
 * @since   0.3
 */
public class StorageConnector implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2524083964906593093L;

    /**
     * The default size (in bytes) of {@link ByteBuffer}s created by storage connectors.
     * Those buffers are typically created when the specified storage object is a
     * {@link File}, {@link Path}, {@link URL} or {@link URI}.
     *
     * <p>The default buffer size is arbitrary and may change in any future Apache SIS version.
     * The current value is {@value}. Users can override this value by providing a pre-allocated
     * buffer with {@link OptionKey#BYTE_BUFFER}.</p>
     *
     * @see OptionKey#BYTE_BUFFER
     *
     * @since 1.4
     */
    @Configuration
    public static final int DEFAULT_BUFFER_SIZE = 32 * 1024;

    /**
     * The read-ahead limit for mark operations before probing input streams.
     * When the storage is an {@link InputStream} or a {@link Reader}, the {@link #getStorageAs(Class)} method
     * marks the current position with this "read ahead limit" value for resetting the stream to its original
     * position if the caller does not recognize the stream content.
     * Implementations of the {@link DataStoreProvider#probeContent(StorageConnector)} method should avoid
     * reading more bytes or characters from an {@code InputStream} or from a {@code Reader} than this value.
     *
     * <p>The read ahead limit is arbitrary and may change in any future Apache SIS version.
     * However it is guaranteed to be smaller than or equal to {@link #DEFAULT_BUFFER_SIZE}.
     * The current value is {@value}.</p>
     *
     * <div class="note"><b>Note for maintainer:</b>
     * this value should not be greater than the {@link BufferedInputStream} default buffer size.</div>
     *
     * @see InputStream#mark(int)
     * @see Reader#mark(int)
     *
     * @since 1.5
     */
    @Configuration
    public static final int READ_AHEAD_LIMIT = 8 * 1024;

    /**
     * The minimal size of the {@link ByteBuffer} to be created. This size is used only
     * for temporary buffers that are unlikely to be used for the actual reading process.
     */
    static final int MINIMAL_BUFFER_SIZE = 256;

    /**
     * A flag for <code>{@linkplain #addView(Class, Object, Class, byte) addView}(…, view, source, flags)</code>
     * telling that after closing the {@code view}, we also need to close the {@code source}.
     * This flag should be set when the view is an {@link ImageInputStream} because Java I/O
     * {@link javax.imageio.stream.FileCacheImageInputStream#close()} does not close the underlying stream.
     * For most other kinds of view, this flag should not be set.
     *
     * @see Coupled#cascadeOnClose()
     */
    private static final byte CASCADE_ON_CLOSE = 1;

    /**
     * A flag for <code>{@linkplain #addView(Class, Object, Class, byte) addView}(…, view, source, flags)</code>
     * telling that before resetting the {@code view}, we need to reset the {@code source} first. This flag can
     * be unset if any change in the position of {@code view} is immediately reflected in the position of
     * {@code source}, and vice-versa.
     *
     * @see Coupled#cascadeOnReset()
     */
    private static final byte CASCADE_ON_RESET = 2;

    /**
     * A flag for <code>{@linkplain #addView(Class, Object, Class, byte) addView}(…, view, source, flags)</code>
     * telling that {@code view} cannot be reset, so it should be set to {@code null} instead. This implies that
     * a new view of the same type will be recreated next time it will be requested.
     *
     * <p>When this flag is set, the {@link #CASCADE_ON_RESET} should usually be set at the same time.</p>
     */
    private static final byte CLEAR_ON_RESET = 4;

    /**
     * Handler to {@code StorageConnector.createFoo()} methods associated to given storage types.
     * Each {@code createFoo()} method may be invoked once for opening an input stream, character
     * reader, database connection, <i>etc</i> from user supplied path, URI, <i>etc</i>.
     *
     * @param  <S>  the type of input (source) created by this {@code Opener} instance.
     */
    @FunctionalInterface
    private interface Opener<S> {
        /**
         * Invoked when first needed for creating an input of the requested type.
         * This method should invoke {@link #addView(Class, Object, Class, byte)}
         * for caching the result before to return the view.
         */
        S open(StorageConnector c) throws Exception;
    }

    /** Helper method for {@link #OPENERS} static initialization. */
    private static <S> void add(final Class<S> type, final Opener<S> op) {
        if (OPENERS.put(type, op) != null) throw new AssertionError(type);
    }

    /**
     * List of types recognized by {@link #getStorageAs(Class)}, associated to the methods for opening stream
     * of those types. This map shall contain every types documented in {@link #getStorageAs(Class)} javadoc.
     * {@code null} values means to use {@link ObjectConverters} for that particular type.
     */
    private static final Map<Class<?>, Opener<?>> OPENERS = new IdentityHashMap<>(16);
    static {
        add(String.class,                  StorageConnector::createString);
        add(ByteBuffer.class,              StorageConnector::createByteBuffer);
        add(DataInput.class,               StorageConnector::createDataInput);
        add(DataOutput.class,              StorageConnector::createDataOutput);
        add(ImageInputStream.class,        StorageConnector::createImageInputStream);
        add(ImageOutputStream.class,       StorageConnector::createImageOutputStream);
        add(InputStream.class,             StorageConnector::createInputStream);
        add(OutputStream.class,            StorageConnector::createOutputStream);
        add(Reader.class,                  StorageConnector::createReader);
        add(DataSource.class,              StorageConnector::createDataSource);
        add(Connection.class,              StorageConnector::createConnection);
        add(ChannelDataInput.class,        StorageConnector::createChannelDataInput);         // Undocumented case (SIS internal)
        add(ChannelDataOutput.class,       StorageConnector::createChannelDataOutput);        // Undocumented case (SIS internal)
        add(ChannelImageInputStream.class, StorageConnector::createChannelImageInputStream);  // Undocumented case (SIS internal)
        add(ChannelFactory.class,          (s) -> null);                                      // Undocumented. Shall not cache.
        /*
         * ChannelFactory may have been created as a side effect of creating a ReadableByteChannel.
         * Caller should have asked for another type (e.g. InputStream) before to ask for that type.
         * Consequently, null value for ChannelFactory shall not be cached since the actual value may
         * be computed later.
         *
         * Following classes will be converted using ObjectConverters, but without throwing an
         * exception if the conversion fail. Instead, getStorageAs(Class) will return null.
         * Classes not listed here will let the UnconvertibleObjectException propagates.
         */
        add(URI.class,  null);
        add(URL.class,  null);
        add(File.class, null);
        add(Path.class, null);
    }

    /**
     * The input/output object given at construction time.
     *
     * @see #getStorage()
     */
    @SuppressWarnings("serial")         // Not statically typed as Serializable.
    final Object storage;

    /**
     * A name for the input/output object, or {@code null} if none.
     * This field is initialized only when first needed.
     *
     * @see #getStorageName()
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
    @SuppressWarnings("serial")         // Not statically typed as Serializable.
    private Map<OptionKey<?>, Object> options;

    /**
     * If a probing operation is ongoing, the provider doing the operation. Otherwise {@code null}.
     * This information is needed because if the storage is a file and that file does not exist,
     * then the {@code StorageConnector} behavior depends on whether the caller is probing or not.
     * If probing, {@link ProbeResult#SUPPORTED} should be returned without creating the file,
     * because attempt to probe that file would cause an {@link java.io.EOFException}.
     * If not probing, then an empty file should be created.
     *
     * @see #wasProbingAbsentFile()
     */
    transient ProbeProviderPair probing;

    /**
     * Views of {@link #storage} as instances of types different than the type of the object given to the constructor.
     * The {@code null} reference can appear in various places:
     *
     * <ul>
     *   <li>A non-existent entry (equivalent to an entry associated to the {@code null} value) means that the value
     *       has not yet been computed.</li>
     *   <li>A {@linkplain Coupled#isValid valid entry} with {@link Coupled#view} set to {@code null} means the value
     *       has been computed and we have determined that {@link #getStorageAs(Class)} shall return {@code null} for
     *       that type.</li>
     *   <li>By convention, the {@code null} key is associated to the {@link #storage} value.</li>
     * </ul>
     *
     * An empty map means that this {@code StorageConnector} has been closed.
     *
     * @see #addView(Class, Object, Class, byte)
     * @see #getView(Class)
     * @see #getStorageAs(Class)
     */
    private transient Map<Class<?>, Coupled> views;

    /**
     * Wraps an instance of @link InputStream}, {@link DataInput}, {@link Reader}, <i>etc.</i> together with additional
     * information about other objects that are coupled with the wrapped object. For example if a {@link Reader} is a
     * wrapper around the user supplied {@link InputStream}, then those two objects will be wrapped in {@code Coupled}
     * instances together with information about how they are related
     *
     * One purpose of {@code Coupled} information is to keep trace of objects which will need to be closed by the
     * {@link StorageConnector#closeAllExcept(Object)} method  (for example an {@link InputStreamReader} wrapping
     * an {@link InputStream}).
     *
     * Another purpose is to determine which views need to be synchronized if {@link StorageConnector#storage} is
     * used independently. They are views that may advance {@code storage} position, but not at the same time as the
     * {@link #view} position (typically because the view reads some bytes in advance and stores them in a buffer).
     * Such coupling may occur when the storage is an {@link InputStream}, an {@link java.io.OutputStream} or a
     * {@link Channel}. The coupled {@link #view} can be:
     *
     * <ul>
     *   <li>{@link Reader} that are wrappers around {@code InputStream}.</li>
     *   <li>{@link ChannelDataInput} when the channel come from an {@code InputStream}.</li>
     *   <li>{@link ChannelDataInput} when the channel has been explicitly given to the constructor.</li>
     * </ul>
     */
    private static final class Coupled {
        /**
         * The {@link StorageConnector#storage} viewed as another kind of object.
         * Supported types are:
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
         *       Same as {@code DataInput} if it can be cast, or {@code null} otherwise.</li>
         *
         *   <li>{@link InputStream}:
         *       If not explicitly provided, this is a wrapper around the above {@link ImageInputStream}.</li>
         *
         *   <li>{@link Reader}:
         *       If not explicitly provided, this is a wrapper around the above {@link InputStream}.</li>
         *
         *   <li>{@link Connection}:
         *       The storage object as a JDBC connection.</li>
         * </ul>
         */
        Object view;

        /**
         * The object that {@link #view} is wrapping. For example if {@code view} is an {@link InputStreamReader},
         * then {@code wrapperFor.view} is an {@link InputStream}. This field is {@code null} if {@link #view} ==
         * {@link StorageConnector#storage}.
         */
        final Coupled wrapperFor;

        /**
         * The other views that are consuming {@link #view}, or {@code null} if none. For each element in this array,
         * {@code wrappedBy[i].wrapperFor == this}.
         */
        private Coupled[] wrappedBy;

        /**
         * Bitwise combination of {@link #CASCADE_ON_CLOSE}, {@link #CASCADE_ON_RESET} or {@link #CLEAR_ON_RESET}.
         */
        final byte cascade;

        /**
         * {@code true} if the position of {@link #view} is synchronized with the position of {@link #wrapperFor}.
         */
        boolean isValid;

        /**
         * Creates a wrapper for {@link StorageConnector#storage}. This constructor is used when we need to create
         * a {@code Coupled} instance for another view wrapping {@code storage}.
         */
        Coupled(final Object storage) {
            view       = storage;
            wrapperFor = null;
            cascade    = 0;
            isValid    = true;
        }

        /**
         * Creates a wrapper for a view wrapping the given {@code Coupled} instance.
         * Caller is responsible to set the {@link #view} field after this constructor call.
         *
         * @param  wrapperFor  the object that {@link #view} will wrap, or {@code null} if none.
         * @param  cascade     bitwise combination of {@link #CASCADE_ON_CLOSE}, {@link #CASCADE_ON_RESET}
         *                     or {@link #CLEAR_ON_RESET}.
         */
        Coupled(final Coupled wrapperFor, final byte cascade) {
            this.wrapperFor = wrapperFor;
            this.cascade    = cascade;
            if (wrapperFor != null) {
                final Coupled[] w = wrapperFor.wrappedBy;
                final int n = (w != null) ? w.length : 0;
                final Coupled[] e = new Coupled[n + 1];
                if (n != 0) System.arraycopy(w, 0, e, 0, n);
                e[n] = this;
                wrapperFor.wrappedBy = e;
            }
        }

        /**
         * {@code true} if after closing the {@link #view}, we need to also close the {@link #wrapperFor}.
         * Should be {@code true} when the view is an {@link ImageInputStream} because Java I/O
         * {@link javax.imageio.stream.FileCacheImageInputStream#close()} does not close the underlying stream.
         * For most other kinds of view, should be {@code false}.
         */
        final boolean cascadeOnClose() {
            return (cascade & CASCADE_ON_CLOSE) != 0;
        }

        /**
         * {@code true} if calls to {@link #reset()} should cascade to {@link #wrapperFor}.
         * This is {@code false} if any change in the position of {@link #view} is immediately
         * reflected in the position of {@link #wrapperFor}, and vice-versa.
         */
        final boolean cascadeOnReset() {
            return (cascade & CASCADE_ON_RESET) != 0;
        }

        /**
         * Declares as invalid all unsynchronized {@code Coupled} instances which are used, directly or indirectly,
         * by this instance. This method is invoked before {@link StorageConnector#getStorageAs(Class)} returns a
         * view, in order to remember which views would need to be resynchronized if they are requested.
         */
        final void invalidateSources() {
            boolean sync = cascadeOnReset();
            for (Coupled c = wrapperFor; sync; c = c.wrapperFor) {
                c.isValid = false;
                sync = c.cascadeOnReset();
            }
        }

        /**
         * Declares as invalid all unsynchronized {@code Coupled} instances which are using, directly or indirectly,
         * this instance. This method is invoked before {@link StorageConnector#getStorageAs(Class)} returns a view,
         * in order to remember which views would need to be resynchronized if they are requested.
         */
        final void invalidateUsages() {
            if (wrappedBy != null) {
                for (final Coupled c : wrappedBy) {
                    if (c.cascadeOnReset()) {
                        c.isValid = false;
                        c.invalidateUsages();
                    }
                }
            }
        }

        /**
         * Identifies the other views to <strong>not</strong> close if we don't want to close the {@link #view}
         * wrapped by this {@code Coupled}. This method identifies only the views that <em>use</em> this view;
         * it does not identify the views <em>used</em> by this view.
         *
         * This method is for {@link StorageConnector#closeAllExcept(Object)} internal usage.
         *
         * @param  toClose  the map where to write the list of views to not close.
         */
        final void protect(final Map<AutoCloseable,Boolean> toClose) {
            if (wrappedBy != null) {
                for (final Coupled c : wrappedBy) {
                    if (!c.cascadeOnClose()) {
                        if (c.view instanceof AutoCloseable) {
                            toClose.put((AutoCloseable) c.view, Boolean.FALSE);
                        }
                        c.protect(toClose);
                    }
                }
            }
        }

        /**
         * Resets the position of all sources of the {@link #view}, then the view itself.
         *
         * @return {@code true} if some kind of reset has been performed.
         *         Note that it does means that the view {@link #isValid} is {@code true}.
         */
        final boolean reset() throws IOException {
            if (isValid) {
                return false;
            }
            /*
             * We need to reset the sources before to reset the view of this Coupled instance.
             * For example if this Coupled instance contains a ChannelDataInput, we need to
             * reset the underlying InputStream before to reset the ChannelDataInput.
             */
            if (cascadeOnReset()) {
                wrapperFor.reset();
            }
            if ((cascade & CLEAR_ON_RESET) != 0) {
                /*
                 * If the view cannot be reset, in some cases we can discard it and recreate a new view when
                 * first needed. The `isValid` flag is left to false for telling that a new value is requested.
                 */
                view = null;
                return true;
            } else if (view instanceof InputStream) {
                /*
                 * Note on InputStream.reset() behavior documented in java.io:
                 *
                 *  - It does not discard the mark, so it is okay if reset() is invoked twice.
                 *  - If mark is unsupported, may either throw IOException or reset the stream
                 *    to an implementation-dependent fixed state.
                 */
                ((InputStream) view).reset();
            } else if (view instanceof Reader) {
                /*
                 * Defined as a matter of principle but should not be needed since we do not wrap java.io.Reader
                 * (except in BufferedReader if the original storage does not support mark/reset).
                 */
                ((Reader) view).reset();
            } else if (view instanceof ChannelData) {
                /*
                 * `ChannelDataInput` can be recycled without the need to discard and recreate it.
                 * However if a `Channel` was used directly, it should have been seek to the channel
                 * beginning first. This seek should be done by above call to `wrapperFor.reset()`,
                 * which should cause the block below (with a call to `rewind()`) to be executed.
                 */
                ((ChannelData) view).seek(0);
            } else if (view instanceof ChannelImageOutputStream) {
                ((ChannelImageOutputStream) view).output().seek(0);
            } else if (view instanceof Channel) {
                /*
                 * Searches for a ChannelDataInput wrapping the channel, because it contains the original position
                 * (note: StorageConnector tries to instantiate ChannelDataInput in priority to all other types).
                 * If we don't find any, this is considered as a non-seekable channel (we do not assume that the
                 * channel original position was 0 when the user gave it to StorageConnector).
                 */
                String name = null;
                if (wrappedBy != null) {
                    for (Coupled c : wrappedBy) {
                        if (c.view instanceof ChannelData) {
                            final var in = (ChannelData) c.view;
                            assert in.channel() == view : c;
                            if (in.rewind()) {
                                return true;
                            }
                            name = in.filename;                                     // For the error message.
                        }
                    }
                }
                if (name == null) name = Classes.getShortClassName(view);
                throw new InvalidSeekException(Resources.format(Resources.Keys.StreamIsForwardOnly_1, name));
            } else {
                /*
                 * For any other kind of object, we don't know how to recycle them. Current implementation
                 * does nothing on the assumption that the object can be reused (example: NetcdfFile).
                 */
            }
            isValid = true;
            return true;
        }

        /**
         * Returns a string representation for debugging purpose.
         */
        @Override
        public String toString() {
            return Strings.toString(getClass(),
                    "view",       Classes.getShortClassName(view),
                    "wrapperFor", (wrapperFor != null) ? Classes.getShortClassName(wrapperFor.view) : null,
                    "cascade",    cascade,
                    "isValid",    isValid);
        }

        /**
         * Formats the current {@code Coupled} and all its children as a tree in the given tree table node.
         * This method is used for {@link StorageConnector#toString()} implementation only and may change
         * in any future version.
         *
         * @param appendTo  where to write name, value and children.
         * @param views     reference to the {@link StorageConnector#views} map. Will be read only.
         */
        @Debug
        final void append(final TreeTable.Node appendTo, final Map<Class<?>, Coupled> views) {
            Class<?> type = null;
            for (final Map.Entry<Class<?>, Coupled> entry : views.entrySet()) {
                if (entry.getValue() == this) {
                    final Class<?> t = Classes.findCommonClass(type, entry.getKey());
                    if (t != Object.class) type = t;
                }
            }
            appendTo.setValue(TableColumn.NAME,  Classes.getShortName(type));
            appendTo.setValue(TableColumn.VALUE, Classes.getShortClassName(view));
            if (wrappedBy != null) {
                for (final Coupled c : wrappedBy) {
                    c.append(appendTo.newChild(), views);
                }
            }
        }
    }

    /**
     * Creates a new data store connection wrapping the given input/output object.
     * The object can be of any type, but the class javadoc lists the most typical ones.
     *
     * @param storage  the input/output object as a URL, file, image input stream, <i>etc.</i>.
     */
    public StorageConnector(final Object storage) {
        this.storage = Objects.requireNonNull(storage);
    }

    /**
     * Creates a new data store connection which has a sub-component of a larger data store.
     * The new storage connector inherits all options that were specified in the parent connector.
     *
     * @param  parent  the storage connector from which to inherit options.
     * @param storage  the input/output object as a URL, file, image input stream, <i>etc.</i>.
     *
     * @since 1.5
     */
    public StorageConnector(final StorageConnector parent, final Object storage) {
        this(storage);
        if (!Containers.isNullOrEmpty(parent.options)) {
            options = new HashMap<>(parent.options);
        }
    }

    /**
     * Sets the option value for the given key.
     * The {@code StorageConnector} class uses the following options, if present:
     *
     * <ul>
     *   <li>{@link OptionKey#ENCODING}     for decoding characters in an input stream, if needed.</li>
     *   <li>{@link OptionKey#URL_ENCODING} for converting URL to URI or filename, if needed.</li>
     *   <li>{@link OptionKey#OPEN_OPTIONS} for specifying whether the data store shall be read only or read/write.</li>
     *   <li>{@link OptionKey#BYTE_BUFFER}  for allowing users to control the byte buffer to be created.</li>
     * </ul>
     *
     * In addition, each {@link DataStore} implementation may use more options.
     *
     * @param <T>    the type of option value.
     * @param key    the option for which to set the value.
     * @param value  the new value for the given option, or {@code null} for removing the value.
     */
    public <T> void setOption(final OptionKey<T> key, final T value) {
        options = key.setValueInto(options, value);
    }

    /**
     * Returns the option value for the given key, or {@code null} if none.
     * This is the value specified by the last call to a {@code setOption(…)} with the given key.
     *
     * @param  <T>  the type of option value.
     * @param  key  the option for which to get the value.
     * @return the current value for the given option, or {@code null} if none.
     */
    public <T> T getOption(final OptionKey<T> key) {
        return key.getValueFrom(options);
    }

    /**
     * Returns the input/output object given at construction time.
     * The object can be of any type, but the class javadoc lists the most typical ones.
     *
     * @return the input/output object as a URL, file, image input stream, <i>etc.</i>.
     * @throws DataStoreException if the storage object has already been used and cannot be reused.
     *
     * @see #getStorageAs(Class)
     */
    public Object getStorage() throws DataStoreException {
        reset();
        return storage;
    }

    /**
     * Returns a short name of the input/output object. For example if the storage is a file,
     * this method returns the filename without the path (but including the file extension).
     * The default implementation performs the following choices based on the type of the
     * {@linkplain #getStorage() storage} object:
     *
     * <ul>
     *   <li>For {@link Path}, {@link File}, {@link URI} or {@link URL}
     *       instances, this method uses dedicated API like {@link Path#getFileName()}.</li>
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
            name = Strings.trimOrNull(IOUtilities.filename(storage));
            if (name == null) {
                name = Strings.trimOrNull(IOUtilities.toString(storage));
                if (name == null) {
                    name = Classes.getShortClassName(storage);
                }
            }
        }
        return name;
    }

    /**
     * Returns the filename extension of the input/output object. The default implementation performs
     * the following choices based on the type of the {@linkplain #getStorage() storage} object:
     *
     * <ul>
     *   <li>For {@link Path}, {@link File}, {@link URI}, {@link URL} or
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
     * Returns {@code true} if the given type is one of the types supported by {@code StorageConnector}.
     * The list of supported types is hard-coded and may change in any future version.
     */
    static boolean isSupportedType(final Class<?> type) {
        return OPENERS.containsKey(type);
    }

    /**
     * Returns the storage as a view of the given type if possible, or {@code null} otherwise.
     * The default implementation accepts the following types:
     *
     * <ul>
     *   <li>{@link String}:
     *     <ul>
     *       <li>If the {@linkplain #getStorage() storage} object is an instance of the {@link Path},
     *           {@link File}, {@link URL}, {@link URI} or {@link CharSequence} types,
     *           returns the string representation of their path.</li>
     *       <li>Otherwise, this method returns {@code null}.</li>
     *     </ul>
     *   </li>
     *   <li>{@link Path}, {@link URI}, {@link URL}, {@link File}:
     *     <ul>
     *       <li>If the {@linkplain #getStorage() storage} object is an instance of the {@link Path},
     *           {@link File}, {@link URL}, {@link URI} or {@link CharSequence} types and
     *           that type can be converted to the requested type, returned the conversion result.</li>
     *       <li>Otherwise, this method returns {@code null}.</li>
     *     </ul>
     *   </li>
     *   <li>{@link ByteBuffer}:
     *     <ul>
     *       <li>If the {@linkplain #getStorage() storage} object can be obtained as described in bullet 2 of the
     *           {@code DataInput} section below, then this method returns the associated byte buffer.</li>
     *       <li>Otherwise, this method returns {@code null}.</li>
     *     </ul>
     *   </li>
     *   <li>{@link DataInput}:
     *     <ul>
     *       <li>If the {@linkplain #getStorage() storage} object is already an instance of {@code DataInput}
     *           (including the {@link ImageInputStream} and {@link ImageOutputStream} types),
     *           then it is returned unchanged.</li>
     *       <li>Otherwise, if the input is an instance of {@link ByteBuffer}, then an {@link ImageInputStream}
     *           backed by a read-only view of that buffer is created when first needed and returned.
     *           The properties (position, mark, limit) of the original buffer are unmodified.</li>
     *       <li>Otherwise, if the input is an instance of {@link Path}, {@link File},
     *           {@link URI}, {@link URL}, {@link CharSequence}, {@link InputStream} or
     *           {@link ReadableByteChannel}, then an {@link ImageInputStream} backed by a
     *           {@link ByteBuffer} is created when first needed and returned.</li>
     *       <li>Otherwise, if {@link ImageIO#createImageInputStream(Object)} returns a non-null value,
     *           then this value is cached and returned.</li>
     *       <li>Otherwise, this method returns {@code null}.</li>
     *     </ul>
     *   </li>
     *   <li>{@link ImageInputStream}:
     *     <ul>
     *       <li>If the above {@code DataInput} can be created and cast to {@code ImageInputStream}, returns it.</li>
     *       <li>Otherwise this method returns {@code null}.</li>
     *     </ul>
     *   </li>
     *   <li>{@link InputStream}:
     *     <ul>
     *       <li>If the {@linkplain #getStorage() storage} object is already an instance of {@link InputStream},
     *           then it is returned unchanged.</li>
     *       <li>Otherwise, if the above {@code ImageInputStream} can be created,
     *           returns a wrapper around that stream.</li>
     *       <li>Otherwise, this method returns {@code null}.</li>
     *     </ul>
     *   </li>
     *   <li>{@link Reader}:
     *     <ul>
     *       <li>If the {@linkplain #getStorage() storage} object is already an instance of {@link Reader},
     *           then it is returned unchanged.</li>
     *
     *       <li>Otherwise, if the above {@code InputStream} can be created, returns an {@link InputStreamReader}
     *           using the encoding specified by {@link OptionKey#ENCODING} if any, or using the system default
     *           encoding otherwise.</li>
     *       <li>Otherwise, this method returns {@code null}.</li>
     *     </ul>
     *   </li>
     *   <li>{@link DataOutput}:
     *     <ul>
     *       <li>If the {@linkplain #getStorage() storage} object is already an instance of {@code DataOutput}
     *           (including the {@link ImageOutputStream} type), then it is returned unchanged.</li>
     *       <li>Otherwise, if the output is an instance of {@link Path}, {@link File},
     *           {@link URI}, {@link URL}, {@link CharSequence}, {@link OutputStream} or
     *           {@link WritableByteChannel}, then an {@link ImageInputStream} backed by a
     *           {@link ByteBuffer} is created when first needed and returned.</li>
     *       <li>Otherwise, if {@link ImageIO#createImageOutputStream(Object)} returns a non-null value,
     *           then this value is cached and returned.</li>
     *       <li>Otherwise, this method returns {@code null}.</li>
     *     </ul>
     *   </li>
     *   <li>{@link ImageOutputStream}:
     *     <ul>
     *       <li>If the above {@code DataOutput} can be created and cast to {@code ImageOutputStream}, returns it.</li>
     *       <li>Otherwise this method returns {@code null}.</li>
     *     </ul>
     *   </li>
     *   <li>{@link OutputStream}:
     *     <ul>
     *       <li>If the above {@code DataOutput} can be created and cast to {@code OutputStream}, returns it.</li>
     *       <li>Otherwise this method returns {@code null}.</li>
     *     </ul>
     *   </li>
     *   <li>{@link DataSource}:
     *     <ul>
     *       <li>If the {@linkplain #getStorage() storage} object is already an instance of {@link DataSource},
     *           then it is returned unchanged.</li>
     *       <li>Otherwise, if the storage is convertible to an {@link URI} and the {@linkplain URI#getScheme()
     *           URI scheme} is "jdbc" (ignoring case), then a data source delegating to {@link DriverManager}
     *           is created when first needed and returned.</li>
     *       <li>Otherwise, this method returns {@code null}.</li>
     *     </ul>
     *   </li>
     *   <li>{@link Connection}:
     *     <ul>
     *       <li>If the {@linkplain #getStorage() storage} object is already an instance of {@link Connection},
     *           then it is returned unchanged.</li>
     *       <li>Otherwise, if the storage is convertible to a {@link DataSource}, then a connection is obtained
     *           when first needed and returned.</li>
     *       <li>Otherwise, this method returns {@code null}.</li>
     *     </ul>
     *   </li>
     *   <li>Any other types:
     *     <ul>
     *       <li>If the storage given at construction time is already an instance of the requested type,
     *           returns it <i>as-is</i>.</li>
     *       <li>Otherwise, this method throws {@link IllegalArgumentException}.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <h4>Usage for probing operations</h4>
     * Multiple invocations of this method on the same {@code StorageConnector} instance will try
     * to return the same instance on a <em>best effort</em> basis. Consequently, implementations of
     * {@link DataStoreProvider#probeContent(StorageConnector)} methods shall not close the stream or
     * database connection returned by this method. In addition, those {@code probeContent(StorageConnector)}
     * methods are responsible for restoring the stream or byte buffer to its original position on return.
     * For an easier and safer way to ensure that the storage position is not modified,
     * see {@link DataStoreProvider#probeContent(StorageConnector, Class, Prober)}.
     *
     * @param  <S>   the compile-time type of the {@code type} argument (the source or storage type).
     * @param  type  the desired type as one of {@code ByteBuffer}, {@code DataInput}, {@code Connection}
     *               class or other types supported by {@code StorageConnector} subclasses.
     * @return the storage as a view of the given type, or {@code null} if the given type is one of the supported
     *         types listed in javadoc but no view can be created for the source given at construction time.
     *         In the latter case, {@link #wasProbingAbsentFile()} can be invoked for determining whether the
     *         reason is that the file does not exist but could be created.
     * @throws IllegalArgumentException if the given {@code type} argument is not one of the supported types
     *         listed in this javadoc or in subclass javadoc.
     * @throws IllegalStateException if this {@code StorageConnector} has been {@linkplain #closeAllExcept closed}.
     * @throws DataStoreException if an error occurred while opening a stream or database connection.
     *
     * @see #getStorage()
     * @see #closeAllExcept(Object)
     * @see DataStoreProvider#probeContent(StorageConnector, Class, Prober)
     */
    public <S> S getStorageAs(final Class<S> type) throws IllegalArgumentException, DataStoreException {
        if (isClosed()) {
            throw new IllegalStateException(Resources.format(Resources.Keys.ClosedStorageConnector));
        }
        /*
         * Verify if the cache contains an instance created by a previous invocation of this method.
         * Note that InputStream may need to be reset if it has been used indirectly by other kind
         * of stream (for example a java.io.Reader). Example:
         *
         *    1) The storage specified at construction time is a `Path`.
         *    2) getStorageAs(InputStream.class) opens an InputStream. Caller rewinds it after use.
         *    3) getStorageAs(Reader.class) wraps the InputStream. Caller rewinds the Reader after use,
         *       but invoking BufferedReader.reset() has no effect on the underlying InputStream.
         *    4) getStorageAs(InputStream.class) needs to rewind the InputStream itself since it was
         *       not done at step 3. However, doing so invalidate the Reader, so we need to discard it.
         */
        Coupled value = getView(Objects.requireNonNull(type));
        if (reset(value)) {
            return type.cast(value.view);               // null is a valid result.
        }
        /*
         * If the storage is already an instance of the requested type, returns the storage as-is.
         * We check if the storage needs to be reset in the same way as in getStorage() method.
         * As a special case, we ensure that InputStream and Reader can be marked.
         */
        if (type.isInstance(storage)) {
            @SuppressWarnings("unchecked")
            S view = (S) storage;
            reset();
            byte cascade = 0;
            if (type == InputStream.class) {
                final InputStream in = (InputStream) view;
                if (!in.markSupported()) {
                    view = type.cast(new BufferedInputStream(in));
                    cascade = (byte) (CLEAR_ON_RESET | CASCADE_ON_RESET);
                }
            } else if (type == Reader.class) {
                final Reader in = (Reader) view;
                if (!in.markSupported()) {
                    view = type.cast(new LineNumberReader(in));
                    cascade = (byte) (CLEAR_ON_RESET | CASCADE_ON_RESET);
                }
            }
            addView(type, view, null, cascade);
            return view;
        }
        /*
         * If the type is not one of the types listed in OPENERS, we delegate to ObjectConverter.
         * It may throw UnconvertibleObjectException (an IllegalArgumentException subtype) if the
         * given type is unrecognized. So the IllegalArgumentException documented in method javadoc
         * happen (indirectly) here.
         */
        final Opener<?> method = OPENERS.get(type);
        if (method == null) {
            S view;
            try {
                view = ObjectConverters.convert(storage, type);
            } catch (UnconvertibleObjectException e) {
                if (!OPENERS.containsKey(type)) throw e;
                Logging.recoverableException(StoreUtilities.LOGGER, StorageConnector.class, "getStorageAs", e);
                view = null;
            }
            addView(type, view);
            return view;
        }
        /*
         * No instance has been created previously for the requested type. Open the stream now.
         * Some types will need to reset the InputStream or Channel, but the decision of doing
         * so or not is left to openers. Result will be cached by the `createFoo()` method.
         * Note that it may cache `null` value if no stream of the given type can be created.
         */
        final Object view;
        try {
            view = method.open(this);
        } catch (DataStoreException e) {
            throw e;
        } catch (Exception e) {
            short key = Errors.Keys.CanNotOpen_1;
            if (e instanceof NoSuchFileException) {
                key = Errors.Keys.FileNotFound_1;
            }
            throw new DataStoreException(Errors.format(key, getStorageName()), e);
        }
        return type.cast(view);
    }

    /**
     * Resets the given view. If the view is an instance of {@link InputStream}, {@link ReadableByteChannel} or
     * other objects that may be affected by views operations, this method will reset the storage position.
     * The view must have been previously marked by {@link InputStream#mark(int)} or equivalent method.
     *
     * <p>This method is <strong>not</strong> a substitute for the requirement that users leave the
     * {@link #getStorageAs(Class)} return value in the same state as they found it. This method is
     * only for handling the cases where using a view has an indirect impact on another view.</p>
     *
     * <h4>Rational</h4>
     * {@link DataStoreProvider#probeContent(StorageConnector)} contract requires that implementers reset the
     * input stream themselves. However if {@link ChannelDataInput} or {@link InputStreamReader} has been used,
     * then the user performed a call to {@link ChannelDataInput#reset()} (for instance), which did not reset
     * the underlying input stream. So we need to perform the missing {@link InputStream#reset()} here, then
     * synchronize the {@code ChannelDataInput} position accordingly.
     *
     * @param  c  container of the view to reset, or {@code null} if none.
     * @return {@code true} if the given view, after reset, is valid.
     *         Note that {@link Coupled#view} may be null and valid.
     */
    private boolean reset(final Coupled c) throws DataStoreException {
        final boolean done;
        if (c == null) {
            return false;
        } else try {
            done = c.reset();
        } catch (IOException e) {
            throw new ForwardOnlyStorageException(Resources.format(
                        Resources.Keys.StreamIsReadOnce_1, getStorageName()), e);
        }
        if (done) {
            c.invalidateSources();
            c.invalidateUsages();
        }
        return c.isValid;
    }

    /**
     * Resets the root {@link #storage} object.
     *
     * @throws DataStoreException if the storage cannot be reset.
     */
    private void reset() throws DataStoreException {
        if (views != null && !views.isEmpty() && !reset(views.get(null))) {
            throw new ForwardOnlyStorageException(Resources.format(
                        Resources.Keys.StreamIsReadOnce_1, getStorageName()));
        }
    }

    /**
     * Returns whether returning the storage would have required the creation of a new file.
     * This method may return {@code true} if all the following conditions are true:
     *
     * <ul>
     *   <li>The {@linkplain #getOption(OptionKey) options} given to this {@code StorageConnector} include
     *       {@link java.nio.file.StandardOpenOption#CREATE} or {@code CREATE_NEW}.</li>
     *   <li>The {@code getStorageAs(…)} and {@code wasProbingAbsentFile()} calls happened in the context of
     *       {@link DataStores} probing the storage content in order to choose a {@link DataStoreProvider}.</li>
     *   <li>A previous {@link #getStorageAs(Class)} call requested some kind of input stream
     *       (e.g. {@link InputStream}, {@link ImageInputStream}, {@link DataInput}, {@link Reader}).</li>
     *   <li>One of the following conditions is true:
     *     <ul>
     *       <li>The input stream is empty.</li>
     *       <li>The {@linkplain #getStorage() storage} is an object convertible to a {@link Path} and the
     *           file identified by that path {@linkplain java.nio.file.Files#notExists does not exist}.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * If all above conditions are true, then {@link #getStorageAs(Class)} returns {@code null} instead of creating
     * a new empty file. In such case, {@link DataStoreProvider} may use this {@code wasProbingAbsentFile()} method
     * for deciding whether to report {@link ProbeResult#SUPPORTED} or {@link ProbeResult#UNSUPPORTED_STORAGE}.
     *
     * <h4>Rational</h4>
     * When the file does not exist and the {@code CREATE} or {@code CREATE_NEW} option is provided,
     * {@code getStorageAs(…)} would normally create a new empty file. However this behavior is modified during probing
     * (the first condition in above list) because newly created files are empty and probing empty files may result in
     * {@link java.io.EOFException} to be thrown or in providers declaring that they do not support the storage.
     *
     * <p>IF the {@code CREATE} or {@code CREATE_NEW} options were not provided, then probing the storage content of an
     * absent file will rather throw {@link java.nio.file.NoSuchFileException} or {@link java.io.FileNotFoundException}.
     * So this method is useful only for {@link DataStore} having write capabilities.</p>
     *
     * @return  whether returning the storage would have required the creation of a new file.
     *
     * @since 1.4
     */
    public boolean wasProbingAbsentFile() {
        return probing != null && probing.probe != null;
    }

    /**
     * Creates a view for the input as a {@link ChannelDataInput} if possible.
     * This is also a starting point for {@link #createDataInput()} and {@link #createByteBuffer()}.
     * This method is one of the {@link #OPENERS} methods and should be invoked at most once per
     * {@code StorageConnector} instance.
     *
     * @return input channel, or {@code null} if none or if {@linkplain #probing} result has been determined externally.
     * @throws IOException if an error occurred while opening a channel for the input.
     *
     * @see #createChannelDataOutput()
     */
    private ChannelDataInput createChannelDataInput() throws IOException, DataStoreException {
        /*
         * Before to try to wrap an InputStream, mark its position so we can rewind if the user asks for
         * the InputStream directly. We need to reset because ChannelDataInput may have read some bytes.
         * Note that if mark is unsupported, the default InputStream.mark(…) implementation does nothing.
         */
        reset();
        if (storage instanceof InputStream) {
            ((InputStream) storage).mark(READ_AHEAD_LIMIT);
        }
        if (storage instanceof ByteBuffer) {
            final ChannelDataInput asDataInput = new ChannelImageInputStream(getStorageName(), (ByteBuffer) storage);
            addView(ChannelDataInput.class, asDataInput);
            return asDataInput;
        }
        /*
         * Following method call recognizes ReadableByteChannel, InputStream (with optimization for FileInputStream),
         * URL, URI, File, Path or other types that may be added in future Apache SIS versions.
         * If the given storage is already a ReadableByteChannel, then the factory will return it as-is.
         */
        final ChannelFactory factory = createChannelFactory(false);
        if (factory == null) {
            return null;
        }
        /*
         * If the storage is a file, that file does not exist, the open options include `CREATE` or `CREATE_NEW`
         * and this method is invoked for probing the file content (not yet for creating the data store), then
         * set the probe result without opening the file. We do that because attempts to probe a newly created
         * file would probably cause an EOFException to be thrown.
         */
        if (probing != null && factory.isCreateNew()) {
            probing.setProbingAbsentFile();
            return null;
        }
        /*
         * ChannelDataInput depends on ReadableByteChannel, which itself depends on storage
         * (potentially an InputStream). We need to remember this chain in `Coupled` objects.
         */
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final String name = getStorageName();
        final ReadableByteChannel channel = factory.readable(name, null);
        addView(ReadableByteChannel.class, channel, null, factory.isCoupled() ? CASCADE_ON_RESET : 0);
        final ByteBuffer buffer = getChannelBuffer(factory);
        final ChannelDataInput asDataInput = new ChannelDataInput(name, channel, buffer, false);
        addView(ChannelDataInput.class, asDataInput, ReadableByteChannel.class, CASCADE_ON_RESET);
        /*
         * Following is an undocumented mechanism for allowing some Apache SIS implementations of DataStore
         * to re-open the same channel or input stream another time, typically for re-reading the same data.
         */
        if (factory.canReopen()) {
            addView(ChannelFactory.class, factory);
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
     * @return input stream, or {@code null} if none or if {@linkplain #probing} result has been determined externally.
     * @throws IOException if an error occurred while opening a stream for the input.
     *
     * @see #createDataOutput()
     */
    private DataInput createDataInput() throws IOException, DataStoreException {
        /*
         * Gets or creates a ChannelImageInputStream instance if possible. We really need that specific
         * type because some SIS data stores will want to access directly the channel and the buffer.
         * We will fallback on the ImageIO.createImageInputStream(Object) method only in last resort.
         */
        Coupled c = getView(ChannelDataInput.class);
        final ChannelDataInput in;
        if (reset(c)) {
            in = (ChannelDataInput) c.view;
        } else {
            in = createChannelDataInput();          // May be null. This method should not have been invoked before.
        }
        final DataInput asDataInput;
        if (in != null) {
            asDataInput = in;
            c = getView(ChannelDataInput.class);    // Refresh because may have been added by createChannelDataInput().
            views.put(DataInput.class, c);          // Share the same `Coupled` instance.
        } else if (wasProbingAbsentFile()) {
            return null;                            // Do not cache, for allowing file creation later.
        } else {
            /*
             * This block is executed for storages of unknown type, when `ChannelFactory` has no branch for that type.
             * The Image I/O plugin mechanism allows users to create streams from arbitrary objets, so we delegate to
             * it in last resort.
             */
            reset();
            try {
                asDataInput = ImageIO.createImageInputStream(storage);
            } catch (IIOException e) {
                throw unwrap(e);
            }
            addView(DataInput.class, asDataInput, null, (byte) (CASCADE_ON_RESET | CASCADE_ON_CLOSE));
            /*
             * Note: Java Image I/O wrappers for Input/OutputStream do NOT close the underlying streams.
             * This is a complication for us. However in Apache SIS, `ImageInputStream` is used only
             * by WorldFileStore. That store has its own mechanism for closing the underlying stream.
             * So the problem described above would hopefully not occur in practice.
             */
        }
        return asDataInput;
    }

    /**
     * Returns or allocate a buffer for use with the {@link ChannelDataInput} or {@link ChannelDataOutput}.
     * If the user did not specify a buffer, this method may allocate a direct buffer for better
     * leveraging of {@link ChannelDataInput}, which tries hard to transfer data in the most direct
     * way between buffers and arrays. By contrast creating a heap buffer may imply the use of a
     * temporary direct buffer cached by the JDK itself (in JDK internal implementation).
     *
     * @param  factory  the factory which will be used for creating the readable or writable channel.
     * @return the byte buffer to use with {@link ChannelDataInput} or {@link ChannelDataOutput}.
     */
    private ByteBuffer getChannelBuffer(final ChannelFactory factory) {
        @SuppressWarnings("deprecation")
        ByteBuffer buffer = getOption(OptionKey.BYTE_BUFFER);               // User-supplied buffer.
        if (buffer == null) {
            if (factory.suggestDirectBuffer) {
                buffer = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE);
            } else {
                buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
            }
        }
        return buffer;
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
     * @return buffer containing at least the first bytes of storage content, or {@code null} if none.
     * @throws IOException if an error occurred while opening a stream for the input.
     */
    private ByteBuffer createByteBuffer() throws IOException, DataStoreException {
        /*
         * First, try to create the ChannelDataInput if it does not already exists.
         * If successful, this will create a ByteBuffer companion as a side effect.
         * Byte order of the view is intentionally left to the default (big endian)
         * because we expect the callers to set the order that they need.
         */
        final ChannelDataInput c = getStorageAs(ChannelDataInput.class);
        ByteBuffer asByteBuffer = null;
        if (c != null) {
            asByteBuffer = c.buffer.asReadOnlyBuffer();
        } else if (wasProbingAbsentFile()) {
            return null;                // Do not cache, for allowing file creation when opening the data store.
        } else {
            /*
             * If no ChannelDataInput has been created by the above code, get the input as an ImageInputStream and
             * read an arbitrary number of bytes. Read only a small number of bytes because, at the contrary of the
             * buffer created in `createChannelDataInput()`, the buffer created here is unlikely to be used for the
             * reading process after the recognition of the file format.
             */
            final ImageInputStream in = getStorageAs(ImageInputStream.class);
            if (in != null) {
                in.mark();
                final var buffer = new byte[MINIMAL_BUFFER_SIZE];
                final int n = in.read(buffer);
                in.reset();
                if (n >= 1) {
                    // Cannot invoke asReadOnly() because `prefetch()` needs to be able to write in it.
                    asByteBuffer = ByteBuffer.wrap(buffer).order(in.getByteOrder());
                    asByteBuffer.limit(n);
                }
            }
        }
        addView(ByteBuffer.class, asByteBuffer);
        return asByteBuffer;
    }

    /**
     * Transfers more bytes from the {@link DataInput} to the {@link ByteBuffer}, if possible.
     * This method returns {@code true} on success, or {@code false} if input is not a readable
     * channel or stream, we have reached the end of stream, or the buffer is full.
     *
     * <p>This method is invoked when the number of bytes in the buffer appears to be insufficient
     * for {@link DataStoreProvider#probeContent(StorageConnector)} purpose.</p>
     *
     * @return {@code true} on success.
     * @throws DataStoreException if an error occurred while reading more bytes.
     */
    final boolean prefetch() throws DataStoreException {
        try {
            /*
             * In most Apache SIS data store implementations, we use ChannelDataInput. If the object wrapped
             * by ChannelDataInput has not been used directly, then Coupled.isValid should be true.  In such
             * case, reset(c) does nothing and ChannelDataInput.prefetch() will read new bytes from current
             * channel position. Otherwise, a new read operation from the beginning will be required and we
             * can only hope that it will read more bytes than last time.
             */
            Coupled c = getView(ChannelDataInput.class);
            if (c != null) {
                reset(c);                   // Does nothing is c.isValid is true.
                return c.isValid && ((ChannelDataInput) c.view).prefetch() > 0;
            }
            /*
             * The above code is the usual case. The code below this point is the fallback used when only
             * an ImageInputStream was available. In such case, the ByteBuffer can only be the one created
             * by the above createByteBuffer() method, which is known to be backed by a writable array.
             */
            c = getView(ImageInputStream.class);
            if (reset(c)) {
                final ImageInputStream input = (ImageInputStream) c.view;
                c = getView(ByteBuffer.class);
                if (reset(c)) {                 // reset(c) as a matter of principle, but (c != null) would have worked.
                    final ByteBuffer buffer = (ByteBuffer) c.view;
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
            }
        } catch (IOException e) {
            throw new DataStoreException(Errors.format(Errors.Keys.CanNotRead_1, getStorageName()), e);
        }
        return false;
    }

    /**
     * Creates an {@link ChannelImageInputStream} from the {@link ChannelDataInput} if possible.
     * This method is one of the {@link #OPENERS} methods and should be invoked at most once per
     * {@code StorageConnector} instance.
     *
     * @return input stream, or {@code null} if none or if {@linkplain #probing} result has been determined externally.
     */
    private ChannelImageInputStream createChannelImageInputStream() throws IOException, DataStoreException {
        final Coupled c;
        final ChannelImageInputStream asImageInput;
        ChannelDataInput input = getStorageAs(ChannelDataInput.class);
        if (input != null)  {
            c = getView(ChannelDataInput.class);
            if (input instanceof ChannelImageInputStream) {
                asImageInput = (ChannelImageInputStream) input;
            } else {
                asImageInput = new ChannelImageInputStream(input);
                c.view = asImageInput;   // Upgrade existing instance for all views.
            }
        } else {
            if (!wasProbingAbsentFile()) {
                addView(ChannelImageInputStream.class, null);   // Remember that there is no view.
            }
            return null;
        }
        views.put(ChannelImageInputStream.class, c);            // Share the same `Coupled` instance.
        return asImageInput;
    }

    /**
     * Creates an {@link ImageInputStream} from the {@link DataInput} if possible. This method casts
     * {@code DataInput} if such cast is allowed, or upgrades {@link ChannelDataInput} implementation.
     *
     * <p>This method is one of the {@link #OPENERS} methods and should be invoked at most once per
     * {@code StorageConnector} instance.</p>
     *
     * @return input stream, or {@code null} if none or if {@linkplain #probing} result has been determined externally.
     */
    private ImageInputStream createImageInputStream() throws IOException, DataStoreException {
        final Coupled c;
        final ImageInputStream asDataInput;
        DataInput input = getStorageAs(DataInput.class);
        if (input instanceof ImageInputStream) {
            asDataInput = (ImageInputStream) input;
            c = views.get(DataInput.class);
        } else {
            input = getStorageAs(ChannelDataInput.class);
            if (input != null)  {
                c = getView(ChannelDataInput.class);
                if (input instanceof ImageInputStream) {
                    asDataInput = (ImageInputStream) input;
                } else {
                    asDataInput = new ChannelImageInputStream((ChannelDataInput) input);
                    c.view = asDataInput;   // Upgrade existing instance for all views.
                }
            } else {
                if (!wasProbingAbsentFile()) {
                    /*
                     * We do not invoke `ImageIO.createImageInputStream(Object)` because we do not know
                     * how the stream will use the `storage` object. It may read in advance some bytes,
                     * which can invalidate the storage for use outside the `ImageInputStream`. Instead
                     * creating image input/output streams is left to caller's responsibility.
                     */
                    addView(ImageInputStream.class, null);          // Remember that there is no view.
                }
                return null;
            }
        }
        views.put(ImageInputStream.class, c);       // Share the same `Coupled` instance.
        return asDataInput;
    }

    /**
     * Creates an input stream from {@link ReadableByteChannel} if possible,
     * or from {@link ImageInputStream} otherwise.
     *
     * <p>This method is one of the {@link #OPENERS} methods and should be invoked at most once per
     * {@code StorageConnector} instance.</p>
     *
     * @return input stream, or {@code null} if none or if {@linkplain #probing} result has been determined externally.
     *
     * @see #createOutputStream()
     */
    private InputStream createInputStream() throws IOException, DataStoreException {
        final Class<ImageInputStream> source = ImageInputStream.class;
        final ImageInputStream input = getStorageAs(source);
        if (input != null) {
            if (input instanceof InputStream) {
                views.put(InputStream.class, views.get(source));        // Share the same `Coupled` instance.
                return (InputStream) input;
            }
            /*
             * Wrap the ImageInputStream as an ordinary InputStream. We avoid setting CASCADE_ON_RESET (unless
             * reset() needs to propagate further than ImageInputStream) because changes in InputStreamAdapter
             * position are immediately reflected by corresponding changes in ImageInputStream position.
             */
            final InputStream in = new InputStreamAdapter(input);
            addView(InputStream.class, in, source, (byte) (getView(source).cascade & CASCADE_ON_RESET));
            return in;
        } else if (!wasProbingAbsentFile()) {
            addView(InputStream.class, null);                                   // Remember that there is no view.
        }
        return null;
    }

    /**
     * Creates a character reader if possible.
     *
     * <p>This method is one of the {@link #OPENERS} methods and should be invoked at most once per
     * {@code StorageConnector} instance.</p>
     *
     * @return input characters, or {@code null} if none or if {@linkplain #probing} result has been determined externally.
     */
    private Reader createReader() throws IOException, DataStoreException {
        final InputStream input = getStorageAs(InputStream.class);
        if (input == null) {
            if (!wasProbingAbsentFile()) {
                addView(Reader.class, null);                                    // Remember that there is no view.
            }
            return null;
        }
        input.mark(READ_AHEAD_LIMIT);
        final Reader in = new RewindableLineReader(input, getOption(OptionKey.ENCODING));
        addView(Reader.class, in, InputStream.class, (byte) (CLEAR_ON_RESET | CASCADE_ON_RESET));
        return in;
    }

    /**
     * Creates a database source if possible.
     *
     * <p>This method is one of the {@link #OPENERS} methods and should be invoked at most once per
     * {@code StorageConnector} instance.</p>
     *
     * @return input/output, or {@code null} if none.
     */
    private DataSource createDataSource() throws DataStoreException {
        final URI uri = getStorageAs(URI.class);
        if (uri != null && Constants.JDBC.equalsIgnoreCase(uri.getScheme())) {
            final DataSource source = new URLDataSource(uri);
            addView(DataSource.class, source, null, (byte) 0);
            return source;
        }
        return null;
    }

    /**
     * Creates a database connection if possible.
     *
     * <p>This method is one of the {@link #OPENERS} methods and should be invoked at most once per
     * {@code StorageConnector} instance.</p>
     *
     * @return input/output, or {@code null} if none.
     */
    private Connection createConnection() throws SQLException, DataStoreException {
        final DataSource source = getStorageAs(DataSource.class);
        if (source != null) {
            final Connection c = source.getConnection();
            addView(Connection.class, c, null, (byte) 0);
            return c;
        }
        return null;
    }

    /**
     * Returns the storage as a path if possible, or {@code null} otherwise.
     *
     * <p>This method is one of the {@link #OPENERS} methods and should be invoked at most once per
     * {@code StorageConnector} instance.</p>
     *
     * @return string representation of the storage path, or {@code null} if none.
     */
    private String createString() {
        return IOUtilities.toString(storage);
    }

    /**
     * Adds the given view in the cache, without dependencies.
     *
     * @param  <S>   the compile-time type of the {@code type} argument.
     * @param  type  the view type.
     * @param  view  the view, or {@code null} if none.
     */
    private <S> void addView(final Class<S> type, final S view) {
        addView(type, view, null, (byte) 0);
    }

    /**
     * Returns a byte channel factory from the storage, or {@code null} if the storage is unsupported.
     * See {@link ChannelFactory#prepare(Object, boolean, String, OpenOption[])} for more information.
     *
     * @param  allowWriteOnly  whether to allow wrapping {@link WritableByteChannel} and {@link OutputStream}.
     * @return the channel factory for the given input, or {@code null} if the given input is of unknown type.
     * @throws IOException if an error occurred while processing the given input.
     */
    private ChannelFactory createChannelFactory(final boolean allowWriteOnly) throws IOException {
        ChannelFactory factory = ChannelFactory.prepare(storage, allowWriteOnly,
                getOption(OptionKey.URL_ENCODING),
                getOption(OptionKey.OPEN_OPTIONS));
        final UnaryOperator<ChannelFactory> wrapper = getOption(InternalOptionKey.CHANNEL_FACTORY_WRAPPER);
        if (factory != null && wrapper != null) {
            factory = wrapper.apply(factory);
        }
        return factory;
    }

    /**
     * Creates a view for the storage as a {@link ChannelDataOutput} if possible.
     * This code is a partial copy of {@link #createDataInput()} adapted for output.
     *
     * @return output channel, or {@code null} if none.
     * @throws IOException if an error occurred while opening a channel for the output.
     *
     * @see #createChannelDataInput()
     */
    private ChannelDataOutput createChannelDataOutput() throws IOException, DataStoreException {
        /*
         * We need to reset because the output that we will build may be derived
         * from the `ChannelDataInput`, which may have read some bytes.
         */
        reset();
        /*
         * Following method call recognizes WritableByteChannel, OutputStream (with optimization for FileOutputStream),
         * URL, URI, File, Path or other types that may be added in future Apache SIS versions.
         * If the given storage is already a WritableByteChannel, then the factory will return it as-is.
         */
        final ChannelFactory factory = createChannelFactory(true);
        if (factory == null) {
            return null;
        }
        /*
         * ChannelDataOutput depends on WritableByteChannel, which itself depends on storage
         * (potentially an OutputStream). We need to remember this chain in `Coupled` objects.
         */
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final String name = getStorageName();
        final WritableByteChannel channel = factory.writable(name, null);
        addView(WritableByteChannel.class, channel, null, factory.isCoupled() ? CASCADE_ON_RESET : 0);
        final ByteBuffer buffer = getChannelBuffer(factory);
        final ChannelDataOutput asDataOutput = new ChannelDataOutput(name, channel, buffer);
        addView(ChannelDataOutput.class, asDataOutput, WritableByteChannel.class, CASCADE_ON_RESET);
        /*
         * Following is an undocumented mechanism for allowing some Apache SIS implementations of DataStore
         * to re-open the same channel or output stream another time, typically for re-writing the same data.
         */
        if (factory.canReopen()) {
            addView(ChannelFactory.class, factory);
        }
        return asDataOutput;
    }

    /**
     * Creates a view for the output as a {@link DataOutput} if possible.
     * This code is a copy of {@link #createDataInput()} adapted for output.
     *
     * @return output stream, or {@code null} if none.
     * @throws IOException if an error occurred while opening a stream for the output.
     *
     * @see #createDataInput()
     */
    private DataOutput createDataOutput() throws IOException, DataStoreException {
        Coupled c = getView(ChannelDataOutput.class);
        final ChannelDataOutput out;
        if (reset(c)) {
            out = (ChannelDataOutput) c.view;
        } else {
            out = createChannelDataOutput();        // May be null. This method should not have been invoked before.
        }
        final DataOutput asDataOutput;
        if (out != null) {
            asDataOutput = out;
            c = getView(ChannelDataOutput.class);   // Refresh because may have been added by createChannelDataOutput().
            views.put(DataOutput.class, c);         // Share the same `Coupled` instance.
        } else {
            /*
             * This block is executed for storages of unknown type, when `ChannelFactory` has no branch for that type.
             * The Image I/O plugin mechanism allows users to create streams from arbitrary objets, so we delegate to
             * it in last resort.
             */
            reset();
            try {
                asDataOutput = ImageIO.createImageOutputStream(storage);
            } catch (IIOException e) {
                throw unwrap(e);
            }
            addView(DataOutput.class, asDataOutput, null, (byte) (CASCADE_ON_RESET | CASCADE_ON_CLOSE));
            /*
             * Note: Java Image I/O wrappers for Input/OutputStream do NOT close the underlying streams.
             * This is a complication for us. However in Apache SIS, `ImageOutputStream` is used only
             * by WorldFileStore. That store has its own mechanism for closing the underlying stream.
             * So the problem described above would hopefully not occur in practice.
             */
        }
        return asDataOutput;
    }

    /**
     * Creates an {@link ImageOutputStream} from the {@link DataOutput} if possible. This method casts
     * {@code DataOutput} if such cast is allowed, or upgrades {@link ChannelDataOutput} implementation.
     *
     * <p>This method is one of the {@link #OPENERS} methods and should be invoked at most once per
     * {@code StorageConnector} instance.</p>
     *
     * @return input stream, or {@code null} if none or if {@linkplain #probing} result has been determined externally.
     */
    private ImageOutputStream createImageOutputStream() throws IOException, DataStoreException {
        final ImageOutputStream asDataOutput;
        DataOutput output = getStorageAs(DataOutput.class);
        if (output instanceof ImageOutputStream) {
            asDataOutput = (ImageOutputStream) output;
            Coupled c = views.get(DataOutput.class);
            views.put(ImageOutputStream.class, c);          // Share the same `Coupled` instance.
        } else {
            final byte cascade;
            final ChannelDataOutput c = getStorageAs(ChannelDataOutput.class);
            if (c != null && c.channel instanceof ReadableByteChannel) {
                asDataOutput = new ChannelImageOutputStream(c);
                cascade = CASCADE_ON_RESET;
            } else {
                asDataOutput = null;                        // Remember that there is no view.
                cascade = 0;
            }
            addView(ImageOutputStream.class, asDataOutput, ChannelDataOutput.class, cascade);
        }
        return asDataOutput;
    }

    /**
     * Creates an output stream from {@link WritableByteChannel} if possible,
     * or from {@link ImageOutputStream} otherwise.
     * This code is a partial copy of {@link #createInputStream()} adapted for output.
     *
     * @return output stream, or {@code null} if none.
     *
     * @see #createInputStream()
     */
    private OutputStream createOutputStream() throws IOException, DataStoreException {
        final Class<DataOutput> target = DataOutput.class;
        final DataOutput output = getStorageAs(target);
        if (output instanceof OutputStream) {
            views.put(OutputStream.class, views.get(target));                   // Share the same `Coupled` instance.
            return (OutputStream) output;
        } else {
            addView(OutputStream.class, null);                                  // Remember that there is no view.
            return null;
        }
    }

    /**
     * Adds the given view in the cache together with information about its dependency.
     * For example, {@link InputStreamReader} is a wrapper for a {@link InputStream}: read operations
     * from the latter may change position of the former, and closing the latter also close the former.
     *
     * @param  <S>      the compile-time type of the {@code type} argument.
     * @param  type     the view type.
     * @param  view     the view, or {@code null} if none.
     * @param  source   the type of input that {@code view} is wrapping, or {@code null} for {@link #storage}.
     * @param  cascade  bitwise combination of {@link #CASCADE_ON_CLOSE}, {@link #CASCADE_ON_RESET} or {@link #CLEAR_ON_RESET}.
     */
    private <S> void addView(final Class<S> type, final S view, final Class<?> source, final byte cascade) {
        if (views == null) {
            views = new IdentityHashMap<>();
            views.put(null, new Coupled(storage));
        }
        Coupled c = views.get(type);
        if (c == null) {
            if (view == storage) {
                c = views.get(null);
                c.invalidateUsages();
            } else {
                c = new Coupled(cascade != 0 ? views.get(source) : null, cascade);
                // Newly created objects are not yet used by anyone, so no need to invoke c.invalidateUsages().
            }
            views.put(type, c);
        } else {
            assert c.view == null || c.view == view : c;
            assert c.cascade == cascade : cascade;
            assert c.wrapperFor == (cascade != 0 ? views.get(source) : null) : c;
            c.invalidateUsages();
        }
        c.view = view;
        c.isValid = true;
        c.invalidateSources();
    }

    /**
     * Returns the view for the given type from the cache.
     * This method does <strong>not</strong> {@linkplain #reset(Coupled) reset} the view.
     *
     * @param  type  the view type, or {@code null} for the {@link #storage} container.
     * @return information associated to the given type. May be {@code null} if the view has never been
     *         requested before. {@link Coupled#view} may be {@code null} if the view has been requested
     *         and we determined that none can be created.
     */
    private Coupled getView(final Class<?> type) {
        return (views != null) ? views.get(type) : null;
    }

    /**
     * Returns the storage as a view of the given type and closes all other views.
     * Invoking this method is equivalent to invoking {@link #getStorageAs(Class)}
     * followed by {@link #closeAllExcept(Object)} except that the latter method is
     * always invoked (in a way similar to "try with resource") and that this method
     * never returns {@code null}.
     *
     * @param  <S>     the compile-time type of the {@code type} argument (the source or storage type).
     * @param  type    the desired type as one of the types documented in {@link #getStorageAs(Class)}
     *                 (example: {@code ByteBuffer}, {@code DataInput}, {@code Connection}).
     * @param  format  short name or abbreviation of the data format (e.g. "CSV", "GML", "WKT", <i>etc</i>).
     *                 Used for information purpose in error messages if needed.
     * @return the storage as a view of the given type. Never {@code null}.
     * @throws IllegalArgumentException if the given {@code type} argument is not one of the supported types.
     * @throws IllegalStateException if this {@code StorageConnector} has been {@linkplain #closeAllExcept closed}.
     * @throws DataStoreException if an error occurred while opening a stream or database connection.
     *
     * @see #getStorageAs(Class)
     * @see #closeAllExcept(Object)
     *
     * @since 1.2
     */
    public <S> S commit(final Class<S> type, final String format) throws IllegalArgumentException, DataStoreException {
        final S view;
        try {
            view = getStorageAs(type);
        } catch (Throwable ex) {
            try {
                closeAllExcept(null);
            } catch (Throwable se) {
                ex.addSuppressed(se);
            }
            throw ex;
        }
        closeAllExcept(view);
        if (view == null) {
            throw new UnsupportedStorageException(null, format, storage, getOption(OptionKey.OPEN_OPTIONS));
        }
        return view;
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
    @SuppressWarnings("element-type-mismatch")
    public void closeAllExcept(final Object view) throws DataStoreException {
        if (views == null) {
            views = Map.of();               // For blocking future usage of this StorageConnector instance.
            if (storage != view && storage instanceof AutoCloseable) try {
                ((AutoCloseable) storage).close();
            } catch (DataStoreException e) {
                throw e;
            } catch (Exception e) {
                throw new DataStoreException(e);
            }
            return;
        }
        /*
         * Create a list of all views to close. The boolean value is TRUE if the view should be closed, or FALSE
         * if the view should be protected (not closed). FALSE values shall have precedence over TRUE values.
         */
        final var toClose = new IdentityHashMap<AutoCloseable,Boolean>(views.size());
        for (Coupled c : views.values()) {
            Object v = c.view;
            if (v != view) {
                if (v instanceof AutoCloseable) {
                    toClose.putIfAbsent((AutoCloseable) v, Boolean.TRUE);       // Mark `v` as needing to be closed.
                }
            } else {
                /*
                 * If there is a view to not close, search for all views that are wrapper for the given view.
                 * Those wrappers shall not be closed. For example if the caller does not want to close the
                 * InputStream view, then we shall not close the InputStreamReader wrapper neither.
                 */
                c.protect(toClose);
                do {
                    v = c.view;
                    if (v instanceof AutoCloseable) {
                        toClose.put((AutoCloseable) v, Boolean.FALSE);          // Protect `v` against closing.
                    }
                    c = c.wrapperFor;
                } while (c != null);
            }
        }
        /*
         * Trim the map in order to keep only the views to close.
         */
        toClose.values().removeIf((c) -> Boolean.FALSE.equals(c));
        /*
         * The "AutoCloseable.close() is not indempotent" problem
         * ------------------------------------------------------
         * We will need a set of objects to close without duplicated values. For example, the values associated to the
         * `ImageInputStream.class` and `DataInput.class` keys are often the same instance.  We must avoid duplicated
         * values because `ImageInputStream.close()` is not indempotent,  i.e.  invoking their `close()` method twice
         * will throw an IOException.
         *
         * Generally speaking, all AutoCloseable instances are not guaranteed to be indempotent because this is not
         * required by the interface contract. Consequently, we must be careful to not invoke the close() method on
         * the same instance twice (indirectly or indirectly). An exception to this rule is ImageInputStream, which
         * does not close its underlying stream. Those exceptions are identified by `cascadeOnClose` set to `true`.
         */
        if (!toClose.isEmpty()) {
            for (Coupled c : views.values()) {
                if (!c.cascadeOnClose() && toClose.containsKey(c.view)) {   // Keep (do not remove) the "top level" view.
                    while ((c = c.wrapperFor) != null) {
                        toClose.remove(c.view);                             // Remove all views below the "top level" one.
                        if (c.cascadeOnClose()) break;
                    }
                }
            }
        }
        views = Map.of();                   // For blocking future usage of this StorageConnector instance.
        /*
         * Now close all remaining items. Typically (but not necessarily) there is only one remaining item.
         * If an exception occurs, we will propagate it only after we are done closing all items.
         */
        DataStoreException failure = null;
        for (final AutoCloseable c : toClose.keySet()) {
            try {
                c.close();
            } catch (Exception e) {
                if (failure == null) {
                    failure = (e instanceof DataStoreException) ? (DataStoreException) e : new DataStoreException(e);
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
     * Returns whether this storage connector has been closed. If this method returns {@code true},
     * then any call to {@link #getStorageAs(Class)} will throw {@link IllegalStateException}.
     *
     * @return {@code true} if this storage connector is closed.
     *
     * @since 1.5
     */
    public final boolean isClosed() {
        return views != null && views.isEmpty();
    }

    /**
     * Returns the cause of given exception if it exists, or the exception itself otherwise.
     * This method is invoked in the {@code catch} block of a {@code try} block invoking
     * {@link ImageIO#createImageInputStream(Object)} or
     * {@link ImageIO#createImageOutputStream(Object)}.
     *
     * <h4>Rational</h4>
     * As of Java 18, above-cited methods systematically catch all {@link IOException}s and wrap
     * them in an {@link IIOException} with <q>Cannot create cache file!</q> error message.
     * This is conform to Image I/O specification but misleading if the stream provider throws an
     * {@link IOException} for another reason. Even when the failure is really caused by a problem
     * with cache file, we want to propagate the original exception to user because its message
     * may tell that there is no space left on device or no write permission.
     *
     * @see org.apache.sis.storage.image.FormatFinder#unwrap(IIOException)
     */
    @Workaround(library = "JDK", version = "18")
    private static IOException unwrap(final IIOException e) {
        final Throwable cause = e.getCause();
        return (cause instanceof IOException) ? (IOException) cause : e;
    }

    /**
     * Returns a string representation of this {@code StorageConnector} for debugging purpose.
     * This string representation is for diagnostic and may change in any future version.
     *
     * @return a string representation of this {@code StorageConnector} for debugging purpose.
     */
    @Override
    public String toString() {
        if (isClosed()) {
            return Resources.format(Resources.Keys.ClosedStorageConnector);
        }
        final var table = new DefaultTreeTable(TableColumn.NAME, TableColumn.VALUE);
        final TreeTable.Node root = table.getRoot();
        root.setValue(TableColumn.NAME,  Classes.getShortClassName(this));
        root.setValue(TableColumn.VALUE, getStorageName());
        if (options != null) {
            final TreeTable.Node op = root.newChild();
            op.setValue(TableColumn.NAME,  "options");
            op.setValue(TableColumn.VALUE,  options);
        }
        final Coupled c = getView(null);
        if (c != null) {
            c.append(root.newChild(), views);
        }
        return table.toString();
    }
}
