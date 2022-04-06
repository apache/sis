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
 * Thrown when a write operation can not be performed because it would cause the replacement of an existing resource.
 * Replacements can be performed only if an option such as {@link WritableGridCoverageResource.CommonOption#REPLACE}
 * has been specified.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public class ResourceAlreadyExistsException extends DataStoreException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3854120553618596031L;

    /**
     * Creates an exception with no cause and no details message.
     */
    public ResourceAlreadyExistsException() {
    }

    /**
     * Creates an exception with the specified details message.
     *
     * @param message  the detail message.
     */
    public ResourceAlreadyExistsException(String message) {
        super(message);
    }

    /**
     * Creates an exception with the specified details message and cause.
     *
     * @param message  the detail message.
     * @param cause    the cause for this exception.
     */
    public ResourceAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
