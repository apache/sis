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
package org.apache.sis.io.wkt;

import javax.measure.unit.Unit;
import org.opengis.util.CodeList;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.parameter.GeneralParameterValue;
import org.apache.sis.util.Numbers;


/**
 * Kind of an element in a <cite>Well Known Text</cite>.
 * Different kinds of elements can be associated to different {@linkplain Colors colors}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-3.00)
 * @version 0.4
 * @module
 */
public enum ElementKind {
    /**
     * Floating point numbers (excluding integer types).
     */
    NUMBER,

    /**
     * Integer numbers.
     */
    INTEGER,

    /**
     * {@linkplain javax.measure.unit.Unit Units of measurement},
     * often represented by {@code UNIT[…]} elements.
     */
    UNIT,

    /**
     * {@linkplain org.opengis.referencing.cs.CoordinateSystemAxis Axes},
     * often represented by {@code AXIS[…]} elements.
     */
    AXIS,

    /**
     * {@linkplain org.opengis.util.CodeList Code list} values.
     */
    CODE_LIST,

    /**
     * {@linkplain org.opengis.parameter.ParameterValue Parameter values},
     * often represented by {@code PARAMETER[…]} elements.
     */
    PARAMETER,

    /**
     * {@linkplain org.opengis.referencing.operation.OperationMethod Operation methods},
     * often represented by {@code PROJECTION[…]} elements.
     */
    METHOD,

    /**
     * {@linkplain org.opengis.referencing.datum.Datum Datum},
     * often represented by {@code DATUM[…]} elements.
     */
    DATUM,

    /**
     * Unformattable elements.
     */
    ERROR;

    /**
     * Returns the element kind for an object of the given type.
     * The current implementation defines the following associations:
     *
     * <table class="sis">
     *   <tr><th>Base type</th>                     <th>Kind</th></tr>
     *   <tr><td>{@link Datum}</td>                 <td>{@link #DATUM}</td></tr>
     *   <tr><td>{@link OperationMethod}</td>       <td>{@link #METHOD}</td></tr>
     *   <tr><td>{@link GeneralParameterValue}</td> <td>{@link #PARAMETER}</td></tr>
     *   <tr><td>{@link CoordinateSystemAxis}</td>  <td>{@link #AXIS}</td></tr>
     *   <tr><td>{@link CodeList}</td>              <td>{@link #CODE_LIST}</td></tr>
     *   <tr><td>{@link Unit}</td>                  <td>{@link #UNIT}</td></tr>
     *   <tr><td>{@link Number}</td>                <td>{@link #INTEGER} or {@link #NUMBER}</td></tr>
     * </table>
     *
     * The given type can be any sub-type of the above types. If an object implements more
     * than one of the above interfaces, then the selected {@code ElementKind} is arbitrary.
     *
     * @param  type The object type, or {@code null}.
     * @return The element kind of the given type, or {@code null} if none match.
     */
    public static ElementKind forType(final Class<?> type) {
        if (type != null) {
            if (Datum                .class.isAssignableFrom(type)) return DATUM;
            if (OperationMethod      .class.isAssignableFrom(type)) return METHOD;
            if (GeneralParameterValue.class.isAssignableFrom(type)) return PARAMETER;
            if (CoordinateSystemAxis .class.isAssignableFrom(type)) return AXIS;
            if (CodeList             .class.isAssignableFrom(type)) return CODE_LIST;
            if (Unit                 .class.isAssignableFrom(type)) return UNIT;
            if (Number.class.isAssignableFrom(type)) {
                return Numbers.isInteger(type) ? INTEGER : NUMBER;
            }
        }
        return null;
    }
}
