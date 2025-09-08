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
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.referencing.crs.DefaultProjectedCRS;
import org.apache.sis.referencing.operation.provider.Mercator1SP;
import org.apache.sis.referencing.operation.provider.TransverseMercator;
import org.apache.sis.referencing.operation.provider.LambertConformal1SP;
import org.apache.sis.referencing.operation.provider.LambertConformal2SP;
import org.apache.sis.referencing.operation.provider.PolarStereographicB;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.util.privy.Constants;

// Test dependencies
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.cs.HardCodedCS;


/**
 * Collection of defining conversions for testing purpose.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class HardCodedConversions {
    /**
     * A defining conversion for a <cite>Mercator (variant A)</cite> (also known as "1SP") projection
     * with a scale factor of 1.
     */
    public static final DefaultConversion MERCATOR;
    static {
        final OperationMethod method = new Mercator1SP();
        MERCATOR = create("Mercator", method, method.getParameters().createValue());
    }

    /**
     * A defining conversion for a <cite>Universal Transverse Mercator zone 9</cite> projection.
     * Pseudo Well-Known Text for the {@link org.opengis.referencing.operation.MathTransform}:
     *
     * {@snippet lang="wkt" :
     *   Param_MT["Transverse Mercator",
     *       Parameter["Longitude of natural origin", -129, Unit["degree"]],
     *       Parameter["Scale factor at natural origin", 0.9996],
     *       Parameter["False easting", 500000, Unit["metre"]]]]
     *   }
     */
    public static final DefaultConversion UTM;
    static {
        final OperationMethod method = new TransverseMercator();
        final ParameterValueGroup pg = method.getParameters().createValue();
        pg.parameter("Longitude of natural origin").setValue(-129);
        pg.parameter("Scale factor at natural origin").setValue(0.9996);
        pg.parameter("False easting").setValue(500000);
        UTM = create("UTM zone 9N", method, pg);
    }

    /**
     * A defining conversion for a <cite>Antarctic Polar Stereographic</cite> projection.
     */
    public static final DefaultConversion POLAR_STEREOGRAPHIC;
    static {
        final OperationMethod method = new PolarStereographicB();
        final ParameterValueGroup pg = method.getParameters().createValue();
        pg.parameter("Latitude of standard parallel").setValue(-71);
        POLAR_STEREOGRAPHIC = create("Antarctic Polar Stereographic", method, pg);
    }

    /**
     * A defining conversion for a <cite>Lambert Conic Conformal (1SP)</cite> projection
     * with a Latitude of natural origin arbitrarily set to 40.
     */
    public static final DefaultConversion LAMBERT;
    static {
        final OperationMethod method = new LambertConformal1SP();
        final ParameterValueGroup pg = method.getParameters().createValue();
        pg.parameter("Latitude of natural origin").setValue(40);
        LAMBERT = create("Lambert Conic Conformal", method, pg);
    }

    /**
     * Creates a defining conversion of the given name with given parameter values.
     */
    private static DefaultConversion create(final String name, final OperationMethod method, final ParameterValueGroup pg) {
        return new DefaultConversion(Map.of(OperationMethod.NAME_KEY, name), method, null, pg);
    }

    /**
     * Do not allow instantiation of this class.
     */
    private HardCodedConversions() {
    }

    /**
     * Creates a two-dimension CRS using the given conversion on the WGS84 datum.
     * This CRS uses (<var>easting</var>, <var>northing</var>) coordinates in metres.
     * The base CRS uses (<var>longitude</var>, <var>latitude</var>) axes
     * and the prime meridian is Greenwich.
     *
     * @param  conversion  the defining conversion as one of the constant defined in this class.
     * @return two-dimensional projection using the given method.
     */
    public static DefaultProjectedCRS createCRS(final DefaultConversion conversion) {
        return new DefaultProjectedCRS(Map.of(ProjectedCRS.NAME_KEY, conversion.getName()),
                HardCodedCRS.WGS84, conversion, HardCodedCS.PROJECTED);
    }

    /**
     * A two-dimensional Mercator projection using the WGS84 datum.
     * This CRS uses (<var>easting</var>, <var>northing</var>) coordinates in metres.
     * The base CRS uses (<var>longitude</var>, <var>latitude</var>) axes
     * and the prime meridian is Greenwich.
     *
     * <p>This CRS is equivalent to {@code EPSG:3395} except for base CRS axis order,
     * since EPSG puts latitude before longitude.</p>
     *
     * @return two-dimensional Mercator projection.
     */
    public static DefaultProjectedCRS mercator() {
        return createCRS(MERCATOR);
    }

    /**
     * A three-dimensional Mercator projection using the WGS84 datum.
     * This CRS uses (<var>easting</var>, <var>northing</var>, <var>height</var>) coordinates in metres.
     * The base CRS uses (<var>longitude</var>, <var>latitude</var>, <var>height</var>) axes
     * and the prime meridian is Greenwich.
     *
     * @return three-dimensional Mercator projection.
     */
    public static DefaultProjectedCRS mercator3D() {
        return new DefaultProjectedCRS(name("Mercator (3D)"),
                HardCodedCRS.WGS84_3D, MERCATOR, HardCodedCS.PROJECTED_3D);
    }

    /**
     * A two- or three-dimensional Mercator projection using the given base CRS.
     * This CRS uses (<var>easting</var>, <var>northing</var>) coordinates in metres.
     *
     * @param  baseCRS  the two- or three-dimensional base CRS.
     * @return two- or three-dimensional Mercator projection.
     */
    public static DefaultProjectedCRS mercator(final GeographicCRS baseCRS) {
        return new DefaultProjectedCRS(name("Mercator (other)"), baseCRS, MERCATOR,
                baseCRS.getCoordinateSystem().getDimension() == 3 ? HardCodedCS.PROJECTED_3D : HardCodedCS.PROJECTED);
    }

    /**
     * An arbitrary CRS using ESRI authority code.
     *
     * @return an arbitrary CRS using ESRI authority code.
     */
    public static DefaultProjectedCRS ESRI() {
        final OperationMethod method = new LambertConformal2SP();
        final ParameterValueGroup pg = method.getParameters().createValue();
        pg.parameter("Longitude of false origin")        .setValue( 3);
        pg.parameter("Latitude of false origin")         .setValue(46.5);
        pg.parameter("Latitude of 1st standard parallel").setValue(44);
        pg.parameter("Latitude of 2nd standard parallel").setValue(49);
        pg.parameter("Easting at false origin") .setValue( 700000);
        pg.parameter("Northing at false origin").setValue(6600000);
        final DefaultConversion c = create("Lambert Conic Conformal", method, pg);
        final ImmutableIdentifier id = new ImmutableIdentifier(Citations.ESRI, Constants.ESRI, "102110");
        return new DefaultProjectedCRS(
                Map.of(ProjectedCRS.NAME_KEY, "RGF 1993 Lambert",
                       ProjectedCRS.IDENTIFIERS_KEY, id),
                HardCodedCRS.GRS80, c, HardCodedCS.PROJECTED);
    }

    /**
     * Puts the given name in a property map CRS constructors.
     */
    private static Map<String,?> name(final String name) {
        return Map.of(ProjectedCRS.NAME_KEY, name);
    }
}
