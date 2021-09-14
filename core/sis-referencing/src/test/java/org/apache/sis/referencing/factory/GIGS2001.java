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

// Test imports
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;



/**
 * Tests {@link org.apache.sis.referencing.factory.sql.EPSGDataAccess#createUnit(String)}.
 * This is part of <cite>Geospatial Integrity of Geoscience Software</cite> (GIGS) tests implemented in GeoAPI.
 *
 * <div class="note"><b>Note:</b>
 * this test is defined in this package instead of in the {@code sql} sub-package because of the need to access
 * package-private methods in {@link ConcurrentAuthorityFactory}, and for keeping all GIGS tests together.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.7
 * @module
 */
@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.JVM)      // Intentionally want some randomness
public final strictfp class GIGS2001 extends org.opengis.test.referencing.gigs.GIGS2001 {
    /**
     * Creates a new test using the default authority factory.
     */
    public GIGS2001() {
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
}
