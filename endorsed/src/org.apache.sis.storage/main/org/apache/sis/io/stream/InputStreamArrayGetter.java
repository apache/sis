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
package org.apache.sis.io.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.function.Supplier;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.storage.base.StoreUtilities;


/**
 * A hack for getting the backing array of an input stream if that array exists.
 * This class searches the array in the following locations:
 *
 * <ul>
 *   <li>{@link ByteArrayInputStream#buf} together with offset and length.
 *       Those fields have {@code protected} access, so they are committed Java API.
 *       However as of Java 19, there is no public accessor for them.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.4
 */
public final class InputStreamArrayGetter extends OutputStream {
    /**
     * The buffer wrapping the array, or {@code null} if unknown.
     * This buffer is not read-only, but nevertheless should not be modified.
     */
    private ByteBuffer buffer;

    /**
     * Creates a pseudo output stream to use as a trick for getting the backing array.
     */
    private InputStreamArrayGetter() {
    }

    /**
     * Whether it is hopefully safe to apply the trick used by this class for getting the backing array.
     * The trick is that the {@link ByteArrayInputStream#transferTo(OutputStream)} implementation gives
     * the backing array in argument in a call to {@link InputStream#write(byte[], int, int)} method.
     * This is verified by looking at the OpenJDK source code, and presumed relatively stable because
     * the code uses only protected fields and public methods, which are committed API.
     * But because users could override the {@code transferTo(…)} method in a subclass,
     * we rely on this strick only if the method implementation is the one provided by
     * the standard {@link ByteArrayInputStream} class.
     *
     * @param  input  the input stream for which to verify the implementation class.
     * @return whether the implementation is the OpenJDK one.
     */
    private static boolean isKnownImplementation(final InputStream input) {
        try {
            return input.getClass().getMethod("transferTo", OutputStream.class).getDeclaringClass() == ByteArrayInputStream.class;
        } catch (NoSuchMethodException e) {
            // Should not happen because we requested a method which should exist.
            Logging.unexpectedException(StoreUtilities.LOGGER, InputStreamArrayGetter.class, "isKnownImplementation", e);
            return false;
        }
    }

    /**
     * Creates a new data input for the given input stream.
     * If the input stream is backed by an array, the array will be wrapped in a read-only mode.
     *
     * @param  filename  a short identifier (typically a filename without path) used for formatting error message.
     * @param  input     the input stream from where data are read.
     * @param  bs        supplier of the buffer where to copy the data.
     * @return the data input using a readable channel.
     * @throws IOException if an error occurred while reading the input stream.
     */
    public static ChannelDataInput channel(final String filename, final InputStream input, final Supplier<ByteBuffer> bs)
            throws IOException
    {
        if (isKnownImplementation(input)) {
            final InputStreamArrayGetter getter = new InputStreamArrayGetter();
            input.transferTo(getter);
            if (getter.buffer != null) {
                return new ChannelDataInput(filename, getter.buffer);
            }
        }
        final ReadableByteChannel channel = Channels.newChannel(input);
        return new ChannelDataInput(filename, channel, bs.get(), false);
    }

    /**
     * Invoked by {@link ByteArrayInputStream#transferTo(OutputStream)}.
     * We use this method as a callback for getting the underlying array.
     *
     * @param  array   the data.
     * @param  offset  the start offset in the array.
     * @param  length  the number of valid bytes in the array.
     * @throws IOException if this method is invoked more than once.
     */
    @Override
    public void write(final byte[] array, final int offset, final int length) throws IOException {
        if (buffer == null) {
            buffer = ByteBuffer.wrap(array, offset, length);
        } else {
            super.write(array, offset, length);
        }
    }

    /**
     * Should never be invoked. If this method is nevertheless invoked,
     * then the {@link ByteArrayInputStream#transferTo(OutputStream)}
     * implementation is not what we expected.
     *
     * @param  b  ignored.
     * @throws IOException always thrown.
     */
    @Override
    public void write(int b) throws IOException {
        throw new IOException("Unexpected implementation.");
    }
}
