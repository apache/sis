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
package org.apache.sis.referencing.operation.provider;


/**
 * Whether this provider expects source and target <abbr>CRS</abbr> in normalized units and axis order.
 * The <abbr>EPSG</abbr> guidance note identifies two categories of formulas regarding their relationship
 * with axis order and units of measurement:
 *
 * <ul>
 *   <li>Formulas where an intrinsic unambiguous relationship exists.</li>
 *   <li>Formulas where no intrinsic relationship exists, in particular affine and polynomial transformations.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public enum FormulaCategory {
    /**
     * An intrinsic unambiguous relationship exists between formula and axes.
     * Formulas in this category are insensitive to axis order and units in input and output coordinates.
     * The software is expected to apply axis swapping and unit conversions automatically by inspecting
     * the source <abbr>CRS</abbr> and target <abbr>CRS</abbr> definitions.
     * Most coordinate operation methods fall into this category.
     * Examples: all map projections, longitude rotation, <i>etc.</i>
     */
    ASSUME_NORMALIZED_CRS,

    /**
     * No intrinsic relationship exists between formula and axes.
     * This is in particular the case of affine and polynomial transformations.
     * Software should not reorder coordinates or convert units before applying the formula.
     * Any unit conversion factor is embedded in the coefficients provided with the definition
     * of the operation method.
     */
    APPLIED_VERBATIM
}
