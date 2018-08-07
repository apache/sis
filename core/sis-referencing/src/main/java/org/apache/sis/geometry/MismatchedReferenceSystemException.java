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
package org.apache.sis.geometry;


/**
 * Indicates that an object cannot be constructed because of a mismatch in the
 * {@linkplain org.opengis.referencing.ReferenceSystem reference systems} of
 * geometric components.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.3
 * @version 0.3
 * @module
 */
public class MismatchedReferenceSystemException extends IllegalArgumentException {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 6222334569692693273L;

    /**
     * Creates an exception with no message.
     */
    public MismatchedReferenceSystemException() {
        super();
    }

    /**
     * Creates an exception with the specified message.
     *
     * @param  message The detail message. The detail message is saved for
     *         later retrieval by the {@link #getMessage()} method.
     */
    public MismatchedReferenceSystemException(final String message) {
        super(message);
    }

    /**
     * Creates an exception with the specified message and cause.
     *
     * @param  message The detail message. The detail message is saved for
     *         later retrieval by the {@link #getMessage()} method.
     * @param  cause The cause.
     */
    public MismatchedReferenceSystemException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
