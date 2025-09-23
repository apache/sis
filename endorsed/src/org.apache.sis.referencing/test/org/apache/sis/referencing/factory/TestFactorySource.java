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

import java.util.HashMap;
import org.postgresql.ds.PGSimpleDataSource;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.metadata.sql.internal.shared.Initializer;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.factory.sql.EPSGFactory;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import static org.opengis.test.Assertions.assertBetween;


/**
 * Provides an EPSG factory to use for testing purpose, or notifies if no factory is available.
 * This is the common class used by all tests that need a full EPSG geodetic dataset to be installed.
 * Use this class as below:
 *
 * {@snippet lang="java" :
 *     @TestInstance(TestInstance.Lifecycle.PER_CLASS)
 *     public class MyTest {
 *         private final TestFactorySource dataEPSG;
 *
 *         public MyTest() throws FactoryException {
 *             dataEPSG = new TestFactorySource();
 *         }
 *
 *         @AfterAll
 *         public void close() throws FactoryException {
 *             dataEPSG.close();
 *         }
 *
 *         @Test
 *         public void testFoo() {
 *             final EPSGFactory factory = dataEPSG.factory();
 *             // Test can happen now.
 *         }
 *     }
 *     }
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class TestFactorySource {
    /**
     * Whether to use PostgreSQL instead of Derby for the tests. This field should be {@code false};
     * the {@code true} value is used only for temporarily debugging of PostgreSQL-specific features.
     * It is developer responsibility to setup a {@code "SpatialMetadata"} database on the local host.
     * This method differs from {@link org.apache.sis.metadata.sql.TestDatabase} by querying a permanent
     * database instead of a temporary database to be deleted after the tests.
     *
     * @see org.apache.sis.metadata.sql.TestDatabase#createOnPostgreSQL(String, boolean)
     */
    private static final boolean TEST_ON_POSTGRESQL = false;
    static {
        if (TEST_ON_POSTGRESQL) {
            final PGSimpleDataSource ds = new PGSimpleDataSource();
            // Server default to "localhost".
            ds.setDatabaseName(Initializer.DATABASE);
            Initializer.setDefault(() -> ds);
        }
    }

    /**
     * The factory instance to use for the tests, or {@code null} if not available.
     */
    private final EPSGFactory factory;

    /**
     * {@code true} if we failed to create the {@link #factory}.
     */
    private static boolean isUnavailable;

    /**
     * Returns the system-wide EPSG factory, or interrupts the tests with JUnit {@code Assumptions}
     * if the EPSG factory is not available. Note that this method breaks isolation between tests.
     * For more isolated tests, use {@link #createFactory()} and {@link #close()} instead.
     *
     * @return the system-wide EPSG factory. Never null if this method returns.
     * @throws FactoryException if an error occurred while fetching the factory.
     */
    public static synchronized EPSGFactory getSharedFactory() throws FactoryException {
        assumeFalse(isUnavailable, "No connection to EPSG dataset.");
        final CRSAuthorityFactory crsFactory = CRS.getAuthorityFactory(Constants.EPSG);
        assumeTrue(crsFactory instanceof EPSGFactory, "No connection to EPSG dataset.");
        try {
            assertNotNull(crsFactory.createGeographicCRS("4326"));
        } catch (UnavailableFactoryException e) {
            isUnavailable = true;
            abort("No connection to EPSG dataset. Caused by:" + System.lineSeparator() + e);
        }
        return (EPSGFactory) crsFactory;
    }

    /**
     * Creates the factory to use for all tests in a class.
     * If this method fails to create the factory, then {@link #factory} is left to {@code null} value.
     *
     * @throws FactoryException if an error occurred while creating the factory.
     */
    public TestFactorySource() throws FactoryException {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        EPSGFactory factory = null;
        synchronized (TestFactorySource.class) {
            if (!isUnavailable) {
                final GeodeticObjectFactory f = GeodeticObjectFactory.provider();
                final var properties = new HashMap<String,Object>(6);
                assertNull(properties.put("datumFactory", f));
                assertNull(properties.put("csFactory", f));
                assertNull(properties.put("crsFactory", f));
                boolean success = false;
                try {
                    factory = new EPSGFactory(properties);
                    assertEquals(0, ((ConcurrentAuthorityFactory) factory).countAvailableDataAccess(),
                                 "Expected no Data Access Object (DAO) before the first test is run.");
                    /*
                     * Above method call may fail if no data source has been specified.
                     * Following method call may fail if a data source has been specified,
                     * but the database does not contain the required tables.
                     */
                    assertNotNull(factory.createUnit(String.valueOf(Constants.EPSG_METRE)));
                    success = true;
                } catch (UnavailableFactoryException e) {
                    isUnavailable = true;
                    String message = "Tests that require the EPSG database cannot be run. Caused by:"
                                   + System.lineSeparator() + e;
                    GeodeticAuthorityFactory.LOGGER.config(message);
                } finally {
                    if (!success && factory != null) {
                        factory.close();
                        factory = null;
                    }
                }
            }
            this.factory = factory;
        }
    }

    /**
     * Returns the factory, or interrupt the test with a JUnit {@code Assumptions} if there is no factory available.
     *
     * @return the factory (never null if this method returns).
     */
    public EPSGFactory factory() {
        assumeTrue(factory != null, "No connection to EPSG dataset.");
        return factory;
    }

    /**
     * Forces release of JDBC connections after the tests in a class.
     *
     * @throws FactoryException if an error occurred while closing the connections.
     */
    public void close() throws FactoryException {
        if (factory != null) {
            final int n = ((ConcurrentAuthorityFactory) factory).countAvailableDataAccess();
            factory.close();
            assertBetween(0, 1, n, "Since we ran all tests sequentially, should have no more than 1 Data Access Object (DAO).");
        }
    }
}
