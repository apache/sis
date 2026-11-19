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
package org.apache.sis.storage.hdf.io;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import org.apache.sis.storage.hdf.filter.Filter;

/**
 * Input stream over the decoded content of a chunk.
 * <p>
 * The stored bytes are read and the filters reversed lazily, on the first
 * read operation; the whole decoded chunk is then kept in memory.
 *
 * @author Johann Sorel (Geomatys)
 */
public class ChunkInputStream extends InputStream {

    private final HDF5DataInput channel;
    private final long address;
    private final long compressedSize;
    private long uncompressedSize = -1;
    private final Filter[] filters;
    private InputStream back;
    private boolean finished = false;

    /**
     * Creates a stream over the chunk stored at the given address.
     *
     * @param channel file channel
     * @param address file address of the stored chunk
     * @param compressedSize stored size in bytes
     * @param filters applied in reversed order on the chunk datas.
     */
    public ChunkInputStream(HDF5DataInput channel, long address, long compressedSize, Filter... filters) {
        this.channel = channel;
        this.address = address;
        this.compressedSize = compressedSize;
        this.filters = filters;
    }

    /**
     * Returns the size in bytes of the decoded chunk.
     * If there are filters, this requires reading and decoding the chunk.
     *
     * @return the size in bytes of the decoded chunk
     * @throws IOException if an error occurred while reading or decoding the chunk.
     */
    public long getUncompressedSize() throws IOException {
        if (uncompressedSize == -1) {
            if (filters.length == 0) {
                uncompressedSize = compressedSize;
            } else {
                //no other choice then to decompress data
                getBack();
            }
        }
        return uncompressedSize;
    }

    private InputStream getBack() throws IOException {
        if (back == null) {
            channel.seek(address);
            byte[] array = channel.readNBytes(Math.toIntExact(compressedSize));
            //apply filters
            for (int i = filters.length - 1; i >= 0; i--) {
                array = filters[i].decode(array);
            }
            uncompressedSize = array.length;
            back = new ByteArrayInputStream(array);
        }
        return back;
    }

    @Override
    public int read() throws IOException {
        if (finished) {
            return -1;
        }
        return getBack().read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        if (finished) {
            return -1;
        }
        return getBack().read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (finished) {
            return -1;
        }
        return getBack().read(b, off, len);
    }

    @Override
    public byte[] readAllBytes() throws IOException {
        if (finished) {
            return new byte[0];
        }
        return getBack().readAllBytes();
    }

    @Override
    public byte[] readNBytes(int len) throws IOException {
        if (finished) {
            return new byte[0];
        }
        return getBack().readNBytes(len);
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        if (finished) {
            return 0;
        }
        return getBack().readNBytes(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        if (back == null & n >= getUncompressedSize()) {
            finished = true;
        }
        return getBack().skip(n);
    }

    @Override
    public void skipNBytes(long n) throws IOException {
        if (finished) {
            throw new EOFException();
        }
        getBack().skipNBytes(n);
    }

    @Override
    public int available() throws IOException {
        if (finished) {
            return 0;
        }
        return getBack().available();
    }

    @Override
    public void close() throws IOException {
        finished = true;
        if (back != null) {
            getBack().close();
        }
    }
}
