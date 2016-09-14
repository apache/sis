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
 * Thrown when a data store failed to construct the coordinate reference system (CRS)
 * or other positioning information. This exception is typically (but not necessarily)
 * caused by {@link org.opengis.referencing.FactoryException} or
 * {@link org.opengis.referencing.operation.TransformException}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public class DataStoreReferencingException extends DataStoreException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2671737996817267335L;

    /**
     * Creates an exception with no cause and no details message.
     */
    public DataStoreReferencingException() {
        super();
    }

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
     *
     * @param cause  the cause for this exception.
     */
    public DataStoreReferencingException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates an exception with the specified details message and cause.
     *
     * @param message  the detail message.
     * @param cause    the cause for this exception.
     */
    public DataStoreReferencingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
