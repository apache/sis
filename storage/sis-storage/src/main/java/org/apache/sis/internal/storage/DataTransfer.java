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
package org.apache.sis.internal.storage;

import java.io.IOException;
import java.nio.Buffer;
import org.apache.sis.util.Debug;


/**
 * Transfers data from a buffer to an array specified at construction time.
 * The kind of buffer and the primitive type in the array depend on the implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
interface DataTransfer {
    /**
     * Returns a file identifier for error messages or debugging purpose.
     */
    @Debug
    String filename();

    /**
     * Returns the size of the Java primitive type which is the element of the array.
     * The size is expressed as the number of bits to shift.
     */
    int dataSizeShift();

    /**
     * Returns the data as a {@code char[]}, {@code short[]}, {@code int[]}, {@code long[]},
     * {@code float[]} or {@code double[]} array. This is either the array given in argument
     * to the subclass constructor, or the array created by {@link #createArray(int)}.
     */
    Object dataArray();

    /**
     * Creates a destination array of the given length.
     */
    void createDataArray(int length);

    /**
     * Sets the destination to the given data array, which may be {@code null}.
     */
    void setDest(Object array) throws ClassCastException;

    /**
     * Returns the view created by the last call to {@link #createView()}, or {@code null} if none.
     */
    Buffer view();

    /**
     * Creates a new buffer of the type required by the array to fill.
     * This method is guaranteed to be invoked exactly once, after the
     * {@link ChannelDataInput#buffer} contains enough data.
     */
    Buffer createView();

    /**
     * Moves to the given position in the stream.
     *
     * @param  position The position where to move.
     * @throws IOException if the stream can not be moved to the given position.
     */
    void seek(long position) throws IOException;

    /**
     * Reads {@code length} values from the stream and stores them into the array known to subclass,
     * starting at index {@code offset}.
     *
     * <p>If a non-null {@code Buffer} is given in argument to this method, then it must be a view over
     * the full content of {@link ChannelDataInput#buffer} (i.e. the view element at index 0 shall be
     * defined by the buffer elements starting at index 0).</p>
     *
     * @param  view     Existing buffer to use as a view over {@link ChannelDataInput#buffer}, or {@code null}.
     * @param  offset   The starting position within {@code dest} to write.
     * @param  length   The number of values to read.
     * @throws IOException if an error (including EOF) occurred while reading the stream.
     */
    void readFully(Buffer view, int offset, int length) throws IOException;
}
