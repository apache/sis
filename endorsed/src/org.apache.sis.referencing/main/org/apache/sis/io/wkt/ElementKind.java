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

import javax.measure.Unit;
import org.opengis.util.CodeList;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.extent.Extent;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.parameter.GeneralParameterValue;
import org.apache.sis.math.NumberType;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.datum.DatumEnsemble;


/**
 * Kind of an element in a <i>Well Known Text</i>.
 * Different kinds of elements can be associated to different {@linkplain Colors colors}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   0.4
 */
public enum ElementKind {
    /**
     * Object name, typically written immediately after the WKT keyword and its opening bracket.
     */
    NAME,

    /**
     * Object identifier, typically written almost last just before remarks.
     */
    IDENTIFIER,

    /**
     * Floating point numbers (excluding integer types).
     */
    NUMBER,

    /**
     * Integer numbers.
     */
    INTEGER,

    /**
     * Units of measurement, often represented by {@code UNIT[…]} elements.
     *
     * @see javax.measure.Unit
     */
    UNIT,

    /**
     * Coordinate system axes, often represented by {@code AXIS[…]} elements.
     *
     * @see org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis
     */
    AXIS,

    /**
     * Code list values.
     *
     * @see org.opengis.util.CodeList
     */
    CODE_LIST,

    /**
     * Name of parameters, often represented by {@code PARAMETER[…]} elements.
     *
     * @see org.apache.sis.parameter.AbstractParameterDescriptor
     */
    PARAMETER,

    /**
     * Operation methods, often represented by {@code PROJECTION[…]} elements.
     *
     * @see org.apache.sis.referencing.operation.DefaultOperationMethod
     */
    METHOD,

    /**
     * Datum or reference frame, often represented by {@code DATUM[…]} elements.
     *
     * @see org.apache.sis.referencing.datum.AbstractDatum
     */
    DATUM,

    /**
     * Ensemble of datum or reference frames, represented by {@code ENSEMBLE[…]} elements.
     *
     * @see org.apache.sis.referencing.datum.DefaultDatumEnsemble
     *
     * @since 1.5
     */
    ENSEMBLE,

    /**
     * CRS, datum or operation scope, often represented by {@code SCOPE[…]} elements.
     *
     * @see org.apache.sis.referencing.DefaultObjectDomain#getScope()
     */
    SCOPE,

    /**
     * CRS, datum or operation domain of validity,
     * often represented by {@code AREA[…]} or {@code BBOX[…]} elements.
     *
     * @see org.apache.sis.referencing.DefaultObjectDomain#getDomainOfValidity()
     */
    EXTENT,

    /**
     * Citation (typically for the authority), often represented by {@code CITATION[…]} elements.
     *
     * @see org.apache.sis.referencing.ImmutableIdentifier#getAuthority()
     */
    CITATION,

    /**
     * Remarks, often represented by {@code REMARKS[…]} elements.
     *
     * <p>When formatting an ISO 19162 Well Known Text, texts quoted as remarks preserve non-ASCII characters.
     * By contrast, quoted texts in any other {@code ElementKind} will have some non-ASCII characters replaced
     * by ASCII ones (e.g. "é" → "e").</p>
     *
     * @see org.apache.sis.referencing.AbstractIdentifiedObject#getRemarks()
     */
    REMARKS,

    /**
     * Unformattable elements.
     */
    ERROR;

    /**
     * Returns the element kind for an object of the given type.
     * The current implementation defines the following associations:
     *
     * <table class="sis">
     *   <caption>Mapping from Java type to WKT element</caption>
     *   <tr><th>Base type</th>                     <th>Kind</th></tr>
     *   <tr><td>{@link Datum}</td>                 <td>{@link #DATUM}</td></tr>
     *   <tr><td>{@link DatumEnsemble}</td>         <td>{@link #ENSEMBLE}</td></tr>
     *   <tr><td>{@link OperationMethod}</td>       <td>{@link #METHOD}</td></tr>
     *   <tr><td>{@link GeneralParameterValue}</td> <td>{@link #PARAMETER}</td></tr>
     *   <tr><td>{@link CoordinateSystemAxis}</td>  <td>{@link #AXIS}</td></tr>
     *   <tr><td>{@link Identifier}</td>            <td>{@link #IDENTIFIER}</td></tr>
     *   <tr><td>{@link Citation}</td>              <td>{@link #CITATION}</td></tr>
     *   <tr><td>{@link CodeList}</td>              <td>{@link #CODE_LIST}</td></tr>
     *   <tr><td>{@link Extent}</td>                <td>{@link #EXTENT}</td></tr>
     *   <tr><td>{@link Unit}</td>                  <td>{@link #UNIT}</td></tr>
     *   <tr><td>{@link Number}</td>                <td>{@link #INTEGER} or {@link #NUMBER}</td></tr>
     * </table>
     *
     * The given type can be any sub-type of the above types. If an object implements more
     * than one of the above interfaces, then the selected {@code ElementKind} is arbitrary.
     *
     * @param  type  the object type, or {@code null}.
     * @return the element kind of the given type, or {@code null} if none match.
     */
    public static ElementKind forType(final Class<?> type) {
        if (type != null) {
            if (Datum                .class.isAssignableFrom(type)) return DATUM;
            if (DatumEnsemble        .class.isAssignableFrom(type)) return ENSEMBLE;
            if (OperationMethod      .class.isAssignableFrom(type)) return METHOD;
            if (GeneralParameterValue.class.isAssignableFrom(type)) return PARAMETER;
            if (CoordinateSystemAxis .class.isAssignableFrom(type)) return AXIS;
            if (Identifier           .class.isAssignableFrom(type)) return IDENTIFIER;
            if (Citation             .class.isAssignableFrom(type)) return CITATION;
            if (CodeList             .class.isAssignableFrom(type)) return CODE_LIST;
            if (Extent               .class.isAssignableFrom(type)) return EXTENT;
            if (Unit                 .class.isAssignableFrom(type)) return UNIT;
            if (Number.class.isAssignableFrom(type)) {
                return NumberType.isInteger(type) ? INTEGER : NUMBER;
            }
        }
        return null;
    }
}
