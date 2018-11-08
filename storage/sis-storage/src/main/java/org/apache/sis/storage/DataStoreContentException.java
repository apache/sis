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
 * @version 0.8
 * @since   0.8
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
     * Creates an exception with the specified cause and no details message.
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

    /**
     * Creates a localized exception with a message saying that the given store can not be read.
     * Location in the file where the error occurred while be fetched from the given {@code store}
     * argument if possible. If the given store is not recognized, then it will be ignored.
     *
     * <p>Examples of messages created by this constructor:</p>
     * <ul>
     *   <li>Can not read <var>“Foo”</var> as a file in the <var>Bar</var> format.</li>
     *   <li>Can not read after column 10 or line 100 of <var>“Foo”</var> as part of a file in the <var>Bar</var> format.</li>
     * </ul>
     *
     * @param locale    the locale of the message to be returned by {@link #getLocalizedMessage()}, or {@code null}.
     * @param format    short name or abbreviation of the data format (e.g. "CSV", "GML", "WKT", <i>etc</i>).
     * @param filename  name of the file or data store where the error occurred.
     * @param store     the input or output object from which to get the current position, or {@code null} if none.
     *                  This can be a {@link java.io.LineNumberReader} or {@link javax.xml.stream.XMLStreamReader}
     *                  for example.
     */
    public DataStoreContentException(Locale locale, String format, String filename, Object store) {
        super(locale, format, filename, store);
    }
}
