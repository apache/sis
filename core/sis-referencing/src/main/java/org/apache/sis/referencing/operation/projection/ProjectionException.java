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
package org.apache.sis.referencing.operation.projection;

import org.opengis.referencing.operation.TransformException;


/**
 * Thrown by {@link NormalizedProjection} when a map projection failed.
 *
 * <div class="section">When this exception is thrown</div>
 * Apache SIS implementations of map projections return a {@linkplain Double#isFinite(double) finite} number
 * under normal conditions, but may also return an {@linkplain Double#isInfinite(double) infinite} number or
 * {@linkplain Double#isNaN(double) NaN} value, or throw this exception.
 * The behavior depends on the reason why the projection can not return a finite number:
 *
 * <ul>
 *   <li>If the expected mathematical value is infinite (for example the Mercator projection at ±90° of latitude),
 *       then the map projection should return a {@link Double#POSITIVE_INFINITY} or {@link Double#NEGATIVE_INFINITY},
 *       depending on the sign of the correct mathematical answer.</li>
 *   <li>If no real number is expected to exist for the input coordinate (for example at a latitude greater than 90°),
 *       then the map projection should return {@link Double#NaN}.</li>
 *   <li>If a real number is expected to exist but the map projection fails to compute it (for example because an
 *       iterative algorithm does not converge), then the projection should throw {@code ProjectionException}.</li>
 * </ul>
 *
 * @author  André Gosselin (MPO)
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @version 0.6
 * @since   0.6
 * @module
 */
public class ProjectionException extends TransformException {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 3031350727691500915L;

    /**
     * Constructs a new exception with no detail message.
     */
    public ProjectionException() {
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param  message  the details message, or {@code null} if none.
     */
    public ProjectionException(final String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause.
     * The details message is copied from the cause.
     *
     * @param  cause  the cause, or {@code null} if none.
     */
    public ProjectionException(final Throwable cause) {
        // Reproduce the behavior of standard Throwable(Throwable) constructor.
        super((cause != null) ? cause.toString() : null, cause);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param  message  the details message, or {@code null} if none.
     * @param  cause    the cause, or {@code null} if none.
     */
    public ProjectionException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
