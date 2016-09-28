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
 * Thrown when a data store is closed and can no more return data.
 *
 * @author  Marc Le Bihan
 * @version 0.6
 * @since   0.6
 * @module
 */
public class DataStoreClosedException extends DataStoreException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7205119080377665796L;

    /**
     * Creates an exception with the specified details message.
     *
     * @param message  the detail message of the exception.
     */
    public DataStoreClosedException(String message) {
        super(message);
    }

    /**
     * Creates an exception with the specified details message and cause.
     *
     * @param message  the detail message of the exception.
     * @param cause    the cause root for the exception.
     */
    public DataStoreClosedException(String message, Throwable cause) {
        super(message, cause);
    }
}
