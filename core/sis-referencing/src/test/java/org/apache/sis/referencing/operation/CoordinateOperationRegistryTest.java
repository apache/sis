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
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.CRS;
import org.apache.sis.io.wkt.WKTFormat;

// Test dependencies
import org.apache.sis.referencing.operation.transform.MathTransformTestCase;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import static org.apache.sis.test.ReferencingAssert.*;
import static org.junit.Assume.*;


/**
 * Tests {@link CoordinateOperationRegistry}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final strictfp class CoordinateOperationRegistryTest extends MathTransformTestCase {
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
     * The instance on which to execute the tests.
     */
    private final CoordinateOperationRegistry registry;

    /**
     * Creates a new test case.
     *
     * @throws FactoryException if an error occurred while creating the factory to be tested.
     */
    public CoordinateOperationRegistryTest() throws FactoryException {
        final CRSAuthorityFactory crsFactory = CRS.getAuthorityFactory("EPSG");
        assumeTrue("EPSG factory required.", crsFactory instanceof CoordinateOperationAuthorityFactory);
        registry = new CoordinateOperationRegistry(factory, (CoordinateOperationAuthorityFactory) crsFactory, null);
    }

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
     * Tests a datum shift between two geographic CRS which also implies a change of prime meridian.
     * The expected coordinate operation is EPSG:8094.
     *
     * @throws ParseException if a CRS used in this test can not be parsed.
     * @throws FactoryException if the operation can not be created.
     * @throws TransformException if an error occurred while converting the test points.
     */
    @Test
    public void testDatumShiftWithLongitudeRotation() throws ParseException, FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCRS = parse(
                "GeodeticCRS[“NTF (Paris)”,\n" +
                "  Datum[“Nouvelle Triangulation Française (Paris)”,\n" +
                "    Ellipsoid[“Clarke 1880 (IGN)”, 6378249.2, 293.4660212936269]],\n" +
                "    PrimeMeridian[“Paris”, 2.5969213],\n" +
                "  CS[ellipsoidal, 2],\n" +
                "    Axis[“Latitude (φ)”, NORTH],\n" +
                "    Axis[“Longitude (λ)”, EAST],\n" +
                "    Unit[“grade”, 0.015707963267948967]\n," +
                "  Id[“EPSG”, 4807]]");

        final CoordinateReferenceSystem targetCRS = CommonCRS.WGS84.geographic();
        final CoordinateOperation operation = registry.createOperation(sourceCRS, targetCRS);
        assertEpsgNameAndIdentifierEqual("NTF (Paris) to WGS 84 (1)", 8094, operation);
        assertEquals("linearAccuracy", 2, CRS.getLinearAccuracy(operation), STRICT);

        transform = operation.getMathTransform();
        assertFalse(transform.isIdentity());
        validate();
    }
}
