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

import java.io.DataInput;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.storage.IOUtilities;
import org.apache.sis.internal.storage.ChannelImageInputStream;


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
 * This class is used only for discovery of a {@code DataStore} implementation capable to handle the input.
 * Once a suitable {@code DataStore} has been found, the {@code DataStoreConnection} instance is typically
 * discarded since each data store implementation will use their own input/output objects.
 *
 * <p>This class does not implement {@link AutoCloseable} on intend, because the connection shall not be closed
 * if it has been taken by a {@link DataStore} instance. The connection shall be closed only if no suitable
 * {@code DataStore} has been found.</p>
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
     * A read-only view of the buffer over the first bytes of the stream, or {@code null} if none.
     * This field is initialized together with {@link #asDataInput} when first needed.
     *
     * @see #asByteBuffer()
     */
    private transient ByteBuffer asByteBuffer;

    /**
     * The input as a data input stream, or {@code null} if none.
     * This field is initialized together with {@link #asByteBuffer} when first needed.
     *
     * <p>Unless the {@link #storage} is already an instance of {@link DataInput}, this field will be
     * given an instance of {@link ChannelImageInputStream} if possible rather than an arbitrary stream.
     * In particular, we invoke the {@link ImageIO#createImageInputStream(Object)} factory method only in
     * last resort because some SIS data stores will want to access the channel and buffer directly.</p>
     *
     * @see #asDataInput()
     */
    private transient DataInput asDataInput;

    /**
     * If {@link #asDataInput} is an instance of {@link ImageInputStream}, then the stream position
     * at the time the {@code asDataInput} field has been initialized. This is often zero.
     */
    private transient long streamOrigin;

    /**
     * {@code true} if {@link #asDataInput} and {@link #asByteBuffer} have been initialized.
     */
    private transient boolean isInitialized;

    /**
     * The input/output object as a JDBC connection.
     *
     * @see #asDatabase()
     */
    private transient Connection asDatabase;

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
     * Returns the input/output object given at construction time.
     * The object can be of any type, but the class javadoc lists the most typical ones.
     *
     * @return The input/output object as a URL, file, image input stream, <i>etc.</i>.
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
     * Returns a read-only view of the first bytes of the input stream, or {@code null} if unavailable.
     * If non-null, this buffer can be used for a quick check of file magic number.
     *
     * @return The first bytes in the stream (read-only), or {@code null} if unavailable.
     * @throws IOException If an error occurred while opening a stream for the input.
     */
    public ByteBuffer asByteBuffer() throws IOException {
        if (!isInitialized) {
            initialize();
        }
        return asByteBuffer;
    }

    /**
     * Returns the input as a {@link DataInput} if possible, or {@code null} otherwise.
     * The default implementation performs the following choice based on the type of the
     * {@linkplain #getStorage() storage} object:
     *
     * <ul>
     *   <li>If the storage is already an instance of {@link DataInput} (including the {@link ImageInputStream}
     *       and {@link javax.imageio.stream.ImageOutputStream} types), then it is returned unchanged.</li>
     *
     *   <li>Otherwise if the input is an instance of {@link java.nio.file.Path}, {@link java.io.File},
     *       {@link java.net.URI}, {@link java.net.URL}, {@link CharSequence}, {@link java.io.InputStream} or
     *       {@link java.nio.channels.ReadableByteChannel}, then an {@link ImageInputStream} backed by a
     *       {@link ByteBuffer} is created when first needed and returned.</li>
     *
     *   <li>Otherwise if {@link ImageIO#createImageInputStream(Object)} returns a non-null value,
     *       then this value is cached and returned.</li>
     *
     *   <li>Otherwise this method returns {@code null}.</li>
     * </ul>
     *
     * Multiple invocations of this method on the same {@code DataStoreConnection} instance will return the same
     * {@code ImageInputStream} instance.
     *
     * @return The input as a {@code DataInput}, or {@code null} if the input is an object of unknown type.
     * @throws IOException If an error occurred while opening a stream for the input.
     */
    public DataInput asDataInput() throws IOException {
        if (!isInitialized) {
            initialize();
        }
        return asDataInput;
    }

    /**
     * Returns the input as a connection to a JDBC database if possible, or {@code null} otherwise.
     * The default implementation performs the following choice based on the type of the
     * {@linkplain #getStorage() storage} object:
     *
     * <ul>
     *   <li>If the storage is already an instance of {@link Connection}, then it is returned unchanged.</li>
     *
     *   <li>Otherwise if the storage is an instance of {@link DataSource}, then a connection is obtained
     *       when first needed and returned.</li>
     *
     *   <li>Otherwise this method returns {@code null}.</li>
     * </ul>
     *
     * Multiple invocations of this method on the same {@code DataStoreConnection} instance will return the same
     * {@code Connection} instance.
     *
     * @return The storage as a {@code Connection}, or {@code null} if the storage is an object of unknown type.
     * @throws SQLException If an error occurred while opening a database connection for the storage.
     */
    public Connection asDatabase() throws SQLException {
        if (asDatabase == null) {
            if (storage instanceof Connection) {
                asDatabase = (Connection) storage;
            } else if (storage instanceof DataSource) {
                asDatabase = ((DataSource) storage).getConnection();
            }
        }
        return asDatabase;
    }

    /**
     * Initializes I/O part, namely the {@link #asDataInput} and {@link #asByteBuffer} fields.
     * Note that some or all of those fields may still be null after this method call.
     *
     * @see #rewind()
     */
    private void initialize() throws IOException {
        if (storage instanceof DataInput) {
            asDataInput = (DataInput) storage;
        } else {
            /*
             * Creates a ChannelImageInputStream instance. We really need that specific type because some
             * SIS data stores will want to access directly the channel and the buffer. We will fallback
             * on the ImageIO.createImageInputStream(Object) method only in last resort.
             */
            final ReadableByteChannel channel = IOUtilities.open(storage, null);
            if (channel != null) {
                final ByteBuffer buffer = ByteBuffer.allocate(4096);
                asDataInput = new ChannelImageInputStream(getStorageName(), channel, buffer, false);
                asByteBuffer = buffer.asReadOnlyBuffer();
            } else {
                asDataInput = ImageIO.createImageInputStream(storage);
            }
        }
        if (asDataInput instanceof ImageInputStream) {
            streamOrigin = ((ImageInputStream) asDataInput).getStreamPosition();
        }
        isInitialized = true;
    }

    /**
     * Rewinds the {@link DataInput} and {@link ByteBuffer} to the beginning of the stream.
     * This method is invoked when more than one {@link DataStore} instance is tried in search
     * for a data store that accept this {@code DataStoreInput} instance.
     *
     * <p>In the default implementation, this method does nothing if {@link #asDataInput()}
     * returns {@code null}.</p>
     *
     * @throws DataStoreException If the stream is open but can not be rewinded.
     */
    public void rewind() throws DataStoreException {
        /*
         * Restores the ImageInputStream to its original position if possible. Note that in
         * the ChannelImageInputStream, this may reload the buffer content if necessary.
         */
        if (asDataInput instanceof ImageInputStream) try {
            ((ImageInputStream) asDataInput).seek(streamOrigin);
        } catch (IOException | IndexOutOfBoundsException e) {
            throw new DataStoreException(Errors.format(Errors.Keys.StreamIsForwardOnly_1, getStorageName()), e);
        }
        /*
         * Copy the position and limits from the buffer. Note that this copy must be performed after the
         * above 'seek', because the seek operation may have modified the buffer position and limit.
         */
        if (asByteBuffer != null && asDataInput instanceof ChannelImageInputStream) {
            final ByteBuffer buffer = ((ChannelImageInputStream) asDataInput).buffer;
            asByteBuffer.clear().limit(buffer.limit()).position(buffer.position());
            asByteBuffer.order(buffer.order());
        }
    }

    /**
     * Closes all streams and connections created by this object, and closes the storage it is closeable.
     * This method closes the objects created by {@link #asDataInput()} and {@link #asDatabase()}, if any,
     * then closes the {@linkplain #getStorage() storage} if it is closeable.
     *
     * <p>This method shall be invoked <strong>only</strong> if no {@link DataStore} accepted this input.
     * Invoking this method in a {@code try} … {@code finally} block is usually not appropriate.</p>
     *
     * @throws DataStoreException If an error occurred while closing the stream or database connection.
     */
    public void close() throws DataStoreException {
        try {
            if (asDatabase != null) {
                asDatabase.close();
            }
            if (asDataInput instanceof AutoCloseable) {
                ((AutoCloseable) asDataInput).close();
                /*
                 * On JDK6, ImageInputStream does not extend Closeable and must
                 * be checked explicitely. On JDK7, this is not needed anymore.
                 */
            } else if (storage instanceof AutoCloseable) {
                /*
                 * Close only if we didn't closed 'asDataInput', because closing 'asDataInput'
                 * automatically close the 'storage' if the former is a wrapper for the later.
                 * Since AutoCloseable.close() is not guaranteed to be indempotent, we should
                 * avoid to call it (indirectly) twice.
                 */
                ((AutoCloseable) storage).close();
            }
        } catch (Exception e) {
            throw new DataStoreException(e);
        } finally {
            asDatabase   = null;
            asDataInput  = null;
            asByteBuffer = null;
        }
    }

    /**
     * Returns a string representation of this {@code DataStoreConnection} for debugging purpose.
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(40);
        buffer.append(Classes.getShortClassName(this)).append("[“").append(getStorageName());
        // TODO: more info here.
        return buffer.append("”]").toString();
    }
}
