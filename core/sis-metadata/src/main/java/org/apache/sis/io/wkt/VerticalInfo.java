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

import java.util.Map;
import java.util.Collections;
import javax.measure.unit.Unit;
import javax.measure.quantity.Length;
import org.opengis.util.FactoryException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.CSFactory;
import org.opengis.referencing.cs.VerticalCS;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.datum.VerticalDatumType;
import org.apache.sis.internal.metadata.AxisNames;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultVerticalExtent;


/**
 * Stores temporary information needed for completing the construction of an {@link DefaultVerticalExtent} instance.
 * WKT of vertical extents looks like:
 *
 * {@preformat wkt
 *     VERTICALEXTENT[-1000, 0, LENGTHUNIT[“metre”, 1]]
 * }
 *
 * But {@code DefaultVerticalExtent} has no {@code unit} property. Instead, {@code DefaultVerticalExtent} has a
 * {@code verticalCRS} property. The WKT specification said that heights are positive toward up and relative to
 * an unspecified mean sea level, but we will try to use the parsed vertical CRS instance if we find a suitable
 * one (i.e. one that defines gravity-related heights or depths), on the assumption that the vertical extent is
 * likely to be defined in the same vertical CRS.
 *
 * <p>This class can be understood as the converse of
 * {@link org.apache.sis.metadata.iso.extent.Extents#getVerticalRange}</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
final class VerticalInfo {
    /**
     * The next instance to resolve. This form a chained list.
     */
    private VerticalInfo next;

    /**
     * The vertical extent pending completion.
     */
    private final DefaultVerticalExtent extent;

    /**
     * The unit specified in the {@code VERTICALEXTENT} WKT element.
     */
    final Unit<Length> unit;

    /**
     * If a vertical CRS could be used pending only a change of units, that CRS.
     * Otherwise {@code null}.
     */
    private VerticalCRS compatibleCRS;

    /**
     * Adds to the chained list a new {@code DefaultVerticalExtent} instance pending completion.
     *
     * @param next    The existing {@code VerticalInfo} instance. Will become the next instance
     *                to process after {@code this} in a chain of {@code VerticalInfo}.
     * @param extents Where to add the vertical extent.
     * @param unit    The unit to assign to the {@code extent}. Can not be null.
     */
    VerticalInfo(final VerticalInfo next, final DefaultExtent extents, final double minimum, final double maximum, final Unit<Length> unit) {
        this.next   = next;
        this.unit   = unit;
        this.extent = new DefaultVerticalExtent(minimum, maximum, null);
        extents.getVerticalElements().add(extent);
    }

    /**
     * If the pending {@code DefaultVerticalExtent} can use the given CRS, completes the extent now.
     * This method invokes {@link DefaultVerticalExtent#setVerticalCRS(VerticalCRS)} with the given CRS if:
     *
     * <ul>
     *   <li>datum type is {@link VerticalDatumType#GEOIDAL},</li>
     *   <li>increasing height values are up, and</li>
     *   <li>axis unit of measurement is the given linear unit.</li>
     * </ul>
     *
     * This method processes also all other {@code VerticalInfo} instances in the chained list.
     *
     * @return The new head of the chained list (may be {@code this}), or {@code null} if the list
     *         became empty as a result of this operation.
     */
    final VerticalInfo resolve(final VerticalCRS crs) {
        if (crs != null && VerticalDatumType.GEOIDAL.equals(crs.getDatum().getVerticalDatumType())) {
            return resolve(crs, crs.getCoordinateSystem().getAxis(0));
        }
        return this;
    }

    /**
     * Implementation of {@link #resolve(VerticalCRS, CoordinateSystemAxis)} to be invoked recursively,
     * after we checked the datum type and fetched the axis once for all.
     */
    private VerticalInfo resolve(final VerticalCRS crs, final CoordinateSystemAxis axis) {
        if (next != null) {
            next = next.resolve(crs, axis);
        }
        final Unit<?> crsUnit = axis.getUnit();
        if (AxisDirection.UP.equals(axis.getDirection()) && unit.equals(crsUnit)) {
            extent.setVerticalCRS(crs);
            return next;
        } else if (unit.isCompatible(crsUnit)) {
            compatibleCRS = crs;
        }
        return this;
    }

    /**
     * Completes the extent with a new CRS using the units specified at construction time.
     * The CRS created by this method is implementation-dependent. The only guarantees are:
     *
     * <ul>
     *   <li>datum type is {@link VerticalDatumType#GEOIDAL},</li>
     *   <li>increasing height values are up, and</li>
     *   <li>axis unit of measurement is the given linear unit.</li>
     * </ul>
     *
     * If this method can not propose a suitable CRS, then it returns {@code this}.
     */
    final VerticalInfo complete(final CRSFactory crsFactory, final CSFactory csFactory) throws FactoryException {
        if (next != null) {
            next = next.complete(crsFactory, csFactory);
        }
        if (compatibleCRS == null) {
            return this;
        }
        final Object name;
        final String abbreviation;
        CoordinateSystemAxis axis = compatibleCRS.getCoordinateSystem().getAxis(0);
        final boolean isUP = AxisDirection.UP.equals(axis.getDirection());
        if (isUP) {
            name = axis.getName();
            abbreviation = axis.getAbbreviation();
        } else {
            name = AxisNames.GRAVITY_RELATED_HEIGHT;
            abbreviation = "H";
        }
        axis = csFactory.createCoordinateSystemAxis(properties(name), abbreviation, AxisDirection.UP, unit);
        /*
         * Naming policy (based on usage of names in the EPSG database):
         *
         *   - We can reuse the old axis name if (and only if) the direction is the same, because the axis
         *     names are constrained by the ISO 19111 specification in a way that do not include the units
         *     of measurement. Examples: "Gravity-related height", "Depth".
         *
         *   - We can not reuse the previous Coordinate System name, because it often contains the axis
         *     abbreviation and unit. Examples: "Vertical CS. Axis: height (H). Orientation: up. UoM: m.".
         *     Since we are lazy, we will reuse the axis name instead, which is more neutral.
         *
         *   - We generally can reuse the CRS name because those names tend to refer to the datum (which is
         *     unchanged) rather than the coordinate system. Examples: "Low Water depth", "NGF Lallemand height",
         *     "JGD2011 (vertical) height". However we make an exception if the direction is down, because in such
         *     cases the previous name may contain terms like "depth", which are not appropriate for our new CRS.
         */
        final VerticalCS cs = csFactory.createVerticalCS (properties(axis.getName()), axis);
        extent.setVerticalCRS(crsFactory.createVerticalCRS(
                properties((isUP ? compatibleCRS : axis).getName()), compatibleCRS.getDatum(), cs));
        return next;
    }

    /**
     * Convenience method for creating the map of properties to give to the factory method.
     */
    private static Map<String,?> properties(final Object name) {
        return Collections.singletonMap(IdentifiedObject.NAME_KEY, name);
    }
}
