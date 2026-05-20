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
package org.apache.sis.referencing.factory;

import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.NoninvertibleTransformException;


/**
 * Thrown when an internal error occurred in a {@code Factory} implementation.
 * This error is not necessarily caused by a malformed <abbr>CRS</abbr> or other geodetic object.
 * It is more likely caused by a bug in the implementation, for example a
 * {@linkplain NoninvertibleTransformException non invertible transform}
 * in a context where this error should not have occurred.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.7
 *
 * @see org.apache.sis.storage.InternalDataStoreException
 *
 * @since 1.7
 */
public class InternalFactoryException extends FactoryException {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 2987973777167896560L;

    /**
     * Construct an exception with no detail message.
     */
    public InternalFactoryException() {
    }

    /**
     * Construct an exception with the specified detail message.
     *
     * @param  message  the detail message, saved for later retrieval by the {@link #getMessage()} method.
     */
    public InternalFactoryException(String message) {
        super(message);
    }

    /**
     * Constructs an exception with the specified cause.
     *
     * @param cause  the cause, saved for later retrieval by the {@link #getCause()} method.
     *
     * @since 1.2
     */
    public InternalFactoryException(Throwable cause) {
        super(cause);
    }

    /**
     * Construct an exception with the specified detail message and cause.
     *
     * @param  message  the detail message, saved for later retrieval by the {@link #getMessage()} method.
     * @param  cause    the cause for this exception, saved for later retrieval by the {@link #getCause()} method.
     */
    public InternalFactoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
