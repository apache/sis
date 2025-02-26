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
package org.apache.sis.storage.isobmff.base;

import java.io.IOException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.storage.isobmff.ByteReader;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.Box;


/**
 * The data of metadata item.
 * Because the amount of data may be large, this implementation does not read data immediately.
 * Instead, data are read when the {@link #readBytes readBytes(…)} method is invoked.
 *
 * <h4>Container</h4>
 * The container can be a {@code Meta} box.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public class ItemData extends Box implements ByteReader {
    /**
     * Numerical representation of the {@code "idat"} box type.
     */
    public static final int BOXTYPE = ((((('i' << 8) | 'd') << 8) | 'a') << 8) | 't';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public int type() {
        return BOXTYPE;
    }

    /**
     * Position in the stream of the first byte.
     */
    @Interpretation(Type.UNSIGNED)
    public final long payloadOffset;

    /**
     * Number of bytes to read.
     */
    @Interpretation(Type.UNSIGNED)
    public final long size;

    /**
     * Creates a new box. Contrarily to other constructors,
     * this constructor does not read the payload immediately.
     *
     * @param  reader  the reader from which to read the fields.
     * @throws IOException if an error occurred while reading the fields.
     */
    public ItemData(final Reader reader) throws IOException {
        payloadOffset = reader.input.getStreamPosition();
        size = reader.endOfCurrentBox() - payloadOffset;
    }

    /**
     * Converts an offset relative to the data of this item to an offset relative to the origin of the input stream.
     * This method updates the {@link FileRegion#offset} value in-place by adding the stream position of the first
     * byte of the data stored in this item.
     *
     * @param  request  the input stream, offset and length of the region to read. Modified in-place by this method.
     * @throws ArithmeticException if an integer overflow occurred.
     */
    @Override
    public void resolve(final FileRegion request) {
        ArgumentChecks.ensureBetween("offset", 0, size - Math.max(0, request.length), request.offset);
        if (request.length > size || request.length < 0) {
            request.length = size;
        }
        request.offset = Math.addExact(payloadOffset, request.offset);
    }
}
