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
import java.io.InputStream;
import jakarta.xml.bind.JAXBException;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.referencing.operation.transform.EllipsoidToCentricTransform;
import org.apache.sis.io.wkt.Convention;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.opengis.test.Validators;
import org.apache.sis.xml.test.TestCase;
import org.apache.sis.referencing.datum.HardCodedDatum;
import org.apache.sis.referencing.crs.HardCodedCRS;
import static org.apache.sis.referencing.Assertions.assertWktEquals;
import static org.apache.sis.test.TestUtilities.getSingleton;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import static org.opengis.test.Assertions.assertIdentifierEquals;


/**
 * Tests the {@link DefaultConcatenatedOperation} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DefaultConcatenatedOperationTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultConcatenatedOperationTest() {
    }

    /**
     * Opens the stream to the XML file in this package containing a projected CRS definition.
     *
     * @return stream opened on the XML document to use for testing purpose.
     */
    private static InputStream openTestFile() {
        // Call to `getResourceAsStream(…)` is caller sensitive: it must be in the same module.
        return DefaultConcatenatedOperationTest.class.getResourceAsStream("ConcatenatedOperation.xml");
    }

    /**
     * Creates a “Tokyo to JGD2000” transformation.
     * This is defined by {@code EPSG:15483}, but this test does not reproduce all metadata.
     *
     * @see DefaultTransformationTest#createGeocentricTranslation()
     */
    private static DefaultConcatenatedOperation createGeocentricTranslation() throws FactoryException, NoninvertibleTransformException {
        final MathTransformFactory mtFactory = DefaultMathTransformFactory.provider();
        final DefaultTransformation op = DefaultTransformationTest.createGeocentricTranslation();

        final var before = new DefaultConversion(
                Map.of(DefaultConversion.NAME_KEY, "Geographic to geocentric"),
                HardCodedCRS.TOKYO,             // SourceCRS
                op.getSourceCRS(),              // TargetCRS
                null,                           // InterpolationCRS
                DefaultOperationMethodTest.create("Geographic/geocentric conversions", "9602", "EPSG guidance note #7-2"),
                EllipsoidToCentricTransform.createGeodeticConversion(mtFactory, HardCodedDatum.TOKYO.getEllipsoid(), true));

        final var after = new DefaultConversion(
                Map.of(DefaultConversion.NAME_KEY, "Geocentric to geographic"),
                op.getTargetCRS(),              // SourceCRS
                HardCodedCRS.JGD2000,           // TargetCRS
                null,                           // InterpolationCRS
                DefaultOperationMethodTest.create("Geographic/geocentric conversions", "9602", "EPSG guidance note #7-2"),
                EllipsoidToCentricTransform.createGeodeticConversion(mtFactory, HardCodedDatum.JGD2000.getEllipsoid(), true).inverse());

        return new DefaultConcatenatedOperation(
                Map.of(DefaultConversion.NAME_KEY, "Tokyo to JGD2000"),
                null, null,
                new AbstractSingleOperation[] {before, op, after}, mtFactory);
    }

    /**
     * Tests WKT formatting. The WKT format used here is not defined in OGC/ISO standards;
     * this is a SIS-specific extension.
     *
     * @throws FactoryException if an error occurred while creating the test operation.
     * @throws NoninvertibleTransformException if an error occurred while creating the test operation.
     */
    @Test
    public void testWKT() throws FactoryException, NoninvertibleTransformException {
        final DefaultConcatenatedOperation op = createGeocentricTranslation();
        assertWktEquals(Convention.WKT2_SIMPLIFIED,
                "ConcatenatedOperation[“Tokyo to JGD2000”,\n" +
                "  SourceCRS[GeographicCRS[“Tokyo”,\n" +
                "    Datum[“Tokyo 1918”,\n" +
                "      Ellipsoid[“Bessel 1841”, 6377397.155, 299.1528128]],\n" +
                "    CS[ellipsoidal, 3],\n" +
                "      Axis[“Longitude (L)”, east, Unit[“degree”, 0.017453292519943295]],\n" +
                "      Axis[“Latitude (B)”, north, Unit[“degree”, 0.017453292519943295]],\n" +
                "      Axis[“Ellipsoidal height (h)”, up, Unit[“metre”, 1]]]],\n" +
                "  TargetCRS[GeographicCRS[“JGD2000”,\n" +
                "    Datum[“Japanese Geodetic Datum 2000”,\n" +
                "      Ellipsoid[“GRS 1980”, 6378137.0, 298.257222101]],\n" +
                "    CS[ellipsoidal, 3],\n" +
                "      Axis[“Longitude (L)”, east, Unit[“degree”, 0.017453292519943295]],\n" +
                "      Axis[“Latitude (B)”, north, Unit[“degree”, 0.017453292519943295]],\n" +
                "      Axis[“Ellipsoidal height (h)”, up, Unit[“metre”, 1]]]],\n" +
                "  Step[\n" +
                "    CoordinateOperation[“Geographic to geocentric”,\n" +
                "      SourceCRS[GeographicCRS[“Tokyo”,\n" +
                "        Datum[“Tokyo 1918”,\n" +
                "          Ellipsoid[“Bessel 1841”, 6377397.155, 299.1528128]],\n" +
                "        CS[ellipsoidal, 3],\n" +
                "          Axis[“Longitude (L)”, east, Unit[“degree”, 0.017453292519943295]],\n" +
                "          Axis[“Latitude (B)”, north, Unit[“degree”, 0.017453292519943295]],\n" +
                "          Axis[“Ellipsoidal height (h)”, up, Unit[“metre”, 1]]]],\n" +
                "      TargetCRS[GeodeticCRS[“Tokyo 1918”,\n" +
                "        Datum[“Tokyo 1918”,\n" +
                "          Ellipsoid[“Bessel 1841”, 6377397.155, 299.1528128]],\n" +
                "        CS[Cartesian, 3],\n" +
                "          Axis[“(X)”, geocentricX],\n" +
                "          Axis[“(Y)”, geocentricY],\n" +
                "          Axis[“(Z)”, geocentricZ],\n" +
                "          Unit[“metre”, 1]]],\n" +
                "      Method[“Geographic/geocentric conversions”]]],\n" +  // Omit non-EPSG parameters for EPSG method.
                "  Step[\n" +
                "    CoordinateOperation[“Tokyo to JGD2000 (GSI)”, Version[“GSI-Jpn”],\n" +
                "      SourceCRS[GeodeticCRS[“Tokyo 1918”,\n" +
                "        Datum[“Tokyo 1918”,\n" +
                "          Ellipsoid[“Bessel 1841”, 6377397.155, 299.1528128]],\n" +
                "        CS[Cartesian, 3],\n" +
                "          Axis[“(X)”, geocentricX],\n" +
                "          Axis[“(Y)”, geocentricY],\n" +
                "          Axis[“(Z)”, geocentricZ],\n" +
                "          Unit[“metre”, 1]]],\n" +
                "      TargetCRS[GeodeticCRS[“JGD2000”,\n" +
                "        Datum[“Japanese Geodetic Datum 2000”,\n" +
                "          Ellipsoid[“GRS 1980”, 6378137.0, 298.257222101]],\n" +
                "        CS[Cartesian, 3],\n" +
                "          Axis[“(X)”, geocentricX],\n" +
                "          Axis[“(Y)”, geocentricY],\n" +
                "          Axis[“(Z)”, geocentricZ],\n" +
                "          Unit[“metre”, 1]]],\n" +
                "      Method[“Geocentric translations”],\n" +
                "        Parameter[“X-axis translation”, -146.414],\n" +
                "        Parameter[“Y-axis translation”, 507.337],\n" +
                "        Parameter[“Z-axis translation”, 680.507]]],\n" +
                "  Step[\n" +
                "    CoordinateOperation[“Geocentric to geographic”,\n" +
                "      SourceCRS[GeodeticCRS[“JGD2000”,\n" +
                "        Datum[“Japanese Geodetic Datum 2000”,\n" +
                "          Ellipsoid[“GRS 1980”, 6378137.0, 298.257222101]],\n" +
                "        CS[Cartesian, 3],\n" +
                "          Axis[“(X)”, geocentricX],\n" +
                "          Axis[“(Y)”, geocentricY],\n" +
                "          Axis[“(Z)”, geocentricZ],\n" +
                "          Unit[“metre”, 1]]],\n" +
                "      TargetCRS[GeographicCRS[“JGD2000”,\n" +
                "        Datum[“Japanese Geodetic Datum 2000”,\n" +
                "          Ellipsoid[“GRS 1980”, 6378137.0, 298.257222101]],\n" +
                "        CS[ellipsoidal, 3],\n" +
                "          Axis[“Longitude (L)”, east, Unit[“degree”, 0.017453292519943295]],\n" +
                "          Axis[“Latitude (B)”, north, Unit[“degree”, 0.017453292519943295]],\n" +
                "          Axis[“Ellipsoidal height (h)”, up, Unit[“metre”, 1]]]],\n" +
                "      Method[“Geographic/geocentric conversions”],\n" +
                "        Parameter[“semi_major”, 6378137.0, Unit[“metre”, 1]],\n" +
                "        Parameter[“semi_minor”, 6356752.314140356, Unit[“metre”, 1]]]]]", op);
    }

    /**
     * Tests (un)marshalling of a concatenated operation.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testXML() throws JAXBException {
        final DefaultConcatenatedOperation op = unmarshalFile(DefaultConcatenatedOperation.class, openTestFile());
        Validators.validate(op);
        assertEquals(2, op.getOperations().size());
        final CoordinateOperation step1 = op.getOperations().get(0);
        final CoordinateOperation step2 = op.getOperations().get(1);
        final CoordinateReferenceSystem sourceCRS = op.getSourceCRS();
        final CoordinateReferenceSystem targetCRS = op.getTargetCRS();

        assertIdentifierEquals("test", "test", null, "concatenated", getSingleton(op       .getIdentifiers()),           "identifier");
        assertIdentifierEquals("test", "test", null, "source",       getSingleton(sourceCRS.getIdentifiers()), "sourceCRS.identifier");
        assertIdentifierEquals("test", "test", null, "target",       getSingleton(targetCRS.getIdentifiers()), "targetCRS.identifier");
        assertIdentifierEquals("test", "test", null, "step-1",       getSingleton(step1    .getIdentifiers()),     "step1.identifier");
        assertIdentifierEquals("test", "test", null, "step-2",       getSingleton(step2    .getIdentifiers()),     "step2.identifier");
        assertInstanceOf(GeodeticCRS.class, sourceCRS);
        assertInstanceOf(GeodeticCRS.class, targetCRS);
        assertSame(step1.getSourceCRS(), sourceCRS);
        assertSame(step2.getTargetCRS(), targetCRS);
        assertSame(step1.getTargetCRS(), step2.getSourceCRS());
        /*
         * Test marshalling and compare with the original file.
         */
        assertMarshalEqualsFile(openTestFile(), op, "xmlns:*", "xsi:schemaLocation");
    }
}
