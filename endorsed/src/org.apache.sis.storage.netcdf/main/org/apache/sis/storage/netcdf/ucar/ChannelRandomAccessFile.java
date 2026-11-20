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
package org.apache.sis.storage.netcdf.ucar;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.channels.SeekableByteChannel;
import ucar.unidata.io.RandomAccessFile;


/**
 * A UCAR random access file reading bytes from an arbitrary {@link Path}.
 * The UCAR library can open only local files or files accessible by HTTP.
 * This class allows the UCAR library to read files on other file systems,
 * for example S3, by delegating the read operations to a Java NIO channel.
 * This is used for netCDF 4 files, which are decoded by the UCAR library.
 *
 * @author  Quentin Bialota (Geomatys)
 */
final class ChannelRandomAccessFile extends RandomAccessFile {
    /**
     * The channel from which to read bytes.
     */
    private final SeekableByteChannel channel;

    /**
     * Creates a new random access file for the given path.
     *
     * @param  path  the file to read.
     * @throws IOException if the channel cannot be opened.
     */
    ChannelRandomAccessFile(final Path path) throws IOException {
        super(defaultBufferSize);
        channel  = Files.newByteChannel(path);
        location = path.toString();
    }

    /**
     * Returns the length of the file in bytes.
     *
     * @return the file length in bytes.
     * @throws IOException if an error occurred while fetching the file size.
     */
    @Override
    public long length() throws IOException {
        return channel.size();
    }

    /**
     * Reads up to {@code len} bytes starting at the given position in the file.
     * This method blocks until the requested number of bytes has been read,
     * or until the end of file has been reached.
     *
     * @param  pos     position in the file of the first byte to read.
     * @param  b       the array where to store the bytes.
     * @param  offset  index in the array of the first byte to write.
     * @param  len     maximal number of bytes to read.
     * @return number of bytes read, or -1 if the given position is at the end of file.
     * @throws IOException if an error occurred while reading the bytes.
     */
    @Override
    protected int read_(final long pos, final byte[] b, final int offset, final int len) throws IOException {
        final ByteBuffer buffer = ByteBuffer.wrap(b, offset, len);
        channel.position(pos);
        while (buffer.hasRemaining()) {
            if (channel.read(buffer) < 0) {
                final int n = buffer.position() - offset;
                return (n != 0) ? n : -1;
            }
        }
        return len;
    }

    /**
     * Closes this random access file and the underlying channel.
     *
     * @throws IOException if an error occurred while closing the channel.
     */
    @Override
    public synchronized void close() throws IOException {
        try {
            super.close();
        } finally {
            channel.close();
        }
    }
}
