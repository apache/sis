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

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.sql.SQLTransientException;
import org.opengis.util.FactoryException;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.cs.CSAuthorityFactory;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;
import org.apache.sis.internal.referencing.LazySet;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.system.SystemListener;
import org.apache.sis.internal.referencing.EPSGFactoryProxy;
import org.apache.sis.referencing.factory.MultiAuthoritiesFactory;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.referencing.factory.UnavailableFactoryException;
import org.apache.sis.referencing.factory.sql.EPSGFactory;
import org.apache.sis.util.logging.Logging;


/**
 * Provides the CRS, CS, datum and coordinate operation authority factories.
 * Provides also the system-wide {@link MultiAuthoritiesFactory} instance used by {@link CRS#forCode(String)}.
 * Current version handles the EPSG factory in a special way, but we may try to avoid doing special cases in a
 * future SIS version (this may require more help from {@link ServiceLoader}).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.7
 * @module
 */
final class AuthorityFactories<T extends AuthorityFactory> extends LazySet<T> {
    /**
     * An array containing only the EPSG factory. Content of this array is initially null.
     * The EPSG factory will be created when first needed by {@link #initialValues()}.
     * This array is returned directly (not cloned) by {@link #initialValues()}.
     */
    private static final GeodeticAuthorityFactory[] EPSG = new GeodeticAuthorityFactory[1];

    /**
     * The unique system-wide authority factory instance that contains all factories found on the classpath,
     * plus the EPSG factory. The {@link EPSGFactoryProxy} most be excluded from this list, since the EPSG
     * factory is handled in a special way.
     */
    static final MultiAuthoritiesFactory ALL = new MultiAuthoritiesFactory(
            new AuthorityFactories<>(CRSAuthorityFactory.class),
            new AuthorityFactories<>(CSAuthorityFactory.class),
            new AuthorityFactories<>(DatumAuthorityFactory.class),
            new AuthorityFactories<>(CoordinateOperationAuthorityFactory.class))
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
        super(type);
    }

    /**
     * Sets the EPSG factory to the given value.
     */
    static void EPSG(final GeodeticAuthorityFactory factory) {
        synchronized (EPSG) {
            EPSG[0] = factory;
        }
    }

    /**
     * Returns the factory connected to the EPSG geodetic dataset if possible, or the EPSG fallback otherwise.
     * If an EPSG data source has been found, then this method returns an instance of {@link EPSGFactory} but
     * there is no guarantee that attempts to use that factory will succeed; for example maybe the EPSG schema
     * does not exist. Callers should be prepared to either receive an {@link EPSGFactoryFallback} directly if
     * the EPSG data source does not exist, or replace the {@code EPSGFactory} by a {@code EPSGFactoryFallback}
     * later if attempt to use the returned factory fails.
     */
    static GeodeticAuthorityFactory EPSG() {
        synchronized (EPSG) {
            GeodeticAuthorityFactory factory = EPSG[0];
            if (factory == null) {
                try {
                    factory = new EPSGFactory(null);
                } catch (FactoryException e) {
                    log(e, false);
                    factory = EPSGFactoryFallback.INSTANCE;
                }
                EPSG[0] = factory;
            }
            return factory;
        }
    }

    /**
     * Returns the fallback to use if the authority factory is not available. Unless the problem may be temporary,
     * this method replaces the {@link EPSGFactory} instance by {@link EPSGFactoryFallback} in order to prevent
     * the same exception to be thrown and logged on every calls to {@link CRS#forCode(String)}.
     */
    static GeodeticAuthorityFactory fallback(final UnavailableFactoryException e) throws UnavailableFactoryException {
        final boolean isTransient = (e.getCause() instanceof SQLTransientException);
        final AuthorityFactory unavailable = e.getUnavailableFactory();
        GeodeticAuthorityFactory factory;
        final boolean alreadyDone;
        synchronized (EPSG) {
            factory = EPSG[0];
            alreadyDone = (factory == EPSGFactoryFallback.INSTANCE);
            if (!alreadyDone) {                             // May have been set in another thread (race condition).
                if (unavailable != factory) {
                    throw e;                                // Exception did not come from a factory that we control.
                }
                factory = EPSGFactoryFallback.INSTANCE;
                if (!isTransient) {
                    ALL.reload();
                    EPSG[0] = factory;
                }
            }
        }
        if (!alreadyDone) {
            log(e, true);
        }
        return factory;
    }

    /**
     * Notifies that a factory is unavailable, but without giving a fallback and without logging.
     * The caller is responsible for throwing an exception, or for logging a warning and provide its own fallback.
     *
     * @return {@code false} if the caller can try again, or {@code true} if the failure can be considered final.
     */
    static boolean failure(final UnavailableFactoryException e) {
        if (!(e.getCause() instanceof SQLTransientException)) {
            final AuthorityFactory unavailable = e.getUnavailableFactory();
            synchronized (EPSG) {
                final GeodeticAuthorityFactory factory = EPSG[0];
                if (factory == EPSGFactoryFallback.INSTANCE) {      // May have been set in another thread.
                    return false;
                }
                if (unavailable == factory) {
                    ALL.reload();
                    EPSG[0] = EPSGFactoryFallback.INSTANCE;
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Logs the given exception at the given level. This method pretends that the logging come from
     * {@link CRS#getAuthorityFactory(String)}, which is the public facade for {@link #EPSG()}.
     */
    private static void log(final Exception e, final boolean isWarning) {
        String message = e.getMessage();        // Prefer the locale of system administrator.
        if (message == null) {
            message = e.toString();
        }
        final LogRecord record = new LogRecord(isWarning ? Level.WARNING : Level.CONFIG, message);
        if (isWarning && !(e instanceof UnavailableFactoryException)) {
            record.setThrown(e);
        }
        record.setLoggerName(Loggers.CRS_FACTORY);
        Logging.log(CRS.class, "getAuthorityFactory", record);
    }

    /**
     * Invoked by {@link LazySet} for adding the EPSG factory before any other factory fetched by {@code ServiceLoader}.
     * We put the EPSG factory first because it is often used anyway even for {@code CRS} and {@code AUTO} namespaces.
     *
     * <p>This method tries to instantiate an {@link EPSGFactory} if possible,
     * or an {@link EPSGFactoryFallback} otherwise.</p>
     *
     * @return the EPSG factory in an array. Callers shall not modify the returned array.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected T[] initialValues() {
        EPSG();                         // Force EPSGFactory instantiation if not already done.
        return (T[]) EPSG;
    }

    /**
     * Invoked by {@link LazySet} for fetching the next element from the given iterator.
     * Skips the {@link EPSGFactoryProxy} if possible, or returns {@code null} otherwise.
     * Note that {@link MultiAuthoritiesFactory} is safe to null values.
     */
    @Override
    protected T next(final Iterator<? extends T> it) {
        T e = it.next();
        if (e instanceof EPSGFactoryProxy) {
            e = it.hasNext() ? it.next() : null;
        }
        return e;
    }
}
