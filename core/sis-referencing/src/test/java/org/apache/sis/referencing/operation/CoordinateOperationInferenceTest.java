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

import java.text.ParseException;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.Projection;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.io.wkt.WKTFormat;

import static org.apache.sis.internal.referencing.Formulas.LINEAR_TOLERANCE;
import static org.apache.sis.internal.referencing.Formulas.ANGULAR_TOLERANCE;

// Test dependencies
import org.apache.sis.referencing.operation.transform.MathTransformTestCase;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Tests {@link CoordinateOperationInference}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn({
    DefaultConversionTest.class,
    DefaultTransformationTest.class,
    DefaultPassThroughOperationTest.class,
    DefaultConcatenatedOperationTest.class
})
public final strictfp class CoordinateOperationInferenceTest extends MathTransformTestCase {
    /**
     * Tolerance threshold for strict comparisons of floating point numbers.
     * This constant can be used like below, where {@code expected} and {@code actual} are {@code double} values:
     *
     * {@preformat java
     *     assertEquals(expected, actual, STRICT);
     * }
     */
    private static final double STRICT = 0;

    /**
     * The transformation factory to use for testing.
     */
    private static DefaultCoordinateOperationFactory factory;

    /**
     * The parser to use for WKT strings used in this test.
     */
    private static WKTFormat parser;

    /**
     * Creates a new {@link DefaultCoordinateOperationFactory} to use for testing purpose.
     * The same factory will be used for all tests in this class.
     *
     * @throws ParseException if an error occurred while preparing the WKT parser.
     */
    @BeforeClass
    public static void createFactory() throws ParseException {
        factory = new DefaultCoordinateOperationFactory();
        parser  = new WKTFormat(null, null);
        /*
         * The fist keyword in WKT below should be "GeodeticCRS" in WKT 2, but we use the WKT 1 keyword ("GEOGCS")
         * for allowing inclusion in ProjectedCRS.  SIS is okay with mixed WKT versions, but this is of course not
         * something to recommend in production.
         */
        parser.addFragment("Sphere",
                "GEOGCS[“Sphere”,\n" +
                "  Datum[“Sphere”, Ellipsoid[“Sphere”, 6370997, 0]],\n" +
                "  CS[ellipsoidal, 2],\n" +
                "  Axis[“Longitude (λ)”, EAST],\n" +
                "  Axis[“Latitude (φ)”, NORTH],\n" +
                "  Unit[“degree”, 0.017453292519943295]]");
        /*
         * NAD27 (EPSG:4267), defined in WKT instead than relying on the CommonCRS.NAD27 constant in order to fix
         * the TOWGS84[…] parameter to values that we control. Note that TOWGS84[…] is not a legal WKT 2 element.
         * We could mix WKT 1 and WKT 2 elements (SIS allows that), but we nevertheless use WKT 1 for the whole
         * string as a matter of principle.
         */
        parser.addFragment("NAD27",
                "GEOGCS[“Sphere”,\n" +
                "  DATUM[“North American Datum 1927”,\n" +
                "    SPHEROID[“Clarke 1866”, 6378206.4, 294.9786982138982],\n" +
                "    TOWGS84[-10, 158, 187]]," +
                "    PRIMEM[“Greenwich”, 0.0]," +
                "  UNIT[“degree”, 0.017453292519943295],\n" +
                "  AXIS[“Latitude (φ)”, NORTH],\n" +
                "  AXIS[“Longitude (λ)”, EAST],\n" +
                "  AUTHORITY[“EPSG”, “4267”]]");
    }

    /**
     * Disposes the factory created by {@link #createFactory()} after all tests have been executed.
     */
    @AfterClass
    public static void disposeFactory() {
        factory = null;
        parser  = null;
    }

    /**
     * Returns the CRS for the given Well Known Text.
     */
    private static CoordinateReferenceSystem parse(final String wkt) throws ParseException {
        return (CoordinateReferenceSystem) parser.parseObject(wkt);
    }

    /**
     * Makes sure that {@code createOperation(sourceCRS, targetCRS)} returns an identity transform
     * when {@code sourceCRS} and {@code targetCRS} are identical.
     *
     * @throws FactoryException if the operation can not be created.
     */
    @Test
    public void testIdentityTransform() throws FactoryException {
        testIdentityTransform(CommonCRS.WGS84.geographic());
        testIdentityTransform(CommonCRS.WGS84.geographic3D());
        testIdentityTransform(CommonCRS.WGS84.geocentric());
        testIdentityTransform(CommonCRS.WGS84.spherical());
        testIdentityTransform(CommonCRS.WGS84.UTM(0, 0));
    }

    /**
     * Implementation of {@link #testIdentityTransform()} using the given CRS.
     */
    private static void testIdentityTransform(final CoordinateReferenceSystem crs) throws FactoryException {
        final CoordinateOperation operation = factory.createOperation(crs, crs);
        assertSame("sourceCRS",  crs, operation.getSourceCRS());
        assertSame("targetCRS",  crs, operation.getTargetCRS());
        assertTrue("isIdentity", operation.getMathTransform().isIdentity());
        assertInstanceOf("operation", Conversion.class, operation);
    }

    /**
     * Tests a transformation that requires a datum shift.
     *
     * @throws ParseException if a CRS used in this test can not be parsed.
     * @throws FactoryException if the operation can not be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    @DependsOnMethod("testIdentityTransform")
    public void testDatumShift() throws ParseException, FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCRS = parse("$NAD27");
        final CoordinateReferenceSystem targetCRS = CommonCRS.WGS84.geographic();
        final CoordinateOperation operation = factory.createOperation(sourceCRS, targetCRS);
        assertSame(sourceCRS, operation.getSourceCRS());
        assertSame(targetCRS, operation.getTargetCRS());
        transform  = operation.getMathTransform();
        tolerance  = ANGULAR_TOLERANCE;
        λDimension = new int[] {1};
        verifyTransform(new double[] {
            39,          -85,
            38.26,       -80.58
        }, new double[] {
            39.00011150, -84.99995603,
            38.26011883, -80.57981725
        });
        validate();
    }

    /**
     * Tests conversion from a geographic to a projected CRS without datum of axis changes.
     *
     * @throws ParseException if a CRS used in this test can not be parsed.
     * @throws FactoryException if the operation can not be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    @DependsOnMethod("testIdentityTransform")
    public void testGeographicToProjected() throws ParseException, FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCRS = parse("$Sphere");
        final CoordinateReferenceSystem targetCRS = parse(
                "ProjectedCRS[“TM”,\n" +
                "  $Sphere,\n" +
                "  Conversion[“TM”,\n" +
                "    Method[“Transverse Mercator”],\n" +
                "    Parameter[“Longitude of natural origin”, 170],\n" +
                "    Parameter[“Latitude of natural origin”, 50],\n" +
                "    Parameter[“Scale factor at natural origin”, 0.95]],\n" +
                "  CS[Cartesian, 2],\n" +
                "  Axis[“x”, EAST],\n" +
                "  Axis[“y”, NORTH],\n" +
                "  Unit[“US survey foot”, 0.304800609601219]]");

        final CoordinateOperation operation = factory.createOperation(sourceCRS, targetCRS);
        assertSame("sourceCRS", sourceCRS, operation.getSourceCRS());
        assertSame("targetCRS", targetCRS, operation.getTargetCRS());
        assertInstanceOf("operation", Projection.class, operation);

        final ParameterValueGroup param = ((SingleOperation) operation).getParameterValues();
        assertEquals("semi_major",     6370997, param.parameter("semi_major"        ).doubleValue(), STRICT);
        assertEquals("semi_minor",     6370997, param.parameter("semi_minor"        ).doubleValue(), STRICT);
        assertEquals("latitude_of_origin",  50, param.parameter("latitude_of_origin").doubleValue(), STRICT);
        assertEquals("central_meridian",   170, param.parameter("central_meridian"  ).doubleValue(), STRICT);
        assertEquals("scale_factor",      0.95, param.parameter("scale_factor"      ).doubleValue(), STRICT);
        assertEquals("false_easting",        0, param.parameter("false_easting"     ).doubleValue(), STRICT);
        assertEquals("false_northing",       0, param.parameter("false_northing"    ).doubleValue(), STRICT);

        transform = operation.getMathTransform();
        tolerance = ANGULAR_TOLERANCE;
        verifyTransform(new double[] {170, 50}, new double[] {0, 0});
        validate();

        transform  = transform.inverse();
        tolerance  = LINEAR_TOLERANCE;
        λDimension = new int[] {0};
        verifyTransform(new double[] {0, 0}, new double[] {170, 50});
        validate();
    }
}
