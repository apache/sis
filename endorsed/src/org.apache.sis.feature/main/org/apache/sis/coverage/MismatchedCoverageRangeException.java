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
package org.apache.sis.coverage;


/**
 * Thrown when the number of bands or sample dimensions specified to a method
 * is not equal to the number expected by a coverage.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see org.opengis.geometry.MismatchedDimensionException
 *
 * @since 1.0
 */
public class MismatchedCoverageRangeException extends IllegalArgumentException {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -8484971745880403661L;

    /**
     * Creates an exception with no message.
     */
    public MismatchedCoverageRangeException() {
        super();
    }

    /**
     * Creates an exception with the specified message.
     *
     * @param message  the detail message, saved for later retrieval by the {@link #getMessage()} method.
     */
    public MismatchedCoverageRangeException(final String message) {
        super(message);
    }

    /**
     * Creates an exception with the specified message and cause.
     *
     * @param message  the detail message, saved for later retrieval by the {@link #getMessage()} method.
     * @param cause    the cause, saved for later retrieval by the {@link #getCause()} method.
     */
    public MismatchedCoverageRangeException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
