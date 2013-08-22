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
     * {@linkplain javax.measure.unit.Unit Units of measurement}.
     * In referencing WKT, this is the text inside {@code UNIT} elements.
     */
    UNIT,

    /**
     * {@linkplain org.opengis.referencing.cs.CoordinateSystemAxis Axes}.
     * In referencing WKT, this is the text inside {@code AXIS} elements.
     */
    AXIS,

    /**
     * {@linkplain org.opengis.util.CodeList Code list} values.
     */
    CODE_LIST,

    /**
     * {@linkplain org.opengis.parameter.ParameterValue Parameter values}.
     * In referencing WKT, this is the text inside {@code PARAMETER} elements.
     */
    PARAMETER,

    /**
     * {@linkplain org.opengis.referencing.operation.OperationMethod Operation methods}.
     * In referencing WKT, this is the text inside {@code PROJECTION} elements.
     */
    METHOD,

    /**
     * {@linkplain org.opengis.referencing.datum.Datum Datum}.
     * In referencing WKT, this is the text inside {@code DATUM} elements.
     */
    DATUM,

    /**
     * Unformattable elements.
     */
    ERROR
}
