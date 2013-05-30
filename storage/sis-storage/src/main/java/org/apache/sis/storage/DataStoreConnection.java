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
import javax.imageio.stream.ImageInputStream;
import org.apache.sis.internal.storage.ChannelImageInputStream;
import org.apache.sis.internal.storage.IOUtilities;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * Information for creating a connection to a {@link DataStore} in read and/or write mode.
 * {@code DataStoreConnection} wraps an input {@link Object}, which can be any of the following types:
 *
 * <ul>
 *   <li>A {@link java.nio.file.Path} or a {@link java.io.File} or file or a directory in a file system.</li>
 *   <li>A {@link java.net.URI} or a {@link java.net.URL} to a distant resource.</li>
 *   <li>A {@link java.nio.channels.Channel} or a {@link DataInput}.</li>
 *   <li>A {@link javax.sql.DataSource} or a {@link java.sql.Connection} to a JDBC database.</li>
 *   <li>Any other {@code DataStore}-specific object, for example {@link ucar.nc2.NetcdfFile}.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public class DataStoreConnection implements Serializable {
    /**
     * The input given at construction time.
     */
    private final Object input;

    /**
     * A name for the input, or {@code null} if none.
     * This field is initialized only when first needed.
     */
    private transient String name;

    /**
     * The filename extension, or {@code null} if none.
     * This field is initialized only when first needed.
     */
    private transient String extension;

    /**
     * The input as a data input stream, or {@code null} if none.
     * This field is initialized together with {@link #asByteBuffer} when first needed.
     *
     * <p>Unless the {@link #input} is already an instance of {@link DataInput}, this field will be given an instance
     * of {@link ChannelImageInputStream}, not an arbitrary stream.  In particular, we do <strong>not</strong> invoke
     * the {@link javax.imageio.ImageIO#createImageInputStream(Object)} factory method because some SIS data stores
     * will want to access the channel and buffer directly.</p>
     *
     * @see #asDataInput()
     */
    private transient DataInput asDataInput;

    /**
     * A read-only view of the buffer over the first bytes of the stream, or {@code null} if none.
     * This field is initialized together with {@link #asDataInput} when first needed.
     *
     * @see #asByteBuffer()
     */
    private transient ByteBuffer asByteBuffer;

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
     * Creates a new data store connection wrapping the given input object.
     * The input can be of any type, but the class javadoc lists the most typical ones.
     *
     * @param input The input as a URL, file, image input stream, <i>etc.</i>.
     */
    public DataStoreConnection(final Object input) {
        ArgumentChecks.ensureNonNull("input", input);
        this.input = input;
    }

    /**
     * Returns the input object given at construction time.
     * The input can be of any type, but the class javadoc lists the most typical ones.
     *
     * @return The input as a URL, file, image input stream, <i>etc.</i>.
     */
    public Object getInput() {
        return input;
    }

    /**
     * Returns the input name, or the {@linkplain Class#getSimpleName() simple class name} if this method can not infer
     * a name from the input. If the input is a {@link java.nio.file.Path}, {@link java.io.File}, {@link java.net.URL}
     * or {@link java.net.URI}, then the default implementation uses dedicated API like {@link Path#getFileName()}.
     * If the input is a {@link CharSequence}, then the default implementation gets a string representation of
     * the input and returns the part after the last {@code '/'} or platform-dependent name separator character,
     * if any.
     *
     * @return The input name, or simple class name if the input is an object of unknown type.
     */
    public String getInputName() {
        if (name == null) {
            name = IOUtilities.filename(input);
            if (name == null) {
                name = input.getClass().getSimpleName();
            }
        }
        return name;
    }

    /**
     * Returns the filename extension of the input. The default implementation recognizes the same input types
     * than the {@link #getInputName()} method. If no extension is found, this method returns an empty string.
     * If the input is an object of unknown type, this method return {@code null}.
     *
     * @return The extension, or an empty string if none, or {@code null} if the input is an object of unknown type.
     */
    public String getFileExtension() {
        if (extension == null) {
            extension = IOUtilities.extension(input);
        }
        return extension;
    }

    /**
     * Returns the input as a {@link DataInput} if possible, or {@code null} otherwise.
     * The default implementation performs the following choice:
     *
     * <ul>
     *   <li>If the input is already an instance of {@link DataInput}, then it is returned unchanged.
     *       This include the {@link ImageInputStream} and {@link javax.imageio.stream.ImageOutputStream} cases,
     *       which are {@code DataInput} sub-interfaces.</li>
     *
     *   <li>Otherwise if the input is an instance of one of the types enumerated in the class javadoc
     *       (except {@code DataStore}-specific types), then an {@link ImageInputStream} is created when first needed
     *       and returned. Multiple invocations of this method on the same {@code DataStoreConnection} instance will
     *       return the same {@code ImageInputStream} instance.</li>
     *
     *   <li>Otherwise this method returns {@code null}.</li>
     * </ul>
     *
     * @return The input as a {@link DataInput}, or {@code null} if the input is an object of unknown type.
     * @throws IOException If an error occurred while opening a stream for the input.
     */
    public DataInput asDataInput() throws IOException {
        if (!isInitialized) {
            initialize();
        }
        return asDataInput;
    }

    /**
     * Returns a read-only view of the buffer over the first bytes of the stream, or {@code null} if unavailable.
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
     * Initializes the {@link #asDataInput} and {@link #asByteBuffer} fields.
     * Note that some or all of those fields may still be null after this method call.
     *
     * @see #rewind()
     */
    private void initialize() throws IOException {
        if (input instanceof DataInput) {
            asDataInput = (DataInput) input;
        } else {
            /*
             * Creates a ChannelImageInputStream instance. We really need that specific type - do NOT use
             * the ImageIO.createImageInputStream(Object) method - because some SIS data stores will want
             * to access directly the channel and the buffer.
             */
            final ReadableByteChannel channel = IOUtilities.open(input, null);
            if (channel != null) {
                final ByteBuffer buffer = ByteBuffer.allocate(4096);
                asDataInput = new ChannelImageInputStream(getInputName(), channel, buffer, false);
                asByteBuffer = buffer.asReadOnlyBuffer();
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
     * @throws IOException If the stream is open but can not be rewinded.
     */
    public void rewind() throws IOException {
        /*
         * Restores the ImageInputStream to its original position if possible. Note that in
         * the ChannelImageInputStream, this may reload the buffer content if necessary.
         */
        if (asDataInput instanceof ImageInputStream) try {
            ((ImageInputStream) asDataInput).seek(streamOrigin);
        } catch (IndexOutOfBoundsException e) {
            throw new IOException(Errors.format(Errors.Keys.StreamIsForwardOnly_1, getInputName()), e);
        }
        /*
         * If the buffer is non-null, then the way we implemented 'initialize()' guarantees that
         * 'asDataInput' is an instance of ChannelImageInputStream. So we copy the position and
         * limits from the buffer. Note that this copy must be performed after the above 'seek',
         * because the seek operation may have modified the buffer position and limit.
         */
        if (asByteBuffer != null) {
            final ByteBuffer buffer = ((ChannelImageInputStream) asDataInput).buffer;
            asByteBuffer.clear().limit(buffer.limit()).position(buffer.position());
        }
    }

    /**
     * Returns a string representation of this {@code DataStoreConnection} for debugging purpose.
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(40);
        buffer.append(getClass().getSimpleName()).append("[“").append(getInputName());
        // TODO: more info here.
        return buffer.append("”]").toString();
    }
}
