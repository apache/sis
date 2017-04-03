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
package org.apache.sis.referencing.operation.matrix;

import org.opengis.geometry.MismatchedDimensionException;


/**
 * Thrown when two matrices can not be added or multiplied because the sizes do not match.
 *
 * <div class="note"><b>Note:</b>
 * This exception extends {@code MismatchedDimensionException} because the matrices in this package
 * are used in <cite>Coordinate Operation Steps</cite>, in which case a mismatched matrix size means
 * that the operation involves two Coordinate Reference Systems of incompatible dimensions.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.4
 *
 * @see org.opengis.geometry.MismatchedReferenceSystemException
 * @see org.apache.sis.referencing.operation.MismatchedDatumException
 *
 * @since 0.4
 * @module
 */
public class MismatchedMatrixSizeException extends MismatchedDimensionException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -6055645640691056657L;

    /**
     * Constructs a new exception with no message.
     */
    public MismatchedMatrixSizeException() {
        super();
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message  the detail message, or {@code null} if none.
     */
    public MismatchedMatrixSizeException(final String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message  the detail message, or {@code null} if none.
     * @param cause    the cause, or {@code null} if none.
     */
    public MismatchedMatrixSizeException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
