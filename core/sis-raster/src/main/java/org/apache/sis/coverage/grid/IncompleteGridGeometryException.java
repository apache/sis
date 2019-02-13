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


/**
 * Thrown by {@link GridGeometry} when a grid geometry can not provide the requested information.
 * For example this exception is thrown when {@link GridGeometry#getEnvelope()} is invoked while
 * the grid geometry has been built with a null envelope.
 *
 * <p>The {@link GridGeometry#isDefined(int)} can be used for avoiding this exception.
 * For example if a process is going to need both the grid extent and the "grid to CRS" transform,
 * than it can verify if those two conditions are met in a single method call:</p>
 *
 * {@preformat java
 *     if (gg.isDefined(GridGeometry.EXTENT | GridGeometry.GRID_TO_CRS) {
 *         GridExtent    extent    = gg.getGridExtent();
 *         MathTransform gridToCRS = gg.getGridToCRS(PixelInCell.CELL_CENTER);
 *         // Do the process.
 *     }
 * }
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class IncompleteGridGeometryException extends IllegalStateException {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -7386283388753448743L;

    /**
     * Constructs an exception with no detail message.
     */
    public IncompleteGridGeometryException() {
    }

    /**
     * Constructs an exception with the specified detail message.
     *
     * @param  message  the detail message.
     */
    public IncompleteGridGeometryException(final String message) {
        super(message);
    }

    /**
     * Constructs an exception with the specified detail message and cause.
     *
     * @param  message  the detail message.
     * @param  cause    the cause for this exception.
     */
    public IncompleteGridGeometryException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
