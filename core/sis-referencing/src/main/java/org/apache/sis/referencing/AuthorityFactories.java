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
package org.apache.sis.referencing;

import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.opengis.util.FactoryException;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.cs.CSAuthorityFactory;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;
import org.apache.sis.internal.util.LazySet;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.system.SystemListener;
import org.apache.sis.referencing.factory.MultiAuthoritiesFactory;
import org.apache.sis.referencing.factory.sql.EPSGFactory;
import org.apache.sis.util.logging.Logging;


/**
 * Provides the CRS, CS, datum and coordinate operation authority factories.
 * Provides also the system-wide {@link MultiAuthoritiesFactory} instance used by {@link CRS#forCode(String)}.
 * Current version handles the EPSG factory in a special way, but we may try to avoid doing special cases in a
 * future SIS version (this may require more help from {@link ServiceLoader}).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class AuthorityFactories<T extends AuthorityFactory> extends LazySet<T> {
    /**
     * An array containing only the EPSG factory. Content of this array is initially null.
     * The EPSG factory will be created when first needed by {@link #initialValues()}.
     */
    private static final AuthorityFactory[] EPSG = new AuthorityFactory[1];

    /**
     * The unique system-wide authority factory instance that contains all factories found on the classpath,
     * plus the EPSG factory.
     */
    static final MultiAuthoritiesFactory ALL = new MultiAuthoritiesFactory(
            new AuthorityFactories<CRSAuthorityFactory>(CRSAuthorityFactory.class),
            new AuthorityFactories<CSAuthorityFactory>(CSAuthorityFactory.class),
            new AuthorityFactories<DatumAuthorityFactory>(DatumAuthorityFactory.class),
            new AuthorityFactories<CoordinateOperationAuthorityFactory>(CoordinateOperationAuthorityFactory.class))
    {
        /** Anonymous constructor */ {
            setLenient(true);
        }

        @Override
        public void reload() {
            EPSG(null);
            super.reload();
        }
    };

    /**
     * Registers a hook for forcing {@code ALL} to reload all CRS, CS, datum and coordinate operation factories
     * when the classpath changed.
     */
    static {
        SystemListener.add(new SystemListener(Modules.REFERENCING) {
            @Override protected void classpathChanged() {ALL.reload();}
        });
    }

    /**
     * Creates a new provider for factories of the given type.
     */
    private AuthorityFactories(final Class<T> type) {
        super(ServiceLoader.load(type));
    }

    /**
     * Sets the EPSG factory to the given value.
     */
    static void EPSG(final AuthorityFactory factory) {
        synchronized (EPSG) {
            EPSG[0] = factory;
        }
    }

    /**
     * Returns the EPSG factory.
     */
    static AuthorityFactory EPSG() {
        synchronized (EPSG) {
            AuthorityFactory factory = EPSG[0];
            if (factory == null) try {
                factory = new EPSGFactory(null);
            } catch (FactoryException e) {
                final LogRecord record = new LogRecord(Level.CONFIG, e.getLocalizedMessage());
                record.setLoggerName(Loggers.CRS_FACTORY);
                Logging.log(CRS.class, "getAuthorityFactory", record);
                factory = EPSGFactoryFallback.INSTANCE;
            }
            EPSG[0] = factory;
            return factory;
        }
    }

    /**
     * Returns the EPSG factory. This method tries to instantiate an {@link EPSGFactory} if possible,
     * or an {@link EPSGFactoryFallback} otherwise.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected T[] initialValues() {
        EPSG();                         // Force creation of EPSG factory if not already done.
        return (T[]) EPSG;
    }
}
