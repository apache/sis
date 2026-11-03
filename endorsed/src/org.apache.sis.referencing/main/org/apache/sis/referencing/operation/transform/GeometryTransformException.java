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
package org.apache.sis.referencing.operation.transform;

import org.opengis.referencing.operation.TransformException;


/**
 * Thrown when a transform cannot be applied on a given geometry or envelope instance.
 * This exception is usually thrown when a geometric prerequisite has not been met.
 * The failed check may be before any attempt to actually transform a point.
 * For example, the geometry may be an instance of an unrecognized type,
 * or the geometry envelope may be outside the
 * {@linkplain AbstractMathTransform#getDomain transform domain}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.8
 * @since   1.8
 */
public class GeometryTransformException extends TransformException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -9058827495784393813L;

    /**
     * Creates an exception with the specified detail message.
     *
     * @param message  the detail message, saved for later retrieval by the {@link #getMessage()} method.
     */
    public GeometryTransformException(String message) {
        super(message);
    }

    /**
     * Creates an exception with the specified cause.
     *
     * @param cause  the cause, saved for later retrieval by the {@link #getCause()} method.
     */
    public GeometryTransformException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates an exception with the specified detail message and cause.
     *
     * @param message  the detail message, saved for later retrieval by the {@link #getMessage()} method.
     * @param cause    the cause, saved for later retrieval by the {@link #getCause()} method.
     */
    public GeometryTransformException(String message, Throwable cause) {
        super(message, cause);
    }
}
