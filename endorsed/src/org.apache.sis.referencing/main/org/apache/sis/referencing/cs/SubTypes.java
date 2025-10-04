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
package org.apache.sis.referencing.cs;

import java.util.Map;
import org.opengis.referencing.cs.AffineCS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.CylindricalCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.LinearCS;
import org.opengis.referencing.cs.PolarCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.TimeCS;
import org.opengis.referencing.cs.VerticalCS;
import org.apache.sis.referencing.internal.shared.AxisDirections;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.referencing.cs.UserDefinedCS;
import org.apache.sis.referencing.legacy.DefaultUserDefinedCS;


/**
 * Implementation of {@link AbstractCS} methods that require knowledge about subclasses.
 * Those methods are defined in a separated static class for avoiding class loading of all
 * coordinate system implementations before necessary.
 *
 * <p>This class currently provides implementation for the following methods:</p>
 * <ul>
 *   <li>{@link AbstractCS#castOrCopy(CoordinateSystem)}</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class SubTypes {
    /**
     * Do not allow instantiation of this class.
     */
    private SubTypes() {
    }

    /**
     * Returns a SIS implementation for the given coordinate system.
     *
     * @see AbstractCS#castOrCopy(CoordinateSystem)
     */
    @SuppressWarnings("deprecation")
    static AbstractCS castOrCopy(final CoordinateSystem object) {
        if (object instanceof AffineCS) {
            return DefaultAffineCS.castOrCopy((AffineCS) object);
        }
        if (object instanceof SphericalCS) {
            return DefaultSphericalCS.castOrCopy((SphericalCS) object);
        }
        if (object instanceof EllipsoidalCS) {
            return DefaultEllipsoidalCS.castOrCopy((EllipsoidalCS) object);
        }
        if (object instanceof CylindricalCS) {
            return DefaultCylindricalCS.castOrCopy((CylindricalCS) object);
        }
        if (object instanceof PolarCS) {
            return DefaultPolarCS.castOrCopy((PolarCS) object);
        }
        if (object instanceof LinearCS) {
            return DefaultLinearCS.castOrCopy((LinearCS) object);
        }
        if (object instanceof VerticalCS) {
            return DefaultVerticalCS.castOrCopy((VerticalCS) object);
        }
        if (object instanceof TimeCS) {
            return DefaultTimeCS.castOrCopy((TimeCS) object);
        }
        if (object instanceof UserDefinedCS) {
            return DefaultUserDefinedCS.castOrCopy((UserDefinedCS) object);
        }
        /*
         * Intentionally check for AbstractCS after the interfaces because user may have defined his own
         * subclass implementing the interface. If we were checking for AbstractCS before the interfaces,
         * the returned instance could have been a user subclass without the JAXB annotations required
         * for XML marshalling.
         */
        if (object == null || object instanceof AbstractCS) {
            return (AbstractCS) object;
        }
        return new AbstractCS(object);
    }

    /**
     * Creates a one-dimensional coordinate system derived from the specified CS.
     *
     * @param  original  the two- or three-dimensional CRS from which to extract an axis.
     * @param  name      name of the new coordinate system.
     * @param  axes      the user-specified coordinate system axes. Only the first one will be used.
     * @return the one-dimensional coordinate system with the specified axis.
     */
    static AbstractCS createOneDimensional(final AbstractCS original, final String name, final CoordinateSystemAxis[] axes) {
        final CoordinateSystemAxis axis = axes[0];
        final AxisDirection dir = AxisDirections.absolute(axis.getDirection());
        final boolean isTemporal;
        if (dir == AxisDirection.UP) {
            isTemporal = false;
        } else if (dir == AxisDirection.FUTURE) {           // Happen with Minkowski coordinate system.
            isTemporal = true;
        } else {
            throw AbstractCS.unexpectedDimension(axes, 2, 3);
        }
        final Map<String,?> properties = original.getPropertiesWithoutIdentifiers(name);
        if (isTemporal) {
            return new DefaultTimeCS(properties, axis);
        } else {
            return new DefaultVerticalCS(properties, axis);
        }
    }
}
