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
package org.apache.sis.geometries.adapter;

import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;


/**
 * Conversions between the spatial reference identifier of the extended geometry formats and a
 * {@link CoordinateReferenceSystem}. A <abbr>SRID</abbr> is the numeric part of an <abbr>EPSG</abbr>
 * code, and is what {@code EWKT} and {@code EWKB} carry in place of a full system definition.
 *
 * <p>The mapping is the one of the identifier the system already carries, never a search for an
 * equivalent definition: {@link #of(CoordinateReferenceSystem)} reads the identifier and
 * {@link #forCode(int)} builds the system the authority defines for that code. Neither method
 * changes the axis order, so a system read back from a code is the one the <abbr>EPSG</abbr>
 * authority defines, which for a geographic system is (<var>latitude</var>, <var>longitude</var>)
 * and not the (<var>longitude</var>, <var>latitude</var>) order that some databases assume.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
final class Srid {
    /**
     * Value meaning that the coordinate reference system has no spatial reference identifier,
     * which is how the extended formats say that they carry no system.
     */
    static final int UNDEFINED = 0;

    /**
     * Do not allow instantiation of this holder of static methods.
     */
    private Srid() {
    }

    /**
     * Returns the spatial reference identifier of the given coordinate reference system, or
     * {@link #UNDEFINED} if it has none. Only the identifiers the system already carries are
     * examined; the <abbr>EPSG</abbr> geodetic dataset is not searched for an equivalent
     * definition, so a system built from axes and a datum has no identifier whatever it describes.
     *
     * @param  crs  the system whose identifier to return, or {@code null}.
     * @return the spatial reference identifier, or {@link #UNDEFINED} if none.
     */
    static int of(final CoordinateReferenceSystem crs) {
        if (crs != null) {
            final Identifier id = IdentifiedObjects.getIdentifier(crs, Citations.EPSG);
            if (id != null) try {
                return Integer.parseInt(id.getCode());
            } catch (NumberFormatException e) {
                // An EPSG code which is not a number cannot be written as a SRID. Fall through.
            }
        }
        return UNDEFINED;
    }

    /**
     * Returns the coordinate reference system which the <abbr>EPSG</abbr> authority defines for
     * the given spatial reference identifier.
     *
     * @param  srid  the spatial reference identifier to resolve.
     * @return the coordinate reference system of that identifier.
     * @throws IllegalArgumentException if the identifier is unknown, or if the definitions
     *         are not available.
     */
    static CoordinateReferenceSystem forCode(final int srid) {
        try {
            return CRS.forCode("EPSG:" + srid);
        } catch (FactoryException e) {
            throw new IllegalArgumentException("Cannot resolve the spatial reference identifier "
                    + srid + " to a coordinate reference system: " + e.getMessage(), e);
        }
    }
}
