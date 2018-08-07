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
package org.apache.sis.referencing.gazetteer;

import org.opengis.referencing.operation.TransformException;


/**
 * Thrown when a coordinate can not be converted to a geographic identifier, or conversely.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public class GazetteerException extends TransformException {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 3607149545794483627L;

    /**
     * Constructs a new exception with no detail message.
     */
    public GazetteerException() {
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param  message  the details message, or {@code null} if none.
     */
    public GazetteerException(final String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause.
     * The details message is copied from the cause.
     *
     * @param  cause  the cause, or {@code null} if none.
     */
    public GazetteerException(final Throwable cause) {
        // Reproduce the behavior of standard Throwable(Throwable) constructor.
        super((cause != null) ? cause.toString() : null, cause);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param  message  the details message, or {@code null} if none.
     * @param  cause    the cause, or {@code null} if none.
     */
    public GazetteerException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
