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
package org.apache.sis.referencing.factory;

import java.util.Map;
import javax.measure.Unit;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import org.opengis.util.FactoryException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.CSFactory;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.datum.DatumFactory;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Conversion;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.MultiRegisterOperations;
import org.apache.sis.referencing.operation.DefaultConversion;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.measure.Units;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opengis.test.referencing.ObjectFactoryTest;
import org.apache.sis.test.FailureDetailsReporter;
import static org.apache.sis.referencing.Assertions.assertWktEquals;
import static org.apache.sis.test.Assertions.assertMessageContains;


/**
 * Tests {@link GeodeticObjectFactory} using the suite of tests provided in the GeoAPI project.
 * Note that this does not include authority factories tests or GIGS tests.
 *
 * @author  Cédric Briançon (Geomatys)
 */
@ExtendWith(FailureDetailsReporter.class)
public final class GeodeticObjectFactoryTest extends ObjectFactoryTest {
    /**
     * Creates a new test suite using the singleton factory instance.
     */
    public GeodeticObjectFactoryTest() {
        super(MultiRegisterOperations.provider());
    }

    /**
     * Tests {@link GeodeticObjectFactory#createFromWKT(String)}. We test only a very small WKT here because
     * it is not the purpose of this class to test the parser. The main purpose of this test is to verify
     * that {@link GeodeticObjectFactory} has been able to instantiate the parser.
     *
     * @throws FactoryException if the parsing failed.
     */
    @Test
    public void testCreateFromWKT() throws FactoryException {
        final CRSFactory crsFactory = factories.getFactory(CRSFactory.class).orElseThrow();
        final GeodeticCRS crs = (GeodeticCRS) crsFactory.createFromWKT(
                "GEOGCS[“WGS 84”,\n" +
                "  DATUM[“World Geodetic System 1984”,\n" +
                "    SPHEROID[“WGS84”, 6378137.0, 298.257223563]],\n" +
                "  PRIMEM[“Greenwich”, 0.0],\n" +
                "  UNIT[“degree”, 0.017453292519943295]]");

        assertEquals("WGS 84", crs.getName().getCode());
        assertEquals("World Geodetic System 1984", crs.getDatum().getName().getCode());
    }

    /**
     * Tests {@link GeodeticObjectFactory#createFromWKT(String)} with an erroneous projection parameter name.
     * The intent is to verify that the expected exception is thrown.
     *
     * @throws FactoryException if the parsing failed for another reason than the expected one.
     */
    @Test
    public void testInvalidParameterInWKT() throws FactoryException {
        final CRSFactory crsFactory = factories.getFactory(CRSFactory.class).orElseThrow();
        var e = assertThrows(InvalidGeodeticParameterException.class,
                () -> crsFactory.createFromWKT(
                "PROJCRS[“Custom”,\n" +
                "  BASEGEODCRS[“North American 1983”,\n" +
                "    DATUM[“North American 1983”,\n" +
                "      ELLIPSOID[“GRS 1980”, 6378137, 298.257222101]]],\n" +
                "  CONVERSION[“Custom”,\n" +
                "    METHOD[“Lambert Conformal Conic”],\n" +
                "    PARAMETER[“Standard parallel 1”, 43.0],\n" +
                "    PARAMETER[“Standard parallel 2”, 45.5],\n" +
                "    PARAMETER[“Central parallel”, 41.75]],\n" +       // Wrong parameter.
                "  CS[Cartesian, 2],\n" +
                "    AXIS[“(Y)”, north],\n" +
                "    AXIS[“(X)”, east],\n" +
                "  UNIT[“metre”, 1]]"),
                "Should not have parsed a WKT with wrong projection parameter.");
        assertMessageContains(e, "Central parallel");
    }

    /**
     * Convenience method creating a map with only the "{@code name"} property.
     * This is the only mandatory property for object creation.
     */
    private static Map<String,?> name(final String name) {
        return Map.of(IdentifiedObject.NAME_KEY, name);
    }

    /**
     * Tests step-by-step the creation of a new projected coordinate reference systems.
     * This test creates every objects itself and compares with expected WKT 1 after each step.
     *
     * <p>Note that practical applications may use existing constants declared in the
     * {@link CommonCRS} class instead of creating everything like this test does.</p>
     *
     * @throws FactoryException if the creation of a geodetic component failed.
     */
    @Test
    public void testStepByStepCreation() throws FactoryException {
        final CRSFactory   crsFactory   = factories.getFactory(CRSFactory.class).orElseThrow();
        final CSFactory    csFactory    = factories.getFactory(CSFactory.class).orElseThrow();
        final DatumFactory datumFactory = factories.getFactory(DatumFactory.class).orElseThrow();
        /*
         * List of all objects to be created in this test.
         */
        final Unit<Length>         linearUnit;
        final Unit<Angle>          angularUnit;
        final Ellipsoid            ellipsoid;
        final PrimeMeridian        meridian;
        final GeodeticDatum        datum;
        final CoordinateSystemAxis longitude, latitude, easting, northing;
        final EllipsoidalCS        geographicCS;
        final GeographicCRS        geographicCRS;
        final OperationMethod      method;
        final ParameterValueGroup  parameters;
        final Conversion           projection;
        final CartesianCS          projectedCS;
        final ProjectedCRS         projectedCRS;
        /*
         * Prime meridian
         */
        angularUnit = Units.DEGREE;
        meridian = datumFactory.createPrimeMeridian(name("Greenwich"), 0, angularUnit);
        assertWktEquals(Convention.WKT1,
                "PRIMEM[“Greenwich”, 0.0]", meridian);
        /*
         * Ellipsoid
         */
        linearUnit = Units.METRE;
        ellipsoid = datumFactory.createEllipsoid(name("Airy1830"), 6377563.396, 6356256.910, linearUnit);
        assertWktEquals(Convention.WKT1,
                "SPHEROID[“Airy1830”, 6377563.396, 299.3249753150345]", ellipsoid);
        /*
         * Geodetic reference frame
         */
        datum = datumFactory.createGeodeticDatum(name("Airy1830"), ellipsoid, meridian);
        assertWktEquals(Convention.WKT1,
                "DATUM[“Airy1830”,\n" +
                "  SPHEROID[“Airy1830”, 6377563.396, 299.3249753150345]]", datum);
        /*
         * Base coordinate reference system
         */
        longitude     =  csFactory.createCoordinateSystemAxis(name("Longitude"), "long", AxisDirection.EAST,  angularUnit);
        latitude      =  csFactory.createCoordinateSystemAxis(name("Latitude"),  "lat",  AxisDirection.NORTH, angularUnit);
        geographicCS  =  csFactory.createEllipsoidalCS(name("Ellipsoidal"), longitude, latitude);
        geographicCRS = crsFactory.createGeographicCRS(name("Airy1830"), datum, geographicCS);
        assertWktEquals(Convention.WKT1,
                "GEOGCS[“Airy1830”,\n" +
                "  DATUM[“Airy1830”,\n" +
                "    SPHEROID[“Airy1830”, 6377563.396, 299.3249753150345]],\n" +
                "    PRIMEM[“Greenwich”, 0.0],\n" +
                "  UNIT[“degree”, 0.017453292519943295],\n" +
                "  AXIS[“Longitude”, EAST],\n" +
                "  AXIS[“Latitude”, NORTH]]", geographicCRS);
        /*
         * Defining conversion
         */
        method = DefaultMathTransformFactory.provider().getOperationMethod("Transverse_Mercator");
        parameters = method.getParameters().createValue();
        parameters.parameter("semi_major")        .setValue(ellipsoid.getSemiMajorAxis());
        parameters.parameter("semi_minor")        .setValue(ellipsoid.getSemiMinorAxis());
        parameters.parameter("central_meridian")  .setValue(     49);
        parameters.parameter("latitude_of_origin").setValue(     -2);
        parameters.parameter("false_easting")     .setValue( 400000);
        parameters.parameter("false_northing")    .setValue(-100000);
        projection = new DefaultConversion(name("GBN grid"), method, null, parameters);
        /*
         * Projected coordinate reference system
         */
        easting      =  csFactory.createCoordinateSystemAxis(name("Easting"),  "x", AxisDirection.EAST,  linearUnit);
        northing     =  csFactory.createCoordinateSystemAxis(name("Northing"), "y", AxisDirection.NORTH, linearUnit);
        projectedCS  =  csFactory.createCartesianCS(name("Cartesian"), easting, northing);
        projectedCRS = crsFactory.createProjectedCRS(name("Great_Britian_National_Grid"), geographicCRS, projection, projectedCS);
        assertWktEquals(Convention.WKT1,
                "PROJCS[“Great_Britian_National_Grid”,\n" +
                "  GEOGCS[“Airy1830”,\n" +
                "    DATUM[“Airy1830”,\n" +
                "      SPHEROID[“Airy1830”, 6377563.396, 299.3249753150345]],\n" +
                "      PRIMEM[“Greenwich”, 0.0],\n" +
                "    UNIT[“degree”, 0.017453292519943295],\n" +
                "    AXIS[“Longitude”, EAST],\n" +
                "    AXIS[“Latitude”, NORTH]],\n" +
                "  PROJECTION[“Transverse_Mercator”, AUTHORITY[“EPSG”, “9807”]],\n" +
                "  PARAMETER[“latitude_of_origin”, -2.0],\n" +
                "  PARAMETER[“central_meridian”, 49.0],\n" +
                "  PARAMETER[“scale_factor”, 1.0],\n" +
                "  PARAMETER[“false_easting”, 400000.0],\n" +
                "  PARAMETER[“false_northing”, -100000.0],\n" +
                "  UNIT[“metre”, 1],\n" +
                "  AXIS[“Easting”, EAST],\n" +
                "  AXIS[“Northing”, NORTH]]", projectedCRS);
    }
}
