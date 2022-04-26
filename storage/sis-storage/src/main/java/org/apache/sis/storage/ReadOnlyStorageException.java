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
 * Thrown when a {@code DataStore} can not perform a write operation.
 * If a data store does not support any write operation, then it should not implement
 * {@link WritableAggregate} or {@link WritableFeatureSet} interface.
 * But in some situations, a data store may implement a {@code Writable*} interface
 * and nevertheless be unable to perform a write operation, for example because the
 * underlying {@link java.nio.channels.Channel} is read-only or part of the file is
 * locked by another process.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 *
 * @see ForwardOnlyStorageException
 */
public class ReadOnlyStorageException extends DataStoreException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5710116172772560023L;

    /**
     * Creates an exception with no cause and no details message.
     */
    public ReadOnlyStorageException() {
    }

    /**
     * Creates an exception with the specified details message.
     *
     * @param message  the detail message.
     */
    public ReadOnlyStorageException(final String message) {
        super(message);
    }

    /**
     * Creates an exception with the specified cause and no details message.
     *
     * @param cause  the cause for this exception.
     */
    public ReadOnlyStorageException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates an exception with the specified details message and cause.
     *
     * @param message  the detail message.
     * @param cause    the cause for this exception.
     */
    public ReadOnlyStorageException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
