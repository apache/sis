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

import java.util.Map;
import java.util.HashMap;
import org.postgresql.ds.PGSimpleDataSource;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.metadata.sql.Initializer;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.factory.sql.EPSGFactory;

import static org.junit.Assume.*;
import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertBetween;


/**
 * Provides an EPSG factory to use for testing purpose, or notifies if no factory is available.
 * This is the common class used by all tests that need a full EPSG geodetic dataset to be installed.
 * Use this class as below:
 *
 * {@snippet lang="java" :
 *     @BeforeClass
 *     public static void createFactory() throws FactoryException {
 *         TestFactorySource.createFactory();
 *     }
 *
 *     @AfterClass
 *     public static void close() throws FactoryException {
 *         TestFactorySource.close();
 *     }
 *
 *     @Test
 *     public void testFoo() {
 *         assumeNotNull(TestFactorySource.factory);
 *         // Test can happen now.
 *     }
 * }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.8
 */
public final class TestFactorySource {
    /**
     * Whether to use PostgreSQL instead of Derby for the tests. This field should be {@code false};
     * the {@code true} value is used only for temporarily debugging of PostgreSQL-specific features.
     * It is developer responsibility to setup a {@code "SpatialMetadata"} database on the local host.
     * This method differs from {@link org.apache.sis.test.sql.TestDatabase} by querying a permanent
     * database instead of a temporary database to be deleted after the tests.
     *
     * @see org.apache.sis.test.sql.TestDatabase#createOnPostgreSQL(String, boolean)
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
     * This field is set by {@link #createFactory()} and cleared by {@link #close()}.
     * Test classes using this field shall declare their own {@code createFactory()}
     * and {@code close()} methods delegating their work to the corresponding methods
     * in this {@code TestFactorySource} class.
     */
    public static EPSGFactory factory;

    /**
     * {@code true} if we failed to create the {@link #factory}.
     */
    private static boolean isUnavailable;

    /**
     * Do not allow instantiation of this class.
     */
    private TestFactorySource() {
    }

    /**
     * Returns the system-wide EPSG factory, or interrupts the tests with {@link org.junit.Assume}
     * if the EPSG factory is not available. Note that this method breaks isolation between tests.
     * For more isolated tests, use {@link #createFactory()} and {@link #close()} instead.
     *
     * @return the system-wide EPSG factory.
     * @throws FactoryException if an error occurred while fetching the factory.
     */
    public static synchronized EPSGFactory getSharedFactory() throws FactoryException {
        assumeFalse("No connection to EPSG dataset.", isUnavailable);
        final CRSAuthorityFactory factory = CRS.getAuthorityFactory(Constants.EPSG);
        assumeTrue("No connection to EPSG dataset.", factory instanceof EPSGFactory);
        try {
            assertNotNull(factory.createGeographicCRS("4326"));
        } catch (UnavailableFactoryException e) {
            isUnavailable = true;
            GeodeticAuthorityFactory.LOGGER.warning(e.toString());
            assumeNoException("No connection to EPSG dataset.", e);
        }
        return (EPSGFactory) factory;
    }

    /**
     * Creates the factory to use for all tests in a class.
     * If this method fails to create the factory, then {@link #factory} is left to {@code null} value.
     *
     * @throws FactoryException if an error occurred while creating the factory.
     */
    public static synchronized void createFactory() throws FactoryException {
        if (!isUnavailable) {
            EPSGFactory af = factory;
            if (af == null) {
                final GeodeticObjectFactory f = new GeodeticObjectFactory();
                final Map<String,Object> properties = new HashMap<>(6);
                assertNull(properties.put("datumFactory", f));
                assertNull(properties.put("csFactory", f));
                assertNull(properties.put("crsFactory", f));
                try {
                    af = new EPSGFactory(properties);
                    assertEquals("Expected no Data Access Object (DAO) before the first test is run.",
                                 0, ((ConcurrentAuthorityFactory) af).countAvailableDataAccess());
                    /*
                     * Above method call may fail if no data source has been specified.
                     * Following method call may fail if a data source has been specified,
                     * but the database does not contain the required tables.
                     */
                    assertNotNull(af.createUnit(String.valueOf(Constants.EPSG_METRE)));
                    factory = af;                                                           // Must be last.
                } catch (UnavailableFactoryException e) {
                    isUnavailable = true;
                    GeodeticAuthorityFactory.LOGGER.warning(e.toString());
                } finally {
                    if (factory != af) {
                        af.close();
                    }
                }
            }
        }
    }

    /**
     * Forces release of JDBC connections after the tests in a class.
     *
     * @throws FactoryException if an error occurred while closing the connections.
     */
    public static synchronized void close() throws FactoryException {
        final EPSGFactory af = factory;
        if (af != null) {
            factory = null;
            final int n = ((ConcurrentAuthorityFactory) af).countAvailableDataAccess();
            af.close();
            assertBetween("Since we ran all tests sequentially, should have no more than 1 Data Access Object (DAO).", 0, 1, n);
        }
    }
}
