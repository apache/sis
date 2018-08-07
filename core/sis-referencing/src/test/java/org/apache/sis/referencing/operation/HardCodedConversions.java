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
package org.apache.sis.referencing.operation;

import java.util.Map;
import java.util.Collections;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.internal.referencing.provider.Mercator1SP;
import org.apache.sis.referencing.crs.DefaultProjectedCRS;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.cs.HardCodedCS;


/**
 * Collection of defining conversions for testing purpose.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final strictfp class HardCodedConversions {
    /**
     * A defining conversion for a <cite>Mercator (variant A)</cite> (also known as "1SP") projection
     * with a scale factor of 1.
     */
    public static final DefaultConversion MERCATOR;
    static {
        final OperationMethod method = new Mercator1SP();
        MERCATOR = new DefaultConversion(Collections.singletonMap(OperationMethod.NAME_KEY, "Mercator"),
                method, null, method.getParameters().createValue());
    }

    /**
     * Do not allow instantiation of this class.
     */
    private HardCodedConversions() {
    }

    /**
     * A two-dimensional Mercator projection using the WGS84 datum.
     * This CRS uses (<var>easting</var>, <var>northing</var>) ordinates in metres.
     * The base CRS uses (<var>longitude</var>, <var>latitude</var>) axes
     * and the prime meridian is Greenwich.
     *
     * <p>This CRS is equivalent to {@code EPSG:3395} except for base CRS axis order,
     * since EPSG puts latitude before longitude.</p>
     *
     * @return two-dimensional Mercator projection.
     */
    public static DefaultProjectedCRS mercator() {
        return new DefaultProjectedCRS(name("Mercator"),
                HardCodedCRS.WGS84, HardCodedConversions.MERCATOR, HardCodedCS.PROJECTED);
    }

    /**
     * A three-dimensional Mercator projection using the WGS84 datum.
     * This CRS uses (<var>easting</var>, <var>northing</var>, <var>height</var>) ordinates in metres.
     * The base CRS uses (<var>longitude</var>, <var>latitude</var>, <var>height</var>) axes
     * and the prime meridian is Greenwich.
     *
     * @return three-dimensional Mercator projection.
     */
    public static DefaultProjectedCRS mercator3D() {
        return new DefaultProjectedCRS(name("Mercator 3D"),
                HardCodedCRS.WGS84_3D, HardCodedConversions.MERCATOR, HardCodedCS.PROJECTED_3D);
    }

    /**
     * A two- or three-dimensional Mercator projection using the given base CRS.
     * This CRS uses (<var>easting</var>, <var>northing</var>) ordinates in metres.
     *
     * @param  baseCRS  the two- or three-dimensional base CRS.
     * @return two- or three-dimensional Mercator projection.
     */
    public static DefaultProjectedCRS mercator(final GeographicCRS baseCRS) {
        return new DefaultProjectedCRS(name("Mercator (other)"), baseCRS, HardCodedConversions.MERCATOR,
                baseCRS.getCoordinateSystem().getDimension() == 3 ? HardCodedCS.PROJECTED_3D : HardCodedCS.PROJECTED);
    }

    /**
     * Puts the given name in a property map CRS constructors.
     */
    private static Map<String,?> name(final String name) {
        return Collections.singletonMap(ProjectedCRS.NAME_KEY, name);
    }
}
