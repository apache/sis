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
import java.util.HashMap;
import java.util.Collections;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CSAuthorityFactory;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.util.FactoryException;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.Constants;


/**
 * Provides non-standard coordinate reference systems for the needs of SIS.
 * Those CRS are defined in the "SIS" namespace. Those CRS may be removed in any future version
 * if the EPSG database provides an equivalent CRS.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class NonStandardCRS {
    /**
     * The prefix for non-standard codes.
     */
    static final String PREFIX = Constants.SIS + ':';

    /**
     * The CRS created so far.
     */
    private static final Map<Integer,CoordinateReferenceSystem> CACHE = new HashMap<>();

    /**
     * Do not allow instantiation of this class.
     */
    private NonStandardCRS() {
    }

    /**
     * Returns a coordinate system for the given code, or {@code null} if none.
     * Codes recognized here are:
     *
     * <ul>
     *   <li>{@code "SIS:6404"} — a geocentric CRS using the EPSG:6404 spherical coordinate system.</li>
     * </ul>
     */
    static synchronized CoordinateReferenceSystem forCode(final String code) throws FactoryException {
        final Integer key;
        try {
            key = Integer.valueOf(code.substring(PREFIX.length()));
        } catch (NumberFormatException e) {
            return null;
        }
        CoordinateReferenceSystem crs = CACHE.get(key);
        if (crs == null) {
            switch (key) {
                case 6404: {
                    crs = DefaultFactories.forBuildin(CRSFactory.class).createGeocentricCRS(
                            Collections.singletonMap(CoordinateReferenceSystem.NAME_KEY, "Spherical"),
                            CommonCRS.SPHERE.datum(), (SphericalCS) getCoordinateSystem((short) 6404));
                    break;
                }
                default: return null;
                // More may be added in the future.
            }
            CACHE.put(key, crs);
        }
        return crs;
    }

    /**
     * Returns the coordinate system for the given EPSG code.
     */
    private static CoordinateSystem getCoordinateSystem(final short code) throws FactoryException {
        final CRSAuthorityFactory factory = CRS.getAuthorityFactory(Constants.EPSG);
        if (factory instanceof CSAuthorityFactory) {
            return ((CSAuthorityFactory) factory).createCoordinateSystem(String.valueOf(code));
        } else {
            return StandardDefinitions.createCoordinateSystem(code);
        }
    }
}
