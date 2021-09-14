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

import org.opengis.util.FactoryException;
import org.apache.sis.internal.system.Loggers;

// Test imports
import org.junit.Rule;
import org.junit.Test;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.LoggingWatcher;


/**
 * Tests {@link org.apache.sis.referencing.factory.sql.EPSGDataAccess#createCoordinateOperation(String)}
 * for map projections.
 * This is part of <cite>Geospatial Integrity of Geoscience Software</cite> (GIGS) tests implemented in GeoAPI.
 *
 * <div class="note"><b>Note:</b>
 * this test is defined in this package instead of in the {@code sql} sub-package because of the need to access
 * package-private methods in {@link ConcurrentAuthorityFactory}, and for keeping all GIGS tests together.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.7
 * @since   0.7
 * @module
 */
@DependsOn({
    GIGS2001.class,     // Units created from EPSG codes
    GIGS3005.class      // Conversions created from properties
})
@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.JVM)      // Intentionally want some randomness
public final strictfp class GIGS2005 extends org.opengis.test.referencing.gigs.GIGS2005 {
    /**
     * A JUnit {@link Rule} for listening to log events. This field is public because JUnit requires us to
     * do so, but should be considered as an implementation details (it should have been a private field).
     */
    @Rule
    public final LoggingWatcher loggings = new LoggingWatcher(Loggers.CRS_FACTORY);

    /**
     * Creates a new test using the default authority factory.
     */
    public GIGS2005() {
        super(TestFactorySource.factory);
    }

    /**
     * Creates the factory to use for all tests in this class.
     *
     * @throws FactoryException if an error occurred while creating the factory.
     */
    @BeforeClass
    public static void createFactory() throws FactoryException {
        TestFactorySource.createFactory();
    }

    /**
     * Forces release of JDBC connections after the tests in this class.
     *
     * @throws FactoryException if an error occurred while closing the connections.
     */
    @AfterClass
    public static void close() throws FactoryException {
        TestFactorySource.close();
    }

    /**
     * Overrides the GeoAPI test for verifying the log messages emitted during the construction of deprecated objects.
     *
     * @throws FactoryException if an error occurred while creating the object.
     */
    @Test
    @Override
    public void testAustralianMapGridZones() throws FactoryException {
        super.testAustralianMapGridZones();
        loggings.assertNextLogContains("EPSG:17448");    // Falls outside EEZ area.
    }

    /**
     * Overrides the GeoAPI test for verifying the log messages emitted during the construction of deprecated objects.
     *
     * @throws FactoryException if an error occurred while creating the object.
     */
    @Test
    @Override
    public void testUSStatePlaneZones_LCC() throws FactoryException {
        super.testUSStatePlaneZones_LCC();
        loggings.assertNextLogContains("EPSG:12112");    // Method changed to accord with NGS practice.
        loggings.assertNextLogContains("EPSG:12113");
    }

    /**
     * Verifies that no unexpected warning has been emitted in this test.
     */
    @After
    public void assertNoUnexpectedLog() {
        loggings.assertNoUnexpectedLog();
    }
}
