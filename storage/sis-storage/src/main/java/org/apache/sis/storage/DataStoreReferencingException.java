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
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;


/**
 * Thrown when a data store failed to construct the coordinate reference system (CRS)
 * or other positioning information. This exception is typically (but not necessarily)
 * caused by {@link FactoryException} or {@link TransformException}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public class DataStoreReferencingException extends DataStoreException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2671737996817267335L;

    /**
     * Creates an exception with the specified details message.
     *
     * @param message  the detail message.
     */
    public DataStoreReferencingException(final String message) {
        super(message);
    }

    /**
     * Creates an exception with the specified cause and no details message.
     * The given cause should (but is not required to) be a {@link FactoryException} or {@link TransformException}.
     *
     * @param cause  the cause for this exception.
     */
    public DataStoreReferencingException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates an exception with the specified details message and cause.
     * The given cause should (but is not required to) be a {@link FactoryException} or {@link TransformException}.
     *
     * @param message  the detail message.
     * @param cause    the cause for this exception.
     */
    public DataStoreReferencingException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a localized exception with a message saying that the given store can not be read.
     * Location in the file where the error occurred while be fetched from the given {@code store}
     * argument if possible. If the given store is not recognized, then it will be ignored.
     *
     * <p>This constructor should be followed by a call to {@link #initCause(Throwable)}
     * with a {@link FactoryException} or {@link TransformException} cause.</p>
     *
     * @param locale    the locale of the message to be returned by {@link #getLocalizedMessage()}, or {@code null}.
     * @param format    short name or abbreviation of the data format (e.g. "CSV", "GML", "WKT", <i>etc</i>).
     * @param filename  name of the file or data store where the error occurred.
     * @param store     the input or output object from which to get the current position, or {@code null} if none.
     *                  This can be a {@link java.io.LineNumberReader} or {@link javax.xml.stream.XMLStreamReader}
     *                  for example.
     */
    public DataStoreReferencingException(Locale locale, String format, String filename, Object store) {
        super(locale, format, filename, store);
    }
}
