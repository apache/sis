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
package org.apache.sis.io.wkt;

import org.opengis.referencing.IdentifiedObject;


/**
 * Thrown by {@link FormattableObject#toWKT()} when an object can not be formatted as WKT.
 * A formatting may fail because an object contains properties which can not be represented
 * by the standard WKT elements.
 *
 * <div class="note"><b>Example:</b>
 * An engineering CRS can not be represented in the WKT 1 format if all axes do not use the same
 * unit of measurement. However such CRS can be represented in the WKT 2 format.</div>
 *
 * This exception may also be thrown by {@link Formatter} if the object given to an
 * {@code append(…)} method is an instance of an unsupported class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 *
 * @see Formatter#setInvalidWKT(IdentifiedObject, Exception)
 */
public class UnformattableObjectException extends UnsupportedOperationException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3623766455562385536L;

    /**
     * Constructs a new exception with no message.
     */
    public UnformattableObjectException() {
        super();
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message The detail message, or {@code null} if none.
     */
    public UnformattableObjectException(final String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message The detail message, or {@code null} if none.
     * @param cause The cause, or {@code null} if none.
     */
    public UnformattableObjectException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the specified cause.
     *
     * @param cause The cause, or {@code null} if none.
     */
    public UnformattableObjectException(final Throwable cause) {
        super(cause);
    }
}
