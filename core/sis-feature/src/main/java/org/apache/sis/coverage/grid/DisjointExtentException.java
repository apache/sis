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
 * Thrown when operations on a {@link GridGeometry} result in an area which
 * does not intersect anymore the {@link GridExtent} of the {@link GridGeometry}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 * @module
 */
public class DisjointExtentException extends IllegalGridGeometryException {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -1437479443547738220L;

    /**
     * Constructs an exception with no detail message.
     */
    public DisjointExtentException() {
    }

    /**
     * Constructs an exception with the specified detail message.
     *
     * @param  message  the detail message.
     */
    public DisjointExtentException(final String message) {
        super(message);
    }

    /**
     * Constructs an exception with the specified detail message and cause.
     *
     * @param  message  the detail message.
     * @param  cause    the cause for this exception.
     */
    public DisjointExtentException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception with an error message built from the given arguments.
     * Current implementation creates the error message immediately, but we may
     * change to deferred creation later if it is a performance issue.
     *
     * @param dim    identification of the dimension having an invalid value.
     * @param min    the lower bound of valid area.
     * @param max    the upper bound of valid area.
     * @param lower  the lower bound specified by user, which is invalid.
     * @param upper  the upper bound specified by user, which is invalid.
     */
    DisjointExtentException(final Object dim, final long min, final long max, final long lower, final long upper) {
        super(Resources.format(Resources.Keys.GridEnvelopeOutsideCoverage_5, new Object[] {dim, min, max, lower, upper}));
    }

    /**
     * Creates an exception with an error message built from the given extents.
     *
     * @param source   extent of the source.
     * @param request  extent of a slice requested by user.
     * @param dim      index of the dimension having an invalid value.
     */
    DisjointExtentException(final GridExtent source, final GridExtent request, final int dim) {
        this(source.getAxisIdentification(dim, dim),
                source .getLow(dim), source .getHigh(dim),
                request.getLow(dim), request.getHigh(dim));
    }
}
