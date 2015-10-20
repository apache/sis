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
package org.apache.sis.referencing.crs;

import java.util.Map;
import java.util.Comparator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.DerivedCRS;
import org.opengis.referencing.crs.EngineeringCRS;
import org.opengis.referencing.crs.GeocentricCRS;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ImageCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.datum.GeodeticDatum;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.cs.AxesConvention;


/**
 * Implementation of {@link AbstractCRS} methods that require knowledge about subclasses.
 * Those methods are defined in a separated static class for avoiding class loading of all
 * coordinate reference system implementations before necessary.
 *
 * <p>This class currently provides implementation for the following methods:</p>
 * <ul>
 *   <li>{@link AbstractCRS#castOrCopy(CoordinateReferenceSystem)}</li>
 *   <li>{@link DefaultCompoundCRS#forConvention(AxesConvention)}</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
final class SubTypes implements Comparator<Object> {
    /**
     * CRS types to sort first in a compound CRS. Any type not in this list will be sorted last.
     * Used for implementation of {@link #BY_TYPE} comparator.
     */
    private static final Class<?>[] TYPE_ORDER = {
        ProjectedCRS.class,
        GeodeticCRS.class,
        VerticalCRS.class,
        TemporalCRS.class
    };

    /**
     * A comparator for sorting CRS objects by their types.
     * The comparison sorts projected CRS first, followed by geodetic, vertical then temporal CRS.
     */
    static final Comparator<Object> BY_TYPE = new SubTypes();

    /**
     * Do not allow instantiation of this class (except the singleton).
     */
    private SubTypes() {
    }

    /**
     * Returns the index of the interface implemented by the given object.
     */
    private static int indexOf(final Object object) {
        int i = 0;
        while (i < TYPE_ORDER.length) {
            if (TYPE_ORDER[i].isInstance(object)) {
                break;
            }
            i++;
        }
        return i;
    }

    /**
     * Implementation of {@link #BY_TYPE} comparator.
     */
    @Override
    public int compare(final Object o1, final Object o2) {
        return indexOf(o1) - indexOf(o2);
    }

    /**
     * Returns a SIS implementation for the given coordinate reference system.
     *
     * @see AbstractCRS#castOrCopy(CoordinateReferenceSystem)
     */
    static AbstractCRS castOrCopy(final CoordinateReferenceSystem object) {
        if (object instanceof DerivedCRS) {
            return DefaultDerivedCRS.castOrCopy((DerivedCRS) object);
        }
        if (object instanceof ProjectedCRS) {
            return DefaultProjectedCRS.castOrCopy((ProjectedCRS) object);
        }
        if (object instanceof GeodeticCRS) {
            if (object instanceof GeographicCRS) {
                return DefaultGeographicCRS.castOrCopy((GeographicCRS) object);
            }
            if (object instanceof GeocentricCRS) {
                return DefaultGeocentricCRS.castOrCopy((GeocentricCRS) object);
            }
            /*
             * The GeographicCRS and GeocentricCRS types are not part of ISO 19111.
             * ISO uses a single type, GeodeticCRS, for both of them and infer the
             * geographic or geocentric type from the coordinate system. We do this
             * check here for instantiating the most appropriate SIS type, but only
             * if we need to create a new object anyway (see below for rational).
             */
            if (object instanceof DefaultGeodeticCRS) {
                // Result of XML unmarshalling — keep as-is. We avoid creating a new object because it
                // would break object identities specified in GML document by the xlink:href attribute.
                // However we may revisit this policy in the future. See SC_CRS.setElement(AbstractCRS).
                return (DefaultGeodeticCRS) object;
            }
            final Map<String,?> properties = IdentifiedObjects.getProperties(object);
            final GeodeticDatum datum = ((GeodeticCRS) object).getDatum();
            final CoordinateSystem cs = object.getCoordinateSystem();
            if (cs instanceof EllipsoidalCS) {
                return new DefaultGeographicCRS(properties, datum, (EllipsoidalCS) cs);
            }
            if (cs instanceof SphericalCS) {
                return new DefaultGeocentricCRS(properties, datum, (SphericalCS) cs);
            }
            if (cs instanceof CartesianCS) {
                return new DefaultGeocentricCRS(properties, datum, (CartesianCS) cs);
            }
        }
        if (object instanceof VerticalCRS) {
            return DefaultVerticalCRS.castOrCopy((VerticalCRS) object);
        }
        if (object instanceof TemporalCRS) {
            return DefaultTemporalCRS.castOrCopy((TemporalCRS) object);
        }
        if (object instanceof EngineeringCRS) {
            return DefaultEngineeringCRS.castOrCopy((EngineeringCRS) object);
        }
        if (object instanceof ImageCRS) {
            return DefaultImageCRS.castOrCopy((ImageCRS) object);
        }
        if (object instanceof CompoundCRS) {
            return DefaultCompoundCRS.castOrCopy((CompoundCRS) object);
        }
        /*
         * Intentionally check for AbstractCRS after the interfaces because user may have defined his own
         * subclass implementing the interface. If we were checking for AbstractCRS before the interfaces,
         * the returned instance could have been a user subclass without the JAXB annotations required
         * for XML marshalling.
         */
        if (object == null || object instanceof AbstractCRS) {
            return (AbstractCRS) object;
        }
        return new AbstractCRS(object);
    }
}
