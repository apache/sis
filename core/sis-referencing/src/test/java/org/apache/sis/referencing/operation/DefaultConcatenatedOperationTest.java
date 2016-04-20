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

import java.util.Collections;
import javax.xml.bind.JAXBException;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.transform.EllipsoidToCentricTransform;
import org.apache.sis.referencing.datum.HardCodedDatum;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.io.wkt.Convention;

import org.opengis.test.Validators;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests the {@link DefaultConcatenatedOperation} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn({
    DefaultTransformationTest.class,
    SingleOperationMarshallingTest.class
})
public final strictfp class DefaultConcatenatedOperationTest extends XMLTestCase {
    /**
     * An XML file in this package containing a projected CRS definition.
     */
    private static final String XML_FILE = "ConcatenatedOperation.xml";

    /**
     * Creates a “Tokyo to JGD2000” transformation.
     *
     * @see DefaultTransformationTest#createGeocentricTranslation()
     */
    private static DefaultConcatenatedOperation createGeocentricTranslation() throws FactoryException, NoninvertibleTransformException {
        final MathTransformFactory mtFactory = DefaultFactories.forBuildin(MathTransformFactory.class);
        final DefaultTransformation op = DefaultTransformationTest.createGeocentricTranslation();

        final DefaultConversion before = new DefaultConversion(
                Collections.singletonMap(DefaultConversion.NAME_KEY, "Geographic to geocentric"),
                HardCodedCRS.TOKYO,             // SourceCRS
                op.getSourceCRS(),              // TargetCRS
                null,                           // InterpolationCRS
                DefaultOperationMethodTest.create("Geographic/geocentric conversions", "9602", "EPSG guidance note #7-2", 3),
                EllipsoidToCentricTransform.createGeodeticConversion(mtFactory, HardCodedDatum.TOKYO.getEllipsoid(), true));

        final DefaultConversion after = new DefaultConversion(
                Collections.singletonMap(DefaultConversion.NAME_KEY, "Geocentric to geographic"),
                op.getTargetCRS(),              // SourceCRS
                HardCodedCRS.JGD2000,           // TargetCRS
                null,                           // InterpolationCRS
                DefaultOperationMethodTest.create("Geographic/geocentric conversions", "9602", "EPSG guidance note #7-2", 3),
                EllipsoidToCentricTransform.createGeodeticConversion(mtFactory, HardCodedDatum.JGD2000.getEllipsoid(), true).inverse());

        return new DefaultConcatenatedOperation(
                Collections.singletonMap(DefaultConversion.NAME_KEY, "Tokyo to JGD2000"),
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
        assertWktEquals(Convention.WKT2_SIMPLIFIED,                             // Pseudo-WKT actually.
                "ConcatenatedOperation[“Tokyo to JGD2000”,\n" +
                "  SourceCRS[GeodeticCRS[“Tokyo”,\n" +
                "    Datum[“Tokyo 1918”,\n" +
                "      Ellipsoid[“Bessel 1841”, 6377397.155, 299.1528128]],\n" +
                "    CS[ellipsoidal, 3],\n" +
                "      Axis[“Longitude (L)”, east, Unit[“degree”, 0.017453292519943295]],\n" +
                "      Axis[“Latitude (B)”, north, Unit[“degree”, 0.017453292519943295]],\n" +
                "      Axis[“Ellipsoidal height (h)”, up, Unit[“metre”, 1]]]],\n" +
                "  TargetCRS[GeodeticCRS[“JGD2000”,\n" +
                "    Datum[“Japanese Geodetic Datum 2000”,\n" +
                "      Ellipsoid[“GRS 1980”, 6378137.0, 298.257222101]],\n" +
                "    CS[ellipsoidal, 3],\n" +
                "      Axis[“Longitude (L)”, east, Unit[“degree”, 0.017453292519943295]],\n" +
                "      Axis[“Latitude (B)”, north, Unit[“degree”, 0.017453292519943295]],\n" +
                "      Axis[“Ellipsoidal height (h)”, up, Unit[“metre”, 1]]]],\n" +
                "  CoordinateOperationStep[“Geographic to geocentric”,\n" +
                "    Method[“Geographic/geocentric conversions”],\n" +
                "      Parameter[“semi_major”, 6377397.155, Unit[“metre”, 1]],\n" +
                "      Parameter[“semi_minor”, 6356078.962818189, Unit[“metre”, 1]]],\n" +
                "  CoordinateOperationStep[“Tokyo to JGD2000 (GSI)”,\n" +
                "    Method[“Geocentric translations”],\n" +
                "      Parameter[“X-axis translation”, -146.414],\n" +
                "      Parameter[“Y-axis translation”, 507.337],\n" +
                "      Parameter[“Z-axis translation”, 680.507]],\n" +
                "  CoordinateOperationStep[“Geocentric to geographic”,\n" +
                "    Method[“Geographic/geocentric conversions”],\n" +
                "      Parameter[“semi_major”, 6378137.0, Unit[“metre”, 1]],\n" +
                "      Parameter[“semi_minor”, 6356752.314140356, Unit[“metre”, 1]]]]", op);
    }

    /**
     * Tests (un)marshalling of a concatenated operation.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testXML() throws JAXBException {
        final DefaultConcatenatedOperation op = unmarshalFile(DefaultConcatenatedOperation.class, XML_FILE);
        Validators.validate(op);
        assertEquals("operations.size()", 2, op.getOperations().size());
        final CoordinateOperation step1 = op.getOperations().get(0);
        final CoordinateOperation step2 = op.getOperations().get(1);
        final CoordinateReferenceSystem sourceCRS = op.getSourceCRS();
        final CoordinateReferenceSystem targetCRS = op.getTargetCRS();

        assertIdentifierEquals(          "identifier", "test", "test", null, "concatenated", getSingleton(op       .getIdentifiers()));
        assertIdentifierEquals("sourceCRS.identifier", "test", "test", null, "source",       getSingleton(sourceCRS.getIdentifiers()));
        assertIdentifierEquals("targetCRS.identifier", "test", "test", null, "target",       getSingleton(targetCRS.getIdentifiers()));
        assertIdentifierEquals(    "step1.identifier", "test", "test", null, "step-1",       getSingleton(step1    .getIdentifiers()));
        assertIdentifierEquals(    "step2.identifier", "test", "test", null, "step-2",       getSingleton(step2    .getIdentifiers()));
        assertInstanceOf("sourceCRS", GeodeticCRS.class, sourceCRS);
        assertInstanceOf("targetCRS", GeodeticCRS.class, targetCRS);
        assertSame("sourceCRS", step1.getSourceCRS(), sourceCRS);
        assertSame("targetCRS", step2.getTargetCRS(), targetCRS);
        assertSame("tmp CRS",   step1.getTargetCRS(), step2.getSourceCRS());
        /*
         * Test marshalling and compare with the original file.
         */
        assertMarshalEqualsFile(XML_FILE, op, "xmlns:*", "xsi:schemaLocation");
    }
}
