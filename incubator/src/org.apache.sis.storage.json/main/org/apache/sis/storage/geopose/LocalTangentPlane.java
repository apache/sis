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
package org.apache.sis.storage.geopose;

import java.util.Map;
import javax.measure.Unit;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.util.FactoryException;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.crs.DefaultDerivedCRS;
import org.apache.sis.referencing.cs.DefaultCartesianCS;
import org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;


/**
 * Origin of a local tangent plane east-north-up frame, and the reference system it defines.
 *
 * <p>This is the inner frame of every Basic GeoPose target, and the frame denoted by the
 * {@code LTP-ENU} identifier of the {@code /geopose/1.0} authority. The derived reference system
 * is built over WGS 84 geographic 3D (EPSG::4979) using the EPSG::9837
 * <q>Geographic/topocentric conversions</q> method.</p>
 *
 * <ul>
 *   <li>Latitude and longitude are in decimal degrees, height is in metres.</li>
 *   <li>The derived axes are east, north and up, in metres.</li>
 *   <li>Instances are immutable.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see "OGC 21-056r11 GeoPose 1.0, Annex B, Local Tangent Plane East-North-Up"
 */
public class LocalTangentPlane {

    /**
     * Name of the EPSG::9837 operation method converting geographic to topocentric coordinates.
     */
    private static final String METHOD_NAME = "Geographic/topocentric conversions";

    private final double latitude;
    private final double longitude;
    private final double height;

    /**
     * Creates a new local tangent plane centered on the given geodetic origin.
     *
     * @param  latitude   latitude of the origin, in decimal degrees
     * @param  longitude  longitude of the origin, in decimal degrees
     * @param  height     ellipsoidal height of the origin, in metres
     */
    public LocalTangentPlane(final double latitude, final double longitude, final double height) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.height = height;
    }

    /**
     * Returns the latitude of the origin.
     *
     * @return latitude in decimal degrees
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Returns the longitude of the origin.
     *
     * @return longitude in decimal degrees
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Returns the ellipsoidal height of the origin.
     *
     * @return height in metres
     */
    public double getHeight() {
        return height;
    }

    /**
     * Returns the east-north-up reference system centered on this origin.
     *
     * @return the derived reference system, in metres
     * @throws FactoryException if the EPSG::9837 method is unavailable or the system cannot be built
     */
    public DefaultDerivedCRS toCoordinateReferenceSystem() throws FactoryException {
        final GeographicCRS base = CommonCRS.WGS84.geographic3D();
        final OperationMethod method = DefaultMathTransformFactory.provider().getOperationMethod(METHOD_NAME);
        final ParameterValueGroup values = method.getParameters().createValue();
        values.parameter("Latitude of topocentric origin").setValue(latitude, Units.DEGREE);
        values.parameter("Longitude of topocentric origin").setValue(longitude, Units.DEGREE);
        values.parameter("Ellipsoidal height of topocentric origin").setValue(height, Units.METRE);

        final Conversion conversion = DefaultCoordinateOperationFactory.provider()
                .createDefiningConversion(name("Geographic to topocentric"), method, values);
        return DefaultDerivedCRS.create(name("LTP-ENU"), base, conversion, createEastNorthUp());
    }

    /**
     * Returns the Cartesian coordinate system with east, north and up axes in metres.
     */
    private static CartesianCS createEastNorthUp() {
        final Unit<?> metre = Units.METRE;
        final CoordinateSystemAxis east  = new DefaultCoordinateSystemAxis(name("Topocentric East"),  "U", AxisDirection.EAST,  metre);
        final CoordinateSystemAxis north = new DefaultCoordinateSystemAxis(name("Topocentric North"), "V", AxisDirection.NORTH, metre);
        final CoordinateSystemAxis up    = new DefaultCoordinateSystemAxis(name("Topocentric height"), "W", AxisDirection.UP,   metre);
        return new DefaultCartesianCS(name("Topocentric East-North-Up"), east, north, up);
    }

    /**
     * Returns the property map naming an identified object.
     */
    private static Map<String,?> name(final String name) {
        return Map.of(IdentifiedObject.NAME_KEY, name);
    }

    @Override
    public String toString() {
        return "LocalTangentPlane[latitude=" + latitude + ", longitude=" + longitude + ", height=" + height + ']';
    }
}
