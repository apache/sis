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
package org.apache.sis.internal.metadata;


/**
 * Constants for axis names specified by ISO 19111 and ISO 19162.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final class AxisNames {
    /**
     * The ISO 19162 <cite>"Longitude"</cite> name.
     */
    public static final String LONGITUDE = "Longitude";

    /**
     * The ISO 19162 <cite>"Latitude"</cite> name.
     */
    public static final String LATITUDE = "Latitude";

    /**
     * The ISO 19111 <cite>"geodetic longitude"</cite> name. Abbreviation is "λ" (lambda).
     */
    public static final String GEODETIC_LONGITUDE = "Geodetic longitude";

    /**
     * The ISO 19111 <cite>"geodetic latitude"</cite> name. Abbreviation is "φ" (phi).
     */
    public static final String GEODETIC_LATITUDE = "Geodetic latitude";

    /**
     * The ISO 19111 <cite>"spherical longitude"</cite> name. Abbreviation is "θ" (theta).
     */
    public static final String SPHERICAL_LONGITUDE = "Spherical longitude";

    /**
     * The ISO 19111 <cite>"spherical latitude"</cite> name. Abbreviation is "φ′" (phi prime).
     */
    public static final String SPHERICAL_LATITUDE = "Spherical latitude";

    /**
     * The ISO 19111 <cite>"ellipsoidal height"</cite> name. Abbreviation is lower case <cite>"h"</cite>.
     */
    public static final String ELLIPSOIDAL_HEIGHT = "Ellipsoidal height";

    /**
     * The ISO 19111 <cite>"gravity-related height"</cite> name. Abbreviation is upper case <cite>"H"</cite>.
     */
    public static final String GRAVITY_RELATED_HEIGHT = "Gravity-related height";

    /**
     * The ISO 19111 <cite>"depth"</cite> name.
     */
    public static final String DEPTH = "Depth";

    /**
     * The ISO 19111 <cite>"Geocentric X"</cite> name. Abbreviation is upper case <cite>"X"</cite>.
     */
    public static final String GEOCENTRIC_X = "Geocentric X";

    /**
     * The ISO 19111 <cite>"Geocentric Y"</cite> name. Abbreviation is upper case <cite>"Y"</cite>.
     */
    public static final String GEOCENTRIC_Y = "Geocentric Y";

    /**
     * The ISO 19111 <cite>"Geocentric Z"</cite> name. Abbreviation is upper case <cite>"Z"</cite>.
     */
    public static final String GEOCENTRIC_Z = "Geocentric Z";

    /**
     * The ISO 19111 <cite>"geocentric radius"</cite> name. Abbreviation is lower case <cite>"r"</cite>.
     */
    public static final String GEOCENTRIC_RADIUS = "Geocentric radius";

    /**
     * The ISO 19111 <cite>"easting"</cite> name. Abbreviation is upper case <cite>"E"</cite>.
     */
    public static final String EASTING = "Easting";

    /**
     * The ISO 19111 <cite>"westing"</cite> name. Abbreviation is upper case <cite>"W"</cite>.
     */
    public static final String WESTING = "Westing";

    /**
     * The ISO 19111 <cite>"northing"</cite> name. Abbreviation is upper case <cite>"N"</cite>.
     */
    public static final String NORTHING = "Northing";

    /**
     * The ISO 19111 <cite>"southing"</cite> name. Abbreviation is upper case <cite>"S"</cite>.
     */
    public static final String SOUTHING = "Southing";

    /**
     * Do not allow instantiation of this class.
     */
    private AxisNames() {
    }
}
