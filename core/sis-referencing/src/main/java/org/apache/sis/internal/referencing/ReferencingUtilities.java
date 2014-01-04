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
package org.apache.sis.internal.referencing;

import javax.measure.unit.Unit;
import org.opengis.parameter.*;
import org.opengis.referencing.*;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Static;


/**
 * A set of static methods working on GeoAPI referencing objects.
 * Some of those methods may be useful, but not really rigorous.
 * This is why they do not appear in the public packages.
 *
 * <p><strong>Do not rely on this API!</strong> It may change in incompatible way in any future release.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 */
public final class ReferencingUtilities extends Static {
    /**
     * Subtypes of {@link IdentifiedObject} for which a URN type is defined.
     * For each interface at index <var>i</var>, the URN type is {@code URN_TYPES[i]}.
     *
     * <p>For performance reasons, most frequently used types should be first.</p>
     */
    private static final Class<?>[] TYPES = {
        CoordinateReferenceSystem.class,
        Datum.class,
        Ellipsoid.class,
        PrimeMeridian.class,
        CoordinateSystem.class,
        CoordinateSystemAxis.class,
        CoordinateOperation.class,
        OperationMethod.class,
        ParameterDescriptor.class,
        ReferenceSystem.class
    };

    /**
     * The URN types for instances of {@link #TYPES}.
     * See {@link org.apache.sis.internal.util.DefinitionURI} javadoc for a list of URN types.
     */
    private static final String[] URN_TYPES = {
        "crs",
        "datum",
        "ellipsoid",
        "meridian",
        "cs",
        "axis",
        "coordinateOperation",
        "method",
        "parameter",
        "referenceSystem"
    };

    /**
     * Do not allow instantiation of this class.
     */
    private ReferencingUtilities() {
    }

    /**
     * Retrieves the value at the specified row and column of the given matrix, wrapped in a {@code Number}.
     * The {@code Number} type depends on the matrix accuracy.
     *
     * @param matrix The matrix from which to get the number.
     * @param row    The row index, from 0 inclusive to {@link Matrix#getNumRow()} exclusive.
     * @param column The column index, from 0 inclusive to {@link Matrix#getNumCol()} exclusive.
     * @return       The current value at the given row and column.
     */
    public static Number getNumber(final Matrix matrix, final int row, final int column) {
        if (matrix instanceof MatrixSIS) {
            return ((MatrixSIS) matrix).getNumber(row, column);
        } else {
            return matrix.getElement(row, column);
        }
    }

    /**
     * Returns the unit used for all axes in the given coordinate system.
     * If not all axes use the same unit, then this method returns {@code null}.
     * This convenience method is often used for Well Know Text (WKT) version 1 formatting.
     *
     * @param cs The coordinate system for which to get the unit, or {@code null}.
     * @return The unit for all axis in the given coordinate system, or {@code null}.
     */
    public static Unit<?> getUnit(final CoordinateSystem cs) {
        Unit<?> unit = null;
        if (cs != null) {
            for (int i=cs.getDimension(); --i>=0;) {
                final Unit<?> candidate = cs.getAxis(i).getUnit();
                if (candidate != null) {
                    if (unit == null) {
                        unit = candidate;
                    } else if (!unit.equals(candidate)) {
                        return null;
                    }
                }
            }
        }
        return unit;
    }

    /**
     * Ensures that the given argument value is {@code false}. This method is invoked by private setter methods,
     * which are themselves invoked by JAXB at unmarshalling time. Invoking this method from those setter methods
     * serves two purposes:
     *
     * <ul>
     *   <li>Make sure that a singleton property is not defined twice in the XML document.</li>
     *   <li>Protect ourselves against changes in immutable objects outside unmarshalling. It should
     *       not be necessary since the setter methods shall not be public, but we are paranoiac.</li>
     *   <li>Be a central point where we can trace all setter methods, in case we want to improve
     *       warning or error messages in future SIS versions.</li>
     * </ul>
     *
     * @param  name The property name, used only in case of error message to format.
     * @param  isDefined Whether the property in the caller object is current defined.
     * @return {@code true} if the caller can set the property.
     * @throws IllegalStateException If {@code isDefined} is {@code true}.
     */
    public static boolean canSetProperty(final String name, final boolean isDefined) throws IllegalStateException {
        if (isDefined) {
            // Future SIS version could log a warning instead if a unmarshalling is in progress.
            throw new IllegalStateException(Errors.format(Errors.Keys.ElementAlreadyPresent_1, name));
        }
        return true;
    }

    /**
     * Returns the URN type for the given class, or {@code null} if unknown.
     * See {@link org.apache.sis.internal.util.DefinitionURI} javadoc for a list of URN types.
     *
     * @param  type The class for which to get the URN type.
     * @return The URN type, or {@code null} if unknown.
     *
     * @see org.apache.sis.internal.util.DefinitionURI
     */
    public static String toURNType(final Class<?> type) {
        for (int i=0; i<TYPES.length; i++) {
            if (TYPES[i].isAssignableFrom(type)) {
                return URN_TYPES[i];
            }
        }
        return null;
    }
}
