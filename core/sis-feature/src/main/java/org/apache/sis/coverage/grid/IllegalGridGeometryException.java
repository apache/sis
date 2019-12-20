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
package org.apache.sis.coverage.grid;

import org.apache.sis.internal.feature.Resources;


/**
 * Thrown when the argument specified to a method or constructor would result in an invalid {@link GridGeometry}.
 * This exception may have a {@link org.opengis.referencing.operation.TransformException} as its cause, in which
 * case the grid geometry failed to use a given "grid to CRS" transform over the given grid extent. Such failure
 * may happen with non-linear transforms, but are less likely in the common case where the grid geometry uses a
 * linear (or affine) "grid to CRS" transform.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 * @module
 */
public class IllegalGridGeometryException extends IllegalArgumentException {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -7849140502096470380L;

    /**
     * Constructs an exception with no detail message.
     */
    public IllegalGridGeometryException() {
    }

    /**
     * Constructs an exception with the specified detail message.
     *
     * @param  message  the detail message.
     */
    public IllegalGridGeometryException(final String message) {
        super(message);
    }

    /**
     * Constructs an exception with the specified cause.
     *
     * @param  cause  the cause for this exception.
     *
     * @since 1.1
     */
    public IllegalGridGeometryException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs an exception with the specified detail message and cause.
     *
     * @param  message  the detail message.
     * @param  cause    the cause for this exception.
     */
    public IllegalGridGeometryException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs an exception with a detail message incriminating the given parameter.
     *
     * @param cause      the cause of the failure to create the grid geometry.
     * @param component  name of the parameter that caused the failure.
     */
    IllegalGridGeometryException(final Throwable cause, final String component) {
        super(Resources.format(Resources.Keys.IllegalGridGeometryComponent_1, component), cause);
    }
}
