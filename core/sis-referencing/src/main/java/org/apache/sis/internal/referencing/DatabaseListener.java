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
package org.apache.sis.internal.referencing;

import java.sql.Connection;
import java.sql.SQLException;
import org.opengis.util.FactoryException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.factory.MultiAuthoritiesFactory;
import org.apache.sis.internal.metadata.sql.Initializer;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.util.logging.Logging;


/**
 * Invoked when a new database is created or when the data source changed.
 * This listener is registered in the following file:
 *
 * {@preformat text
 *   META-INF/services/org.apache.sis.internal.metadata.sql.Initializer
 * }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final class DatabaseListener extends Initializer {
    /**
     * To be invoked by refection.
     */
    public DatabaseListener() {
    }

    /**
     * Invoked when a new database is created. The current Apache SIS version does not create the EPSG database
     * immediately. We rather rely on automatic installation by {@code EPSGInstaller} the first time that a CRS
     * is requested. However we may revisit this policy in a future SIS version.
     *
     * @param connection Connection to the empty database.
     * @throws SQLException if an error occurred while populating the database.
     */
    @Override
    protected void createSchema(Connection connection) throws SQLException {
    }

    /**
     * Invoked when the data source changed.
     */
    @Override
    protected void dataSourceChanged() {
        try {
            ((MultiAuthoritiesFactory) CRS.getAuthorityFactory(null)).reload();
        } catch (FactoryException e) {
            // Should never happen for a null argument given to CRS.getAuthorityFactory(…).
            Logging.unexpectedException(Logging.getLogger(Loggers.CRS_FACTORY), CRS.class, "getAuthorityFactory", e);
        }
    }
}
