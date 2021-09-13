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

import org.opengis.util.FactoryException;
import org.opengis.referencing.cs.VerticalCS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.Conversion;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.referencing.AxisDirections;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.cs.AxisFilter;
import org.apache.sis.referencing.operation.DefaultConversion;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.internal.referencing.ReferencingUtilities.getPropertiesForModifiedCRS;


/**
 * Helper class for separating the ellipsoidal height from the horizontal part of a CRS.
 * This is the converse of {@link org.apache.sis.internal.referencing.EllipsoidalHeightCombiner}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
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
    private final boolean vertical;

    /**
     * Creates a new separator for a CRS having the given datum.
     *
     * @param  datum     the datum of the CRS to separate.
     * @param  vertical  whether to extract the vertical component ({@code true}) or the horizontal component ({@code false}).
     */
    EllipsoidalHeightSeparator(final GeodeticDatum datum, final boolean vertical) {
        this.datum    = datum;
        this.vertical = vertical;
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
     * Extracts the horizontal or vertical component of the coordinate reference system.
     *
     * @param  crs  the coordinate reference system from which to extract the horizontal or vertical component.
     * @return the requested component.
     * @throws IllegalArgumentException if the specified coordinate system can not be filtered.
     *         It may be because the coordinate system would contain an illegal number of axes,
     *         or because an axis would have an unexpected direction or unexpected unit of measurement.
     * @throws ClassCastException if a coordinate system is not of the expected type.
     */
    SingleCRS separate(final SingleCRS crs) throws FactoryException {
        final CoordinateSystem cs = CoordinateSystems.replaceAxes(crs.getCoordinateSystem(), this);
        if (vertical) {
            VerticalCRS component = CommonCRS.Vertical.ELLIPSOIDAL.crs();
            if (!Utilities.equalsIgnoreMetadata(component.getCoordinateSystem(), cs)) {
                component = factory().createVerticalCRS(getPropertiesForModifiedCRS(component), component.getDatum(), (VerticalCS) cs);
            }
            return component;
        }
        /*
         * Horizontal CRS requested. If geographic, try to use one of the pre-defined instances if suitable.
         * If no pre-defined instance match, create a new CRS.
         */
        if (crs instanceof GeodeticCRS) {
            if (!(cs instanceof EllipsoidalCS)) {
                throw new IllegalArgumentException(Errors.format(
                        Errors.Keys.UnsupportedCoordinateSystem_1, IdentifiedObjects.getName(cs, null)));
            }
            final CommonCRS ref = CommonCRS.WGS84;
            if (Utilities.equalsIgnoreMetadata(ref.geographic().getCoordinateSystem(), cs)) {
                final CommonCRS c = CommonCRS.forDatum(datum);
                if (c != null) return c.geographic();
            } else if (Utilities.equalsIgnoreMetadata(ref.normalizedGeographic().getCoordinateSystem(), cs)) {
                final CommonCRS c = CommonCRS.forDatum(datum);
                if (c != null) return c.normalizedGeographic();
            }
            return factory().createGeographicCRS(getPropertiesForModifiedCRS(crs), datum, (EllipsoidalCS) cs);
        }
        /*
         * In the projected CRS case, in addition of reducing the number of dimensions in the CartesianCS,
         * we also need to reduce the number of dimensions in the base CRS and in the conversion.
         */
        if (crs instanceof ProjectedCRS) {
            GeographicCRS baseCRS = ((ProjectedCRS) crs).getBaseCRS();
            if (ReferencingUtilities.getDimension(baseCRS) != 2) {
                baseCRS = (GeographicCRS) separate(baseCRS);
            }
            Conversion projection = ((ProjectedCRS) crs).getConversionFromBase();
            /*
             * The conversion object of the given CRS has a base (source) CRS and a target CRS that are not
             * the ones of the new `ProjectedCRS` to create. In addition it has a `MathTransform` expecting
             * three-dimensional coordinates, while we need 2 dimensions. We can not use that transform even
             * after reducing its number of dimensions (with `TransformSeparator`) because the `ProjectedCRS`
             * constructor expects a normalized transform, while the `projection.getMathTransform()` may not
             * be normalized. So we are better to let constructor recreate the transform from the parameters.
             */
            projection = new DefaultConversion(getPropertiesForModifiedCRS(projection),
                                projection.getMethod(), null, projection.getParameterValues());
            return factory().createProjectedCRS(getPropertiesForModifiedCRS(crs), baseCRS, projection, (CartesianCS) cs);
        }
        throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedType_1, crs.getClass()));
    }
}
