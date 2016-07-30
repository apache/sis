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


/**
 * Thrown when a store can not be read because the stream contains invalid data.
 * It may be for example a logical inconsistency, or a reference not found,
 * or an unsupported file format version, <i>etc.</i>
 *
 * <div class="note"><b>Note:</b>
 * exceptions that are caused by {@link java.io.IOException} or {@link java.sql.SQLException}
 * should generally be wrapped by another type of {@link DataStoreException}, unless the data
 * store can determine that the error was caused by a problem with the stream content rather
 * than some I/O problems.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public class DataStoreContentException extends DataStoreException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3469934460013440211L;

    /**
     * Creates an exception with the specified details message.
     *
     * @param message  the detail message.
     */
    public DataStoreContentException(String message) {
        super(message);
    }

    /**
     * Creates an exception with the specified cause.
     *
     * @param cause  the cause for this exception.
     */
    public DataStoreContentException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates an exception with the specified details message and cause.
     *
     * @param message  the detail message.
     * @param cause    the cause for this exception.
     */
    public DataStoreContentException(String message, Throwable cause) {
        super(message, cause);
    }
}
