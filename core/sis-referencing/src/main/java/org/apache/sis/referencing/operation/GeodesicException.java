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
package org.apache.sis.referencing.operation;

import org.opengis.referencing.operation.TransformException;


/**
 * Thrown when an error occurred while computing geodesic between two points.
 *
 * <div class="note"><b>API note:</b>
 * defined as a sub-type of {@link TransformException} because some kind of coordinate operations are involved in
 * geodesics calculation (e.g. transformation of latitudes and longitudes to coordinates on an auxiliary sphere).
 * The starting and ending points can also be given in a CRS that require additional coordinate operations.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class GeodesicException extends TransformException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7844524593329236175L;

    /**
     * Constructs a new exception with no message.
     */
    public GeodesicException() {
        super();
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message  the detail message, or {@code null} if none.
     */
    public GeodesicException(final String message) {
        super(message);
    }
}
