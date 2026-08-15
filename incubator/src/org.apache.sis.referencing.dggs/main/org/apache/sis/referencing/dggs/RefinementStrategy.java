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
package org.apache.sis.referencing.dggs;

import java.util.List;
import org.opengis.util.CodeList;


/**
 * List of characteristics of the DGGS refinement strategy.
 *
 * @author Johann Sorel (Geomatys)
 * @see https://docs.ogc.org/as/20-040r3/20-040r3.html#tab-DGG_RefinementStrategy
 */
public final class RefinementStrategy extends CodeList<RefinementStrategy> {

    /**
     * We need to construct values with `valueOf(String)` instead of the constructor because this package is not
     * exported to GeoAPI. See `CodeList` class javadoc.
     */

    /**
     * parent⇐zone.representativePosition() = child⇐zone.representativePosition() for one child.
     * Each parent cell shares a cell←zone.representativePosition with one of its child cells.
     */
    public static final RefinementStrategy centredChildCell;

    /**
     * parent.boundary = parent.child().boundary.
     * The boundary of the set of child cells for a parent is identical to the parent’s boundary.
     */
    public static final RefinementStrategy nestedChildCell;

    /**
     * Each parent cell has a child⇐zone.representativePosition coincident with each of the parent’s nodes
     * (zero-Dimensional topological boundary element).
     */
    public static final RefinementStrategy nodeCentredChildCell;

    /**
     * Each parent cell of dimension greater than 1 has a child cell for which the cell⇐zone.representativePosition
     * lies on each of the parent’s edges (one-Dimensional topological boundary element)
     */
    public static final RefinementStrategy edgeCentredChildCell;

    /**
     * Each parent cell of dimension greater than 2 has a child cell for which the cell⇐zone.representativePosition
     * lies on each of the parent’s faces (two-Dimensional topological boundary element)
     */
    public static final RefinementStrategy faceCentredChildCell;

    /**
     * Each parent cell of dimension greater than 3 has a child cell for which the cell⇐zone.representativePosition
     * lies on each of the parent’s solids (three-Dimensional topological boundary element)
     */
    public static final RefinementStrategy solidCentredChildCell;

    /**
     * All code list values created in the currently running <abbr>JVM</abbr>.
     */
    private static final List<RefinementStrategy> VALUES = initialValues(
        // Inline assignments for getting compiler error if a field is missing or duplicated.
        centredChildCell      = new RefinementStrategy("centredChildCell"),
        nestedChildCell       = new RefinementStrategy("nestedChildCell"),
        nodeCentredChildCell  = new RefinementStrategy("nodeCentredChildCell"),
        edgeCentredChildCell  = new RefinementStrategy("edgeCentredChildCell"),
        faceCentredChildCell  = new RefinementStrategy("faceCentredChildCell"),
        solidCentredChildCell = new RefinementStrategy("solidCentredChildCell"));

    private final String description;

    /**
     * Constructs an element of the given name.
     *
     * @param name the name of the new element.
     */
    private RefinementStrategy(final String name) {
        this(name, null);
    }

    private RefinementStrategy(final String name, String description) {
        super(name);
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Returns the list of codes of the same kind than this code list element.
     *
     * @return all code {@linkplain #values() values} for this code list.
     */
    @Override
    public RefinementStrategy[] family() {
        return VALUES.toArray(RefinementStrategy[]::new);
    }

    /**
     * Returns the sub zone order type that matches the given string, or returns a new one if none match it.
     *
     * @param code the name of the code to fetch or to create.
     * @return a code matching the given name.
     */
    public static RefinementStrategy valueOf(String code) {
        return valueOf(VALUES, code, RefinementStrategy::new);
    }

    /**
     * Returns the sub zone order type that matches the given string, or returns a new one if none match it.
     *
     * @param code the name of the code to fetch or to create.
     * @param description if not found set this description to the newly created instance
     * @return a code matching the given name.
     */
    public static RefinementStrategy valueOf(String code, String description) {
        return valueOf(VALUES, code, (c) -> new RefinementStrategy(c, description));
    }
}
