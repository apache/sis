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

import java.util.Set;
import java.util.Collections;
import org.apache.sis.referencing.internal.shared.AxisDirections;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.OperationNotFoundException;
import org.apache.sis.referencing.operation.matrix.UnderdeterminedMatrixException;
import org.apache.sis.util.collection.CodeListSet;


/**
 * Thrown when a coordinate operation cannot be find because some dimensions are missing in the source <abbr>CRS</abbr>.
 * The missing dimensions are identified by the directions of the missing axes. For example, if the coordinate system of
 * the source <abbr>CRS</abbr> does not have a temporal axis which was requested by the target <abbr>CRS</abbr>,
 * then the {@link #getMissingAxes()} should contain {@link AxisDirection#FUTURE} or {@link AxisDirection#PAST}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.6
 * @since   1.6
 */
public class MissingSourceDimensionsException extends OperationNotFoundException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5228802326932593847L;

    /**
     * The unknown axes identified by their directions, created when first needed.
     */
    private CodeListSet<AxisDirection> axes;

    /**
     * Constructs a new exception with no message.
     */
    public MissingSourceDimensionsException() {
        super();
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message  the detail message, or {@code null} if none.
     */
    public MissingSourceDimensionsException(final String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message  the detail message, or {@code null} if none.
     * @param cause    the cause, or {@code null} if none.
     */
    public MissingSourceDimensionsException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Adds all axis directions of the target coordinate system
     * that are not found in the source coordinate system.
     */
    final void addMissing(final CoordinateSystem source, final CoordinateSystem target) {
        for (int i = target.getDimension(); --i >= 0;) {
            AxisDirection direction = target.getAxis(i).getDirection();
            if (AxisDirections.indexOfColinear(source, direction) < 0) {
                addMissing(direction);
            }
        }
    }

    /**
     * Indicates which dimension was missing, specified as an axis direction.
     *
     * @param  direction  direction of the missing axis.
     */
    public void addMissing(final AxisDirection direction) {
        if (axes == null) {
            axes = new CodeListSet<>(AxisDirection.class);
            final Throwable cause = getCause();
            if (cause instanceof UnderdeterminedMatrixException) {
                axes.addAll(((UnderdeterminedMatrixException) cause).getUnknownAxes());
            }
        }
        axes.add(direction);
    }

    /**
     * Indices which axes were missing in the source coordinate system.
     * The returned set does not need to be exhaustive and may be empty if this information is not available.
     *
     * @return directions of axes that are missing in the source coordinate system.
     */
    public Set<AxisDirection> getMissingAxes() {
        if (axes != null) {
            return Collections.unmodifiableSet(axes);
        }
        final Throwable cause = getCause();
        if (cause instanceof UnderdeterminedMatrixException) {
            return ((UnderdeterminedMatrixException) cause).getUnknownAxes();
        }
        return Set.of();
    }
}
