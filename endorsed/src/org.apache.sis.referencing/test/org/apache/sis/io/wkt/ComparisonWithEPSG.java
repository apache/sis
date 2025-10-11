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
package org.apache.sis.io.wkt;

import java.text.ParseException;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.factory.sql.EPSGFactory;

// Test dependencies
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.factory.TestFactorySource;
import static org.apache.sis.test.Assertions.assertEqualsIgnoreMetadata;


/**
 * Compares the result of some WKT parsing with the expected result from EPSG database.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
@Execution(ExecutionMode.CONCURRENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class ComparisonWithEPSG extends TestCase {
    /**
     * The source of the EPSG factory.
     */
    private final TestFactorySource dataEPSG;

    /**
     * Creates the factory to use for all tests in this class.
     *
     * @throws FactoryException if an error occurred while creating the factory.
     */
    public ComparisonWithEPSG() throws FactoryException {
        dataEPSG = new TestFactorySource();
    }

    /**
     * Forces release of JDBC connections after the tests in this class.
     *
     * @throws FactoryException if an error occurred while closing the connections.
     */
    @AfterAll
    public void close() throws FactoryException {
        dataEPSG.close();
    }

    /**
     * Tests "Campo Inchauspe / Argentina 7" (EPSG:22197).
     * This projection has a <q>Latitude of natural origin</q> at the south pole.
     *
     * @throws FactoryException if an error occurred while creating the CRS.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-377">SIS-377 on issues tracker</a>
     */
    @Test
    public void testLatitudeAtPole() throws FactoryException {
        compare("PROJCRS[\"Campo Inchauspe / Argentina 7\",\n" +
                "  BASEGEODCRS[\"Campo Inchauspe\",\n" +
                "    DATUM[\"Campo Inchauspe\",\n" +
                "      ELLIPSOID[\"International 1924\",6378388,297,LENGTHUNIT[\"metre\",1.0]]]],\n" +
                "  CONVERSION[\"Argentina zone 7\",\n" +
                "    METHOD[\"Transverse Mercator\",ID[\"EPSG\",9807]],\n" +
                "    PARAMETER[\"Latitude of natural origin\",-90,ANGLEUNIT[\"degree\",0.01745329252]],\n" +
                "    PARAMETER[\"Longitude of natural origin\",-54,ANGLEUNIT[\"degree\",0.01745329252]],\n" +
                "    PARAMETER[\"Scale factor at natural origin\",1,SCALEUNIT[\"unity\",1.0]],\n" +
                "    PARAMETER[\"False easting\",7500000,LENGTHUNIT[\"metre\",1.0]],\n" +
                "    PARAMETER[\"False northing\",0,LENGTHUNIT[\"metre\",1.0]]],\n" +
                "  CS[cartesian,2],\n" +
                "    AXIS[\"northing (X)\",north,ORDER[1]],\n" +
                "    AXIS[\"easting (Y)\",east,ORDER[2]],\n" +
                "    LENGTHUNIT[\"metre\",1.0],\n" +
                "  ID[\"EPSG\",22197]]", 22197);
    }

    /**
     * Tests "Pulkovo 1942 / 3-degree Gauss-Kruger CM 180E" (EPSG:2636).
     * This projection has a <q>Longitude of natural origin</q> at the anti-meridian.
     *
     * @throws FactoryException if an error occurred while creating the CRS.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-377">SIS-377 on issues tracker</a>
     */
    @Test
    public void testLongitudeAtAntiMeridian() throws FactoryException {
        compare("PROJCRS[\"Pulkovo 1942 / 3-degree Gauss-Kruger CM 180E\",\n" +
                "  BASEGEODCRS[\"Pulkovo 1942\",\n" +
                "    DATUM[\"Pulkovo 1942\",\n" +
                "      ELLIPSOID[\"Krassowsky 1940\",6378245,298.3,LENGTHUNIT[\"metre\",1.0]]]],\n" +
                "  CONVERSION[\"3-degree Gauss-Kruger CM 180\",\n" +
                "    METHOD[\"Transverse Mercator\",ID[\"EPSG\",9807]],\n" +
                "    PARAMETER[\"Latitude of natural origin\",0,ANGLEUNIT[\"degree\",0.01745329252]],\n" +
                "    PARAMETER[\"Longitude of natural origin\",180,ANGLEUNIT[\"degree\",0.01745329252]],\n" +
                "    PARAMETER[\"Scale factor at natural origin\",1,SCALEUNIT[\"unity\",1.0]],\n" +
                "    PARAMETER[\"False easting\",500000,LENGTHUNIT[\"metre\",1.0]],\n" +
                "    PARAMETER[\"False northing\",0,LENGTHUNIT[\"metre\",1.0]]],\n" +
                "  CS[cartesian,2],\n" +
                "    AXIS[\"northing (X)\",north,ORDER[1]],\n" +
                "    AXIS[\"easting (Y)\",east,ORDER[2]],\n" +
                "    LENGTHUNIT[\"metre\",1.0],\n" +
                "  ID[\"EPSG\",2636]]", 2636);
    }

    /**
     * Tests "Belge 1950 (Brussels) / Belge Lambert 50" (EPSG:21500).
     * This projection has a <q>Latitude of false origin</q> at the anti-meridian.
     *
     * @throws FactoryException if an error occurred while creating the CRS.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-377">SIS-377 on issues tracker</a>
     */
    @Test
    public void testLambert() throws FactoryException {
        compare("PROJCRS[\"Belge 1950 (Brussels) / Belge Lambert 50\",\n" +
                "  BASEGEODCRS[\"Belge 1950 (Brussels)\",\n" +
                "    DATUM[\"Reseau National Belge 1950 (Brussels)\",\n" +
                "      ELLIPSOID[\"International 1924\",6378388,297,LENGTHUNIT[\"metre\",1.0]]],\n" +
                "    PRIMEM[\"Brussels\",4.367975,ANGLEUNIT[\"degree\",0.01745329252]]],\n" +
                "  CONVERSION[\"Belge Lambert 50\",\n" +
                "    METHOD[\"Lambert Conic Conformal (2SP)\",ID[\"EPSG\",9802]],\n" +
                "    PARAMETER[\"Latitude of false origin\",90,ANGLEUNIT[\"degree\",0.01745329252]],\n" +
                "    PARAMETER[\"Longitude of false origin\",0,ANGLEUNIT[\"degree\",0.01745329252]],\n" +
                "    PARAMETER[\"Latitude of 1st standard parallel\",49.833333333333,ANGLEUNIT[\"degree\",0.01745329252]],\n" +
                "    PARAMETER[\"Latitude of 2nd standard parallel\",51.166666666667,ANGLEUNIT[\"degree\",0.01745329252]],\n" +
                "    PARAMETER[\"Easting at false origin\",150000,LENGTHUNIT[\"metre\",1.0]],\n" +
                "    PARAMETER[\"Northing at false origin\",5400000,LENGTHUNIT[\"metre\",1.0]]],\n" +
                "  CS[cartesian,2],\n" +
                "    AXIS[\"easting (X)\",east,ORDER[1]],\n" +
                "    AXIS[\"northing (Y)\",north,ORDER[2]],\n" +
                "    LENGTHUNIT[\"metre\",1.0],\n" +
                "  ID[\"EPSG\",21500]]", 21500);
    }

    /**
     * Compares a projected CRS parsed from a WKT with a the CRS built from EPSG database.
     * The latter is taken as the reference.
     */
    private void compare(final String wkt, final int epsg) throws FactoryException {
        final CoordinateReferenceSystem crs = CRS.fromWKT(wkt);
        final EPSGFactory factory = dataEPSG.factory();
        final CoordinateReferenceSystem reference = factory.createProjectedCRS(Integer.toString(epsg));
        assertEqualsIgnoreMetadata(reference, crs);
    }

    /**
     * Tests formatting a coordinate operation from an EPSG code and parsing it back.
     *
     * @throws FactoryException if an error occurred while creating the coordinate operation.
     * @throws ParseException if the WKT cannot be parsed.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-512">SIS-512 on issues tracker</a>
     */
    @Test
    public void testCoordinateOperation() throws FactoryException, ParseException {
        final EPSGFactory factory = dataEPSG.factory();
        CoordinateOperation opFromCode = factory.createCoordinateOperation("5630");
        String wkt = opFromCode.toWKT();
        WKTFormat parser = new WKTFormat();
        CoordinateOperation opFromWKT = (CoordinateOperation) parser.parseObject(wkt);
        assertEqualsIgnoreMetadata(opFromCode, opFromWKT);
    }
}
