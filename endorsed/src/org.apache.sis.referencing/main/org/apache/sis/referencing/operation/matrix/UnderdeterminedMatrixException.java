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

import java.util.Set;
import java.util.Collections;
import org.apache.sis.util.collection.CodeListSet;
import org.opengis.referencing.cs.AxisDirection;


/**
 * Thrown when a matrix cannot be determined because of unknown terms.
 * It may happen, for example, during the construction of an affine transform between two
 * coordinate systems when the source coordinate system does not have enough dimensions.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.6
 *
 * @see Matrices#createTransform(AxisDirection[], AxisDirection[])
 *
 * @since 1.6
 */
public class UnderdeterminedMatrixException extends IllegalArgumentException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5248380660948565901L;

    /**
     * The unknown axes, identified by their directions.
     */
    private final CodeListSet<AxisDirection> axes = new CodeListSet<>(AxisDirection.class);

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message  the detail message, or {@code null} if none.
     */
    public UnderdeterminedMatrixException(final String message) {
        super(message);
    }

    /**
     * Indicates which term was unknown, specified as an axis direction for convenience.
     * Unknown axes correspond to specific row indexes in the matrix, but are specified
     * as axis directions for making easier for the caller to analyze. There is usually
     * only one missing axis (typically the vertical or temporal one), but more can be added.
     *
     * <p>In the current Apache <abbr>SIS</abbr> version, the only reason for undetermined
     * matrix is a missing axis, but more reasons may be added in future versions.</p>
     *
     * @param  direction  direction of the unknown axis.
     */
    public void addUnknown(final AxisDirection direction) {
        axes.add(direction);
    }

    /**
     * Indicates which axes were unknown during the attempt to create an affine transform.
     * Unknown axes are identified by the directions that they were expected to have.
     *
     * @return directions of axes that are missing in the source coordinate system.
     */
    public Set<AxisDirection> getUnknownAxes() {
        return Collections.unmodifiableSet(axes);
    }
}
