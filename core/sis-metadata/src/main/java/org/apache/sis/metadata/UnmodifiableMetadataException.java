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
 * Thrown on attempt to set a read-only value in a metadata object.
 * This exception may happen in the following scenarios:
 *
 * <ul>
 *   <li>A metadata instance was initially {@linkplain org.apache.sis.metadata.ModifiableMetadata
 *       modifiable}, but that instance has since be declared unmodifiable.</li>
 *   <li>A write operation has been attempted on the {@linkplain AbstractMetadata#asMap() map view},
 *       but the metadata object has no corresponding setter methods.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public class UnmodifiableMetadataException extends UnsupportedOperationException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 286569086054839096L;

    /**
     * Creates a new exception with the specified detail message.
     *
     * @param message The detail message.
     */
    public UnmodifiableMetadataException(final String message) {
        super(message);
    }
}
