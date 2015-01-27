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
package org.apache.sis.metadata;


/**
 * Thrown when a metadata is in a invalid state, usually because a mandatory property is missing.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public class InvalidMetadataException extends IllegalStateException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 947896851753786460L;

    /**
     * Creates a new exception with the specified detail message.
     *
     * @param message The detail message.
     */
    public InvalidMetadataException(final String message) {
        super(message);
    }
}
