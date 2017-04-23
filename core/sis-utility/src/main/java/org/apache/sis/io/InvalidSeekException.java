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
package org.apache.sis.io;

import java.io.IOException;


/**
 * Thrown when an input stream or a channel can not modify its position to the given value.
 * If may be because the given position is after the end of file, but not necessarily.
 * This exception may also be thrown by implementations that cache a portion of a file
 * and can only seek inside that portion.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 *
 * @see java.nio.channels.SeekableByteChannel#position(long)
 * @see java.io.InputStream#reset()
 * @see javax.imageio.stream.ImageInputStream#reset()
 *
 * @since 0.8
 * @module
 */
public class InvalidSeekException extends IOException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -3327667930906602606L;

    /**
     * Constructs a new exception with no message.
     */
    public InvalidSeekException() {
        super();
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message  the detail message, or {@code null} if none.
     */
    public InvalidSeekException(final String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message  the detail message, or {@code null} if none.
     * @param cause    the cause, or {@code null} if none.
     */
    public InvalidSeekException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
