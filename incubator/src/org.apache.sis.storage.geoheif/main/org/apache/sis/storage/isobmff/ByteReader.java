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
package org.apache.sis.storage.isobmff;

import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.UnsupportedEncodingException;


/**
 * Interface implemented by {@link TreeNode} which read bytes only when requested.
 * Contrarily to {@link Box} constructors which are read all the payload immediately,
 * the method provided by this interface reads only a subset of a potentially large
 * array of bytes.
 *
 * @author Martin Desruisseaux (Geomatys)
 */
public interface ByteReader {
    /**
     * Converts an offset relative to the data of this item to an offset relative to the origin of the input stream.
     * This method updates the {@link FileRegion#offset} value in-place, typically by adding the stream position of
     * the first byte of the data stored in this item. The {@link FileRegion#input} is usually unmodified, but this
     * method may also replace it by a temporary instance if the bytes to read are spread in different regions of the file.
     *
     * @param  request  the input stream, offset and length of the region to read. Modified in-place by this method.
     * @throws UnsupportedEncodingException if this item uses an unsupported construction method.
     * @throws DataStoreContentException if the <abbr>HEIF</abbr> file is malformed.
     * @throws DataStoreException if another logical error occurred.
     * @throws ArithmeticException if an integer overflow occurred.
     */
    void resolve(FileRegion request) throws DataStoreException;

    /**
     * A sub-region of the item to read. Instances of this class are given to {@link #resolve(FileRegion)}
     * and modified in-place for converting values relative to the item to values relative to the stream.
     */
    static final class FileRegion {
        /**
         * The input stream from which to read the bytes. After the call to {@link #resolve(FileRegion)},
         * it may be a temporary instance providing a view of distinct extents as if they were a single
         * stream of consecutive bytes.
         */
        public ChannelDataInput input;

        /**
         * Offset of the first byte to read. Before the call to {@link #resolve(FileRegion)},
         * this offset is relative to the data stored by the item (i.e., offset zero is the
         * first byte stored by the item). After the call to {@code resolve(FileRegion)},
         * the offset is relative to the origin of {@link #input}.
         */
        public long offset;

        /**
         * Maximum number of bytes to read, starting at the offset, or a negative value for reading all.
         * After the call to {@link #resolve(FileRegion)}, should be updated to a value not greater than
         * the number of bytes actually available.
         */
        public long length;

        /**
         * Creates an initially empty file region.
         */
        public FileRegion() {
        }

        /**
         * Skips the given number of bytes at the beginning.
         * This method increments {@link #offset} and decrements {@link #length} by the given amount.
         *
         * @param  bytes  the number of bytes to skip at the beginning.
         */
        public void skip(final long bytes) {
            offset  = Math.addExact(offset, bytes);
            length -= bytes;
        }
    }
}
