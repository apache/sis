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
package org.apache.sis.internal.storage.io;

import java.io.IOException;
import java.nio.InvalidMarkException;


/**
 * Stream reader or writer capable to mark its current position and reset to that position later.
 * The stream shall support nested marks.
 *
 * <div class="note"><b>Use case:</b>
 * this interface can be used when we need to move to a previously marked position, but we do not know how many nested
 * {@code mark()} method calls may have been performed (typically because the stream has been used by arbitrary code).
 * We can compare {@link #getStreamPosition()} value after {@link #reset()} method calls with the expected position.
 * </div>
 *
 * <div class="note"><b>Design note:</b>
 * an alternative could be to support the {@code seek(long)} method. But using marks instead allows the stream
 * to invalidate the marks if needed (for example when {@link ChannelData#setStreamPosition(long)} is invoked).
 * </div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public interface Markable {
    /**
     * Returns the current byte position of the stream.
     *
     * @return the position of the stream.
     * @throws IOException if the position can not be obtained.
     */
    long getStreamPosition() throws IOException;

    /**
     * Pushes the current stream position onto a stack of marked positions.
     * A subsequent call to the {@link #reset()} method repositions this stream
     * at the last marked position so that subsequent reads re-read the same bytes.
     * Calls to {@code mark()} and {@code reset()} can be nested arbitrarily.
     *
     * @throws IOException if this stream can not mark the current position.
     */
    void mark() throws IOException;

    /**
     * Resets the current stream byte and bit positions from the stack of marked positions.
     * An {@code IOException} may be be thrown if the previous marked position lies in the
     * discarded portion of the stream.
     *
     * @throws InvalidMarkException if there is no mark.
     * @throws IOException if a mark was defined but this stream can not move to that position.
     */
    void reset() throws IOException;
}
