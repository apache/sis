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

import java.io.Closeable;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import javax.imageio.stream.ImageOutputStream;


/**
 * Adds the missing methods in {@code ChannelDataOutput} for implementing the {@code DataOutput} interface.
 * Current implementation does not yet implements the {@code ImageOutputStream} sub-interface, but a future
 * implementation may do so.
 *
 * <p>We do not implement {@link ImageOutputStream} yet because the latter inherits all read operations from
 * {@code ImageInputStream}, while the {@code org.apache.sis.storage.base} package keeps the concerns
 * separated. Despite that, the name of this {@code ChannelImageOutputStream} anticipates a future version
 * which would implement the image I/O interface.</p>
 *
 * <p>This class is a place-holder for future development.</p>
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public class ChannelImageOutputStream extends ChannelDataOutput implements DataOutput, Closeable {
    /**
     * Creates a new output stream for the given channel and using the given buffer.
     *
     * @param  filename  a file identifier used only for formatting error message.
     * @param  channel   the channel where to write data.
     * @param  buffer    the buffer from where to read the data.
     * @throws IOException if an error occurred while writing into channel.
     */
    public ChannelImageOutputStream(final String filename, final WritableByteChannel channel, final ByteBuffer buffer)
            throws IOException
    {
        super(filename, channel, buffer);
    }

    /**
     * Creates a new output source from the given {@code ChannelDataOutput}.
     * This constructor is invoked when we need to change the implementation class
     * from {@code ChannelDataOutput} to {@code ChannelImageOutputStream}.
     *
     * @param  output  the existing instance from which to takes the channel and buffer.
     * @throws IOException if an error occurred while writing into channel.
     */
    public ChannelImageOutputStream(final ChannelDataOutput output) throws IOException {
        super(output.filename, output.channel, output.buffer);
    }

    /**
     * Closes the {@linkplain #channel}.
     *
     * @throws IOException if an error occurred while closing the channel.
     */
    @Override
    public final void close() throws IOException {
        channel.close();
    }
}
