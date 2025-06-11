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
 * Thrown when a resources cannot be filtered with a given query.
 * Some examples of cases where this exception may be thrown are:
 *
 * <ul>
 *   <li>The {@link Query} is an instance of a class unsupported by the resource
 *       (for example, applying a {@link CoverageQuery} on a {@link FeatureSet}).</li>
 *   <li>The query is requesting a property that does not exist in the {@link Resource}.</li>
 *   <li>The values in the {@link DataStore} are unconvertible to some characteristics
 *       (e.g., type or name) requested by the query.</li>
 * </ul>
 *
 * This exception may be thrown when {@link FeatureSet#subset(Query)} is executed,
 * but may also be deferred until another method of the returned subset is invoked.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 0.8
 *
 * @see FeatureSet#subset(Query)
 *
 * @since 0.8
 */
public class UnsupportedQueryException extends DataStoreException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4593505566766684270L;

    /**
     * Creates an exception with no cause and no details message.
     */
    public UnsupportedQueryException() {
    }

    /**
     * Creates an exception with the specified details message.
     *
     * @param message  the detail message.
     */
    public UnsupportedQueryException(final String message) {
        super(message);
    }

    /**
     * Creates an exception with the specified cause and no details message.
     *
     * @param cause  the cause for this exception.
     */
    public UnsupportedQueryException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates an exception with the specified details message and cause.
     *
     * @param message  the detail message.
     * @param cause    the cause for this exception.
     */
    public UnsupportedQueryException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
