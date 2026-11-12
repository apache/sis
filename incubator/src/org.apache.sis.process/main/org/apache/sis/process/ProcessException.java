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
package org.apache.sis.process;

import org.opengis.metadata.Identifier;


/**
 * Thrown when a {@linkplain Process process} failed.
 *
 * @author Martin Desruisseaux (Geomatys)
 */
public class ProcessException extends Exception {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -9009593453460083634L;

    /**
     * The process identifier, or {@code null}. We retain only the identifier rather than
     * a full {@link Process} object because the process may not be serializable, or may
     * be associated with large amount of data that we don't want to serialize.
     */
    private final Identifier processId;

    /**
     * Creates a new exception with the specified detail message.
     *
     * @param message The details message, or {@code null}.
     * @param process The process that failed, or {@code null}.
     */
    public ProcessException(final String message, final Process process) {
        this(message, process, null);
    }

    /**
     * Creates a new exception with the specified detail message and cause.
     *
     * @param message The details message, or {@code null}.
     * @param process The process that failed, or {@code null}.
     * @param cause   The cause, or {@code null}.
     */
    public ProcessException(final String message, final Process process, final Throwable cause) {
        super(message, cause);
        if (process != null) {
            final ProcessDescriptor descriptor = process.getDescriptor();
            if (descriptor != null) {
                processId = descriptor.getIdentifier();
                return;
            }
        }
        processId = null;
    }

    /**
     * Returns the identifier of the process that failed, or {@code null} if this information
     * is not available.
     *
     * @return The identifier of the process that failed, or {@code null}.
     */
    public Identifier getProcessIdentifier() {
        return processId;
    }
}
