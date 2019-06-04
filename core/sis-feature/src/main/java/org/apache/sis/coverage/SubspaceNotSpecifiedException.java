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

import org.opengis.coverage.CannotEvaluateException;


/**
 * Thrown when an operation can only be applied on a subspace of a multi-dimensional coverage,
 * but not such subspace has been specified.
 * For example if a {@link org.apache.sis.coverage.grid.GridCoverage} has three or more dimensions,
 * then a two-dimensional slice must be specified in order to produce a {@link java.awt.image.RenderedImage}
 * from that grid coverage.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see <a href="https://en.wikipedia.org/wiki/Linear_subspace">Linear subspace on Wikipedia</a>
 *
 * @since 1.0
 * @module
 */
public class SubspaceNotSpecifiedException extends CannotEvaluateException {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -8993517725073815199L;

    /**
     * Constructs an exception with no detail message.
     */
    public SubspaceNotSpecifiedException() {
    }

    /**
     * Constructs an exception with the specified detail message.
     *
     * @param  message  the detail message.
     */
    public SubspaceNotSpecifiedException(final String message) {
        super(message);
    }

    /**
     * Constructs an exception with the specified detail message and cause.
     *
     * @param  message  the detail message.
     * @param  cause    the cause for this exception.
     */
    public SubspaceNotSpecifiedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
