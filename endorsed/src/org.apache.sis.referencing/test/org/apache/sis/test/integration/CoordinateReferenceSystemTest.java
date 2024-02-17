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
package org.apache.sis.test.integration;

import org.opengis.util.FactoryException;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.crs.DerivedCRS;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.GeneralDerivedCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.factory.TestFactorySource;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Advanced CRS constructions requiring the EPSG geodetic dataset.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class CoordinateReferenceSystemTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public CoordinateReferenceSystemTest() {
    }

    /**
     * Tests creation from codes in the
     * {@code "urn:ogc:def:type, type₁:authority₁:version₁:code₁, type₂:authority₂:version₂:code₂"} form.
     *
     * @throws FactoryException if an authority or a code is not recognized.
     */
    @Test
    public void testCreateFromCombinedURN() throws FactoryException {
        assumeTrue(TestFactorySource.getSharedFactory() != null);
        CoordinateReferenceSystem crs = CRS.forCode("urn:ogc:def:crs, crs:EPSG::27700, crs:EPSG::5701");
        assertSame(CRS.forCode("EPSG:7405"), crs, "OSGB 1936 / British National Grid + ODN height");
    }

    /**
     * Tests creation of "EPSG topocentric example A/B". They are derived geodetic CRS.
     *
     * @throws FactoryException if an authority or a code is not recognized.
     */
    @Test
    public void testDerivedCRS() throws FactoryException {
        assumeTrue(TestFactorySource.getSharedFactory() != null);
        CoordinateReferenceSystem crs = CRS.forCode("EPSG:5820");
        assertInstanceOf(DerivedCRS .class, crs);
        assertInstanceOf(GeodeticCRS.class, crs);
        assertInstanceOf(CartesianCS.class, crs.getCoordinateSystem());
        assertInstanceOf(CartesianCS.class, ((GeneralDerivedCRS) crs).getBaseCRS().getCoordinateSystem());
        /*
         * Some tests are disabled because `EPSGDataAccess` confuse this derived CRS
         * with a projected CRS. We are waiting for upgrade to EPSG database 10+
         * before to re-evaluate how to fix this issue.
         *
         * https://issues.apache.org/jira/browse/SIS-518
         */
        crs = CRS.forCode("EPSG:5819");
//      assertInstanceOf(DerivedCRS .class, crs);
//      assertInstanceOf(GeodeticCRS.class, crs);
        assertInstanceOf(CartesianCS.class, crs.getCoordinateSystem());
        assertInstanceOf(EllipsoidalCS.class, ((GeneralDerivedCRS) crs).getBaseCRS().getCoordinateSystem());
    }
}
