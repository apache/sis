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

import java.util.Locale;
import java.nio.file.OpenOption;
import org.apache.sis.internal.storage.Resources;
import org.apache.sis.internal.storage.io.IOUtilities;


/**
 * Thrown when an operation would require to move the cursor back, but the underlying storage does not allow that.
 * For example this exception is thrown if the user wants to read the same data a second time, but the underlying
 * {@linkplain java.nio.channels.ReadableByteChannel} is not
 * {@linkplain java.nio.channels.SeekableByteChannel seekable}.
 *
 * <p>This exception typically does not depend on the {@link DataStore} implementation, but rather on the
 * {@link StorageConnector} value given to the data store.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 *
 * @see ReadOnlyStorageException
 */
public class ForwardOnlyStorageException extends DataStoreException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5750925701319201321L;

    /**
     * Creates an exception with no cause and no details message.
     */
    public ForwardOnlyStorageException() {
    }

    /**
     * Creates an exception with the specified details message.
     *
     * @param message  the detail message.
     */
    public ForwardOnlyStorageException(final String message) {
        super(message);
    }

    /**
     * Creates an exception with the specified details message and cause.
     *
     * @param message  the detail message in the default locale.
     * @param cause    the cause for this exception.
     */
    public ForwardOnlyStorageException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a localized exception with a default message saying that the stream is read-once or write-once.
     *
     * @param locale    the locale of the message to be returned by {@link #getLocalizedMessage()}, or {@code null}.
     * @param filename  name of the file or data store where the error occurred.
     * @param options   the option used for opening the file, or {@code null} or empty if unknown.
     *                  This method looks in particular for {@link java.nio.file.StandardOpenOption#READ} and
     *                  {@code WRITE} options for inferring if the data store was used as a reader or as a writer.
     */
    public ForwardOnlyStorageException(final Locale locale, final String filename, final OpenOption... options) {
        super(locale, IOUtilities.isWrite(options) ? Resources.Keys.StreamIsWriteOnce_1
                                                   : Resources.Keys.StreamIsReadOnce_1, filename);
    }
}
