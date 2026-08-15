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
 * List of sub zone ordering methods.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SubZoneOrder extends CodeList<SubZoneOrder> {

    /**
     * We need to construct values with `valueOf(String)` instead of the constructor because this package is not
     * exported to GeoAPI. See `CodeList` class javadoc.
     */

    /**
     * Grid scan line ordering.
     */
    public static final SubZoneOrder SCANLINE;

    /**
     * Spiral from center cell.
     */
    public static final SubZoneOrder SPIRAL_FROM_CENTER;

    /**
     * Morton curve iteration.
     */
    public static final SubZoneOrder MORTON_CURVE;

    /**
     * Hilbert curve iteration.
     */
    public static final SubZoneOrder HILBERT_CURVE;

    /**
     * All code list values created in the currently running <abbr>JVM</abbr>.
     */
    private static final List<SubZoneOrder> VALUES = initialValues(
        // Inline assignments for getting compiler error if a field is missing or duplicated.
        SCANLINE           = new SubZoneOrder("SCANLINE"),
        SPIRAL_FROM_CENTER = new SubZoneOrder("SPIRAL_FROM_CENTER"),
        MORTON_CURVE       = new SubZoneOrder("MORTON_CURVE"),
        HILBERT_CURVE      = new SubZoneOrder("HILBERT_CURVE"));

    private final String description;

    /**
     * Constructs an element of the given name.
     *
     * @param name the name of the new element.
     */
    private SubZoneOrder(final String name) {
        this(name, null);
    }

    private SubZoneOrder(final String name, String description) {
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
    public SubZoneOrder[] family() {
        return VALUES.toArray(SubZoneOrder[]::new);
    }

    /**
     * Returns the sub zone order type that matches the given string, or returns a new one if none match it.
     *
     * @param code the name of the code to fetch or to create.
     * @return a code matching the given name.
     */
    public static SubZoneOrder valueOf(String code) {
        return valueOf(VALUES, code, SubZoneOrder::new);
    }

    /**
     * Returns the sub zone order type that matches the given string, or returns a new one if none match it.
     *
     * @param code the name of the code to fetch or to create.
     * @param description if not found set this description to the newly created instance
     * @return a code matching the given name.
     */
    public static SubZoneOrder valueOf(String code, String description) {
        return valueOf(VALUES, code, (c) -> new SubZoneOrder(c, description));
    }
}
