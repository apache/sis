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

import java.util.logging.Logger;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.util.logging.Logging;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.util.FactoryException;


/**
 * Conversions between the spatial reference identifier of the extended geometry formats and a
 * {@link CoordinateReferenceSystem}. A <abbr>SRID</abbr> is the numeric part of an <abbr>EPSG</abbr>
 * code, and is what {@code EWKT} and {@code EWKB} carry in place of a full system definition.
 *
 * <p>Neither method changes the axis order, so a system read back from a code is the one the
 * <abbr>EPSG</abbr> authority defines, which for a geographic system is (<var>latitude</var>,
 * <var>longitude</var>) and not the (<var>longitude</var>, <var>latitude</var>) order that some
 * databases assume.</p>
 *
 * <h2>Heights</h2>
 * A spatial reference identifier usually names a two-dimensional system, while the geometry it
 * describes may have a <var>z</var> ordinate: {@code SRID=4326} on a {@code POINT Z} is the usual
 * way of writing a position above the ellipsoid of <abbr>EPSG</abbr>:4326. The two directions
 * therefore do not map one system onto one code:
 *
 * <ul>
 *   <li>{@link #forCode(int, int)} adds an {@linkplain CommonCRS.Vertical#ELLIPSOIDAL ellipsoidal
 *       height} to a two-dimensional system when the geometry needs three dimensions.</li>
 *   <li>{@link #of(CoordinateReferenceSystem)} looks at the
 *       {@linkplain CRS#getHorizontalComponent horizontal component} of a three-dimensional system
 *       when the system itself has no code, which is the converse operation.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
final class Srid {
    /**
     * The logger where to report the absence of the <abbr>EPSG</abbr> definitions, which keeps a
     * geometry from being written with its identifier but does not keep it from being written.
     */
    private static final Logger LOGGER = Logger.getLogger("org.apache.sis.geometries");

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
     * {@link #UNDEFINED} if none can be determined.
     *
     * <p>Three sources are tried in turn, from the cheapest and most certain to the most costly:
     * the identifier the system itself carries, then the identifier of its
     * {@linkplain CRS#getHorizontalComponent horizontal component}, then a search of the
     * <abbr>EPSG</abbr> definitions for a system equivalent to that horizontal component. The
     * last one is needed because a three-dimensional system built by adding an ellipsoidal height
     * to a two-dimensional one keeps no identifier — neither the combined system nor the
     * horizontal component extracted back from it carries the code the height was added to.</p>
     *
     * @param  crs  the system whose identifier to return, or {@code null}.
     * @return the spatial reference identifier, or {@link #UNDEFINED} if none.
     */
    static int of(final CoordinateReferenceSystem crs) {
        if (crs == null) {
            return UNDEFINED;
        }
        int srid = declaredCode(crs);
        if (srid == UNDEFINED) {
            final SingleCRS horizontal = CRS.getHorizontalComponent(crs);
            if (horizontal != null && horizontal != crs) {
                srid = declaredCode(horizontal);
                if (srid == UNDEFINED) try {
                    final Integer code = IdentifiedObjects.lookupEPSG(horizontal);
                    if (code != null) {
                        srid = code;
                    }
                } catch (FactoryException e) {
                    // The definitions are not available. The geometry is written without a SRID.
                    Logging.ignorableException(LOGGER, Srid.class, "of", e);
                }
            }
        }
        return srid;
    }

    /**
     * Returns the <abbr>EPSG</abbr> code which the given system already carries, or
     * {@link #UNDEFINED} if it carries none. The <abbr>EPSG</abbr> geodetic dataset is not
     * searched, so this method is cheap and never fails.
     */
    private static int declaredCode(final CoordinateReferenceSystem crs) {
        final Identifier id = IdentifiedObjects.getIdentifier(crs, Citations.EPSG);
        if (id != null) try {
            return Integer.parseInt(id.getCode());
        } catch (NumberFormatException e) {
            // An EPSG code which is not a number cannot be written as a SRID.
        }
        return UNDEFINED;
    }

    /**
     * Returns the coordinate reference system which the <abbr>EPSG</abbr> authority defines for
     * the given spatial reference identifier, in the given number of dimensions.
     *
     * <p>A two-dimensional system asked for three dimensions is completed with an
     * {@linkplain CommonCRS.Vertical#ELLIPSOIDAL ellipsoidal height}, which is what the
     * <var>z</var> ordinate of a geometry carrying such an identifier means. Any other
     * disagreement between the two is an error: dropping a dimension would silently discard
     * ordinates.</p>
     *
     * @param  srid       the spatial reference identifier to resolve.
     * @param  dimension  number of ordinates the positions have, 2 or 3.
     * @return the coordinate reference system of that identifier, of exactly {@code dimension}
     *         dimensions.
     * @throws IllegalArgumentException if the identifier is unknown, if the definitions are not
     *         available, or if the system cannot describe positions of that many ordinates.
     */
    static CoordinateReferenceSystem forCode(final int srid, final int dimension) {
        final CoordinateReferenceSystem crs;
        try {
            crs = CRS.forCode("EPSG:" + srid);
        } catch (FactoryException e) {
            throw new IllegalArgumentException("Cannot resolve the spatial reference identifier "
                    + srid + " to a coordinate reference system: " + e.getMessage(), e);
        }
        final int actual = crs.getCoordinateSystem().getDimension();
        if (actual == dimension) {
            return crs;
        }
        if (actual == 2 && dimension == 3) try {
            return CRS.compound(crs, CommonCRS.Vertical.ELLIPSOIDAL.crs());
        } catch (FactoryException e) {
            throw new IllegalArgumentException("Cannot add an ellipsoidal height to the coordinate"
                    + " reference system of the spatial reference identifier " + srid + ": " + e.getMessage(), e);
        }
        throw new IllegalArgumentException("The coordinate reference system of the spatial reference"
                + " identifier " + srid + " has " + actual + " dimensions, which cannot describe"
                + " positions of " + dimension + " ordinates.");
    }
}
