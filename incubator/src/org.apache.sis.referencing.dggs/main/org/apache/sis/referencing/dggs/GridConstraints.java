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
 * Set of descriptive information on the dggrs cell properties.
 *
 * @author Johann Sorel (Geomatys)
 * @see https://docs.ogc.org/as/20-040r3/20-040r3.html#tab-DGG_GridConstraint
 */
public final class GridConstraints extends CodeList<GridConstraints> {

    /**
     * We need to construct values with `valueOf(String)` instead of the constructor because this package is not
     * exported to GeoAPI. See `CodeList` class javadoc.
     */

    /**
     * Cell edges are parallel to the base CRS’s coordinate system axes.
     */
    public static final GridConstraints cellAxisAligned;

    /**
     * Variation in shape between all the cells in each DiscreteGlobalGrid is minimized.
     */
    public static final GridConstraints cellConformal;

    /**
     * Variation in bearing from one cell’s representative position to the next neighboring cell’s representative
     * positions in each DiscreteGlobalGrid is minimized.
     */
    public static final GridConstraints cellEquiAngular;

    /**
     * ariation in distance from a cell’s representative position to all of it’s neighboring cell’s representative
     * positions in each DiscreteGlobalGrid is minimized.
     */
    public static final GridConstraints cellEquiDistant;

    /**
     * Each parent cell of dimension greater than 2 has a child cell for which the cell⇐zone.representativePosition
     * lies on each of the parent’s faces (two-Dimensional topological boundary element)
     */
    public static final GridConstraints cellEquiSized;

    /**
     * All code list values created in the currently running <abbr>JVM</abbr>.
     */
    private static final List<GridConstraints> VALUES = initialValues(
        // Inline assignments for getting compiler error if a field is missing or duplicated.
        cellAxisAligned = new GridConstraints("cellAxisAligned"),
        cellConformal   = new GridConstraints("cellConformal"),
        cellEquiAngular = new GridConstraints("cellEquiAngular"),
        cellEquiDistant = new GridConstraints("cellEquiDistant"),
        cellEquiSized   = new GridConstraints("cellEquiSized"));

    private final String description;

    /**
     * Constructs an element of the given name.
     *
     * @param name the name of the new element.
     */
    private GridConstraints(final String name) {
        this(name, null);
    }

    private GridConstraints(final String name, String description) {
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
    public GridConstraints[] family() {
        return VALUES.toArray(GridConstraints[]::new);
    }

    /**
     * Returns the sub zone order type that matches the given string, or returns a new one if none match it.
     *
     * @param code the name of the code to fetch or to create.
     * @return a code matching the given name.
     */
    public static GridConstraints valueOf(String code) {
        return valueOf(VALUES, code, GridConstraints::new);
    }

    /**
     * Returns the sub zone order type that matches the given string, or returns a new one if none match it.
     *
     * @param code the name of the code to fetch or to create.
     * @param description if not found set this description to the newly created instance
     * @return a code matching the given name.
     */
    public static GridConstraints valueOf(String code, String description) {
        return valueOf(VALUES, code, (c) -> new GridConstraints(c, description));
    }
}
