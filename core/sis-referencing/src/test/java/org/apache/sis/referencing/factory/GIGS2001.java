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
import org.opengis.util.FactoryException;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.referencing.factory.sql.EPSGFactory;

import static org.opengis.test.Assert.*;


/**
 * Manages connection to the temporary database used by some Apache SIS tests.
 */
public final strictfp class GIGS2001 {
    /**
     * The factory instance to use for the tests, or {@code null} if not available.
     * This field is set by {@link #createFactory()} and cleared by {@link #close()}.
     */
    public static EPSGFactory factory;

    /**
     * {@code true} if we failed to create the {@link #factory}.
     */
    private static boolean isUnavailable;

    /**
     * Creates a new test using the default authority factory.
     */
    private GIGS2001() {
    }

    /**
     * Creates the factory to use for all tests in this class.
     * If this method fails to create the factory, then {@link #factory} is left to {@code null} value.
     *
     * @throws FactoryException if an error occurred while creating the factory.
     */
    @SuppressWarnings("null")
    public static void createFactory() throws FactoryException {
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
                    Logging.getLogger(Loggers.CRS_FACTORY).warning(e.toString());
                } finally {
                    if (factory != af) {
                        af.close();
                    }
                }
            }
        }
    }

    /**
     * Force releases of JDBC connections after the tests in this class.
     *
     * @throws FactoryException if an error occurred while closing the connections.
     */
    public static void close() throws FactoryException {
        final EPSGFactory af = factory;
        if (af != null) {
            factory = null;
            final int n = ((ConcurrentAuthorityFactory) af).countAvailableDataAccess();
            af.close();
            assertBetween("Since we ran all tests sequantially, should have no more than 1 Data Access Object (DAO).", 0, 1, n);
        }
    }
}
