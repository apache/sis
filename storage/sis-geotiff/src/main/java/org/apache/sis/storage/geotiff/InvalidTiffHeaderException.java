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
package org.apache.sis.storage.geotiff;

import java.io.IOException;


/**
 * Thrown when a TIFF Image File Directory can not be read because of a logical inconsistency.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
class InvalidTiffHeaderException extends IOException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3469934460013440211L;

    /**
     * Creates a new exception with the given error message.
     *
     * @param message the error message.
     */
    InvalidTiffHeaderException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the given error message and error cause.
     *
     * @param message  the error message.
     * @param cause    the cause of this error.
     */
    InvalidTiffHeaderException(String message, final Throwable cause) {
        super(message, cause);
    }
}
