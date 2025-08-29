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

import java.util.Set;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.sql.SQLTransientException;
import org.opengis.util.FactoryException;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.CSAuthorityFactory;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;
import org.apache.sis.referencing.privy.LazySet;
import org.apache.sis.system.Reflect;
import org.apache.sis.system.Loggers;
import org.apache.sis.system.Modules;
import org.apache.sis.system.Configuration;
import org.apache.sis.system.SystemListener;
import org.apache.sis.referencing.internal.EPSGFactoryProxy;
import org.apache.sis.referencing.factory.MultiAuthoritiesFactory;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.referencing.factory.IdentifiedObjectFinder;
import org.apache.sis.referencing.factory.UnavailableFactoryException;
import org.apache.sis.referencing.factory.sql.EPSGFactory;
import org.apache.sis.referencing.privy.FilteredIterator;
import org.apache.sis.util.logging.Logging;


/**
 * Provides the <abbr>CRS</abbr>, <abbr>CS</abbr>, datum and coordinate operation authority factories.
 * Provides also the system-wide {@link MultiAuthoritiesFactory} instance used by {@link CRS#forCode(String)}.
 * Current version handles the <abbr>EPSG</abbr> factory in a special way, but we may try to avoid doing special
 * cases in a future <abbr>SIS</abbr> version (this may require more help from {@link ServiceLoader}).
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class AuthorityFactories<T extends AuthorityFactory> extends LazySet<T> {
    /**
     * The logger to use for reporting object creations.
     */
    static final Logger LOGGER = Logger.getLogger(Loggers.CRS_FACTORY);

    /**
     * The EPSG factory, or {@code null} if not yet initialized.
     * The EPSG factory will be created when first needed by {@link #initialValues()}.
     */
    private static GeodeticAuthorityFactory EPSG;

    /**
     * The unique system-wide authority factory instance that contains all factories found on the module path,
     * plus the EPSG factory. The {@link EPSGFactoryProxy} must be excluded from this list, since the EPSG
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
            setEPSG(null);
            super.reload();
        }
    };

    /**
     * Registers a hook for forcing {@code ALL} to reload all CRS, CS, datum and coordinate operation factories
     * when the module path changed.
     */
    static {
        SystemListener.add(new SystemListener(Modules.REFERENCING) {
            @Override protected void classpathChanged() {ALL.reload();}
        });
    }

    /**
     * The type of service to request with {@link ServiceLoader}, or {@code null} if unknown.
     */
    private final Class<T> service;

    /**
     * Creates a new provider for factories of the given type.
     */
    private AuthorityFactories(final Class<T> type) {
        service = type;
    }

    /**
     * Creates the iterator which will provide the elements of this set before filtering.
     */
    @Override
    protected Iterator<? extends T> createSourceIterator() {
        ServiceLoader<T> loader;
        try {
            loader = ServiceLoader.load(service, Reflect.getContextClassLoader());
        } catch (SecurityException e) {
            Reflect.log(AuthorityFactories.class, "createSourceIterator", e);
            loader = ServiceLoader.load(service);
        }
        // Excludes the `EPSGFactoryProxy` instance.
        return new FilteredIterator<>(loader.iterator(),
                (element) -> (element instanceof EPSGFactoryProxy) ? null : element);
    }

    /**
     * Invoked by {@link LazySet} for adding the EPSG factory before any other factory fetched by {@code ServiceLoader}.
     * We put the EPSG factory first because it is often used anyway even for {@code CRS} and {@code AUTO} namespaces.
     * This method tries to instantiate an {@link EPSGFactory} if possible, or an {@link EPSGFactoryFallback} otherwise.
     *
     * @return the EPSG factory in an array.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected T[] initialValues() {
        return (T[]) new GeodeticAuthorityFactory[] {getEPSG(true)};
    }

    /**
     * Sets the EPSG factory to the given value.
     *
     * @param  factory  the factory to use, or {@code null} for reloading.
     */
    @Configuration
    static synchronized void setEPSG(final GeodeticAuthorityFactory factory) {
        EPSG = factory;
    }

    /**
     * Returns the factory connected to the <abbr>EPSG</abbr> geodetic dataset if possible, or the fallback otherwise.
     * If an <abbr>EPSG</abbr> data source has been found, then this method returns an instance of {@link EPSGFactory}.
     * But unless {@code test} is {@code true}, there is no guarantee that attempts to use that factory will succeed.
     * For example, maybe the {@code EPSG} schema does not exist and no installation scripts are available on the module-path.
     * Callers should be prepared to either receive an {@link EPSGFactoryFallback} directly if the EPSG data source does not exist,
     * or replace the {@code EPSGFactory} by a {@code EPSGFactoryFallback} later if attempt to use the returned factory fails.
     */
    static synchronized GeodeticAuthorityFactory getEPSG(final boolean test) {
        if (EPSG == null) try {
            EPSG = new EPSGFactory(null);
            if (test) {
                EPSG.createPrimeMeridian(StandardDefinitions.GREENWICH);
            }
        } catch (FactoryException e) {
            log(e, false);
            EPSG = EPSGFactoryFallback.INSTANCE;
        }
        return EPSG;
    }

    /**
     * Returns the fallback to use if the authority factory is not available. Unless the problem may be temporary,
     * this method replaces the {@link EPSGFactory} instance by {@link EPSGFactoryFallback} in order to prevent
     * the same exception to be thrown and logged on every calls to {@link CRS#forCode(String)}.
     */
    static GeodeticAuthorityFactory fallback(final UnavailableFactoryException e) throws UnavailableFactoryException {
        final AuthorityFactory unavailable = e.getUnavailableFactory();
        if (unavailable instanceof EPSGFactoryFallback) {
            throw e;
        }
        boolean isWarning = true;
        if (!(e.getCause() instanceof SQLTransientException)) {
            synchronized (AuthorityFactories.class) {
                if (unavailable == EPSG) {
                    ALL.reload();               // Must be before setting the `EPSG` field.
                    EPSG = EPSGFactoryFallback.INSTANCE;
                    isWarning = false;          // Use config level.
                }
            }
        }
        log(e, isWarning);
        return EPSGFactoryFallback.INSTANCE;    // Do not return `EPSG` because it may still have the previous value.
    }

    /**
     * Notifies that a factory is unavailable, but without logging.
     * The callers are responsible for either throwing an exception,
     * or for logging a warning and do their own fallback.
     *
     * @return {@code false} if the caller may want to try again, or
     *         {@code true} if the failure is considered definitive.
     */
    static boolean isUnavailable(final UnavailableFactoryException e) {
        final AuthorityFactory unavailable = e.getUnavailableFactory();
        if (unavailable instanceof EPSGFactoryFallback) {
            return true;
        }
        if (e.getCause() instanceof SQLTransientException) {
            // Not definitive, but caller should still throw an exception or log a warning.
            return true;
        }
        synchronized (AuthorityFactories.class) {
            if (unavailable == EPSG) {
                ALL.reload();   // Must be before setting the `EPSG` field.
                EPSG = EPSGFactoryFallback.INSTANCE;
            }
        }
        return false;
    }

    /**
     * Logs the given exception at the given level. This method pretends that the logging come from
     * {@link CRS#getAuthorityFactory(String)}, which is the public facade for {@link #getEPSG(boolean)}.
     */
    private static void log(final Exception e, final boolean isWarning) {
        String message = e.getMessage();        // Prefer the locale of system administrator.
        if (message == null) {
            message = e.toString();
        }
        final var record = new LogRecord(isWarning ? Level.WARNING : Level.CONFIG, message);
        if (isWarning && !(e instanceof UnavailableFactoryException)) {
            record.setThrown(e);
        }
        Logging.completeAndLog(LOGGER, CRS.class, "getAuthorityFactory", record);
    }

    /**
     * Creates a finder which can be used for looking up unidentified objects using the EPSG database.
     * The returned finder uses the fallback if the main <abbr>EPSG</abbr> factory appears to be unavailable.
     *
     * @return a finder to use for looking up unidentified objects.
     * @throws FactoryException if the finder cannot be created.
     */
    static IdentifiedObjectFinder finderForEPSG() throws FactoryException {
        final GeodeticAuthorityFactory factory = getEPSG(false);
        if (factory instanceof EPSGFactoryFallback) {
            return ((EPSGFactoryFallback) factory).newIdentifiedObjectFinder();
        }
        return new IdentifiedObjectFinder.Wrapper(factory.newIdentifiedObjectFinder()) {
            /** Whether the fallback has already been set. */
            private boolean isUsingFallback;

            /** Report that the main factory is not available and switch to the fallback. */
            private void report(UnavailableFactoryException e) throws FactoryException {
                if (isUsingFallback) throw e;
                isUsingFallback = true;
                delegate(fallback(e).newIdentifiedObjectFinder());
            }

            /** Lookups objects which are approximately equal, using the fallback if necessary. */
            @Override public Set<IdentifiedObject> find(final IdentifiedObject object) throws FactoryException {
                for (;;) try {      // Executed at most twice.
                    return super.find(object);
                } catch (UnavailableFactoryException e) {
                    report(e);
                }
            }

            /** Lookups an object which is approximately equal, using the fallback if necessary. */
            @Override public IdentifiedObject findSingleton(final IdentifiedObject object) throws FactoryException {
                for (;;) try {      // Executed at most twice.
                    return super.findSingleton(object);
                } catch (UnavailableFactoryException e) {
                    report(e);
                }
            }

            /** Returns a set of authority codes, using the fallback if necessary. */
            @Override protected Iterable<String> getCodeCandidates(final IdentifiedObject object) throws FactoryException {
                for (;;) try {      // Executed at most twice.
                    return super.getCodeCandidates(object);
                } catch (UnavailableFactoryException e) {
                    report(e);
                }
            }
        };
    }
}
