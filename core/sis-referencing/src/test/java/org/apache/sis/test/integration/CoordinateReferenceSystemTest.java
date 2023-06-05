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
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assume.assumeNotNull;
import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertInstanceOf;


/**
 * Advanced CRS constructions requiring the EPSG geodetic dataset.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   0.8
 */
@DependsOn({
    org.apache.sis.referencing.CRSTest.class,
    org.apache.sis.referencing.CommonCRSTest.class,
    org.apache.sis.referencing.factory.sql.EPSGFactoryTest.class,
    org.apache.sis.referencing.factory.MultiAuthoritiesFactoryTest.class
})
public final class CoordinateReferenceSystemTest extends TestCase {
    /**
     * Tests creation from codes in the
     * {@code "urn:ogc:def:type, type₁:authority₁:version₁:code₁, type₂:authority₂:version₂:code₂"} form.
     *
     * @throws FactoryException if an authority or a code is not recognized.
     */
    @Test
    public void testCreateFromCombinedURN() throws FactoryException {
        assumeNotNull(TestFactorySource.getSharedFactory());
        CoordinateReferenceSystem crs = CRS.forCode("urn:ogc:def:crs, crs:EPSG::27700, crs:EPSG::5701");
        assertSame("OSGB 1936 / British National Grid + ODN height", CRS.forCode("EPSG:7405"), crs);
    }

    /**
     * Tests creation of "EPSG topocentric example A/B". They are derived geodetic CRS.
     *
     * @throws FactoryException if an authority or a code is not recognized.
     */
    @Test
    public void testDerivedCRS() throws FactoryException {
        assumeNotNull(TestFactorySource.getSharedFactory());
        CoordinateReferenceSystem crs = CRS.forCode("EPSG:5820");
        assertInstanceOf("Derived CRS type",  DerivedCRS .class, crs);
        assertInstanceOf("Derived CRS type",  GeodeticCRS.class, crs);
        assertInstanceOf("CS of derived CRS", CartesianCS.class, crs.getCoordinateSystem());
        assertInstanceOf("CS of base CRS",    CartesianCS.class, ((GeneralDerivedCRS) crs).getBaseCRS().getCoordinateSystem());
        /*
         * Some tests are disabled because `EPSGDataAccess` confuse this derived CRS
         * with a projected CRS. We are waiting for upgrade to EPSG database 10+
         * before to re-evaluate how to fix this issue.
         *
         * https://issues.apache.org/jira/browse/SIS-518
         */
        crs = CRS.forCode("EPSG:5819");
//      assertInstanceOf("Derived CRS type",  DerivedCRS .class, crs);
//      assertInstanceOf("Derived CRS type",  GeodeticCRS.class, crs);
        assertInstanceOf("CS of derived CRS", CartesianCS.class, crs.getCoordinateSystem());
        assertInstanceOf("CS of base CRS",    EllipsoidalCS.class, ((GeneralDerivedCRS) crs).getBaseCRS().getCoordinateSystem());
    }
}
