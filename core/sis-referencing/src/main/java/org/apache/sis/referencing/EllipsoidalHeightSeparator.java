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
package org.apache.sis.referencing;

import java.util.Map;
import java.util.Collections;
import org.opengis.util.FactoryException;
import org.opengis.referencing.cs.VerticalCS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.Conversion;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.metadata.AxisDirections;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.cs.AxisFilter;
import org.apache.sis.util.Utilities;


/**
 * Helper class for separating the ellipsoidal height from the horizontal part of a CRS.
 * This is the converse of {@link org.apache.sis.internal.referencing.EllipsoidalHeightCombiner}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see org.apache.sis.internal.referencing.EllipsoidalHeightCombiner
 *
 * @since 1.0
 * @module
 */
final class EllipsoidalHeightSeparator implements AxisFilter {
    /**
     * The value of {@link SingleCRS#getDatum()}.
     */
    private final GeodeticDatum datum;

    /**
     * Whether to extract the vertical component ({@code true}) or the horizontal component ({@code false}).
     */
    private boolean vertical;

    /**
     * Creates a new separator for a CRS having the given datum.
     */
    EllipsoidalHeightSeparator(final GeodeticDatum datum) {
        this.datum = datum;
    }

    /**
     * Returns {@code true} if the given axis shall be included in the new coordinate system.
     */
    @Override
    public boolean accept(final CoordinateSystemAxis axis) {
        return AxisDirections.isVertical(axis.getDirection()) == vertical;
    }

    /**
     * The factory to use for creating new coordinate reference system.
     */
    private static CRSFactory factory() {
        return DefaultFactories.forBuildin(CRSFactory.class);
    }

    /**
     * Returns properties with the name of the given CRS.
     */
    private static Map<String,?> properties(final SingleCRS component) {
        return Collections.singletonMap(SingleCRS.NAME_KEY, component.getName());
    }

    /**
     * Extracts the horizontal or vertical component of the coordinate reference system.
     *
     * @param  crs       the coordinate reference system from which to extract the horizontal or vertical component.
     * @param  vertical  whether to extract the vertical component ({@code true}) or the horizontal component ({@code false}).
     * @return the requested component.
     * @throws IllegalArgumentException if the specified coordinate system can not be filtered.
     *         It may be because the coordinate system would contain an illegal number of axes,
     *         or because an axis would have an unexpected direction or unexpected unit of measurement.
     * @throws ClassCastException if a coordinate system is not of the expected type.
     */
    SingleCRS separate(final SingleCRS crs, final boolean vertical) throws FactoryException {
        this.vertical = vertical;
        final CoordinateSystem cs = CoordinateSystems.replaceAxes(crs.getCoordinateSystem(), this);
        if (vertical) {
            VerticalCRS component = CommonCRS.Vertical.ELLIPSOIDAL.crs();
            if (!Utilities.equalsIgnoreMetadata(component.getCoordinateSystem(), cs)) {
                component = factory().createVerticalCRS(properties(component), component.getDatum(), (VerticalCS) cs);
            }
            return component;
        }
        if (crs instanceof GeographicCRS) {
            final CommonCRS ref = CommonCRS.WGS84;
            if (Utilities.equalsIgnoreMetadata(ref.geographic().getCoordinateSystem(), cs)) {
                final CommonCRS c = CommonCRS.forDatum(datum);
                if (c != null) return c.geographic();
            } else if (Utilities.equalsIgnoreMetadata(ref.normalizedGeographic().getCoordinateSystem(), cs)) {
                final CommonCRS c = CommonCRS.forDatum(datum);
                if (c != null) return c.normalizedGeographic();
            }
            return factory().createGeographicCRS(properties(crs), datum, (EllipsoidalCS) cs);
        }
        if (crs instanceof ProjectedCRS) {
            GeographicCRS baseCRS = ((ProjectedCRS) crs).getBaseCRS();
            if (ReferencingUtilities.getDimension(baseCRS) != 2) {
                baseCRS = (GeographicCRS) separate(baseCRS, false);
            }
            Conversion projection = ((ProjectedCRS) crs).getConversionFromBase();
            return factory().createProjectedCRS(properties(crs), baseCRS, projection, (CartesianCS) cs);
        }
        throw new IllegalArgumentException();
    }
}
