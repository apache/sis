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
import org.apache.sis.internal.storage.Resources;


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
 * @since   0.8
 * @version 0.8
 * @module
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
        super();
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
     * Creates a localized exception with a default message saying that the stream is read-once.
     *
     * @param locale   the locale of the message to be returned by {@link #getLocalizedMessage()}, or {@code null}.
     */
    public ForwardOnlyStorageException(final Locale locale) {
        super(locale, Resources.Keys.StreamIsReadOnce);
    }
}
