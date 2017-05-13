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
package org.apache.sis.metadata.sql;


/**
 * Thrown when a metadata access failed.
 * The cause for this exception is typically a {@link java.sql.SQLException}.
 *
 * @author  Touraïvane (IRD)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public class MetadataStoreException extends Exception {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7156617726114815455L;

    /**
     * Creates an instance of {@code MetadataException} with the specified detail message.
     *
     * @param message  the detail message.
     */
    public MetadataStoreException(final String message) {
        super(message);
    }

    /**
     * Creates an instance of {@code MetadataException} with the specified cause.
     *
     * @param cause  the cause of this exception.
     */
    public MetadataStoreException(final Exception cause) {
        super(cause);
    }

    /**
     * Creates an instance of {@code MetadataException} with the specified message and cause.
     *
     * @param message  the detail message.
     * @param cause    the cause of this exception.
     */
    public MetadataStoreException(final String message, final Exception cause) {
        super(message, cause);
    }
}
