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

import java.util.ServiceLoader;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.opengis.referencing.*;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.metadata.citation.Citation;
import org.opengis.util.FactoryException;
import org.apache.sis.internal.util.Citations;
import org.apache.sis.internal.util.DefinitionURI;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.iso.DefaultNameSpace;


/**
 * A factory that delegates the object creation to another factory determined from the <var>authority</var> part
 * in {@code "}<var>authority</var>{@code :}<var>code</var>{code "} arguments.
 * The list of factories to use as delegates can be specified at construction time.
 *
 * <p>This factory requires that every codes given to a {@code createFoo(String)} method are prefixed by
 * the authority name, for example {@code "EPSG:4326"}. When a {@code createFoo(String)} method is invoked,
 * this class extracts the authority name from the {@code "authority:code"} argument and searches
 * for a factory for that authority in the list of factories given at construction time.
 * If a factory is found, then the work is delegated to that factory.
 * Otherwise a {@link NoSuchAuthorityCodeException} is thrown.</p>
 *
 * <p>This factory can also parse URNs of the form
 * {@code "urn:ogc:def:}<var>type</var>{@code :}<var>authority</var>{@code :}<var>version</var>{@code :}<var>code</var>{@code "}.
 * In such case, the <var>type</var> specified in the URN may be used for invoking a more specific method.
 * However {@code MultiAuthoritiesFactory} uses the type information in the URN only for
 * delegating to a more specific method, never for delegating to a less specific method.
 * An exception will be thrown if the type in the URN is incompatible with the method invoked.</p>
 *
 * <div class="note"><b>Example:</b>
 * if the {@link #createObject(String)} method is invoked with the <code>"urn:ogc:def:<b>crs</b>:EPSG::4326"</code>
 * URN, then {@code MultiAuthoritiesFactory} will delegate (indirectly, ignoring caching for this example) the object
 * creation to {@link org.apache.sis.referencing.factory.sql.EPSGDataAccess#createCoordinateReferenceSystem(String)}
 * instead of {@link org.apache.sis.referencing.factory.sql.EPSGDataAccess#createObject(String)} because of the
 * {@code "crs"} part in the URN. The more specific method gives more performances and avoid ambiguities.</div>
 *
 * <div class="section">Multiple versions for the same authority</div>
 * {@code MultiAuthoritiesFactory} accepts an arbitrary amount of factories for the same authority, provided that
 * those factories have different version numbers. If a {@code createFoo(String)} method is invoked with a URN
 * containing a version number, then {@code MultiAuthoritiesFactory} will search for a factory with that exact
 * version, or throw a {@link NoSuchAuthorityCodeException} if no suitable factory is found.
 * If a {@code createFoo(String)} method is invoked with the version number omitted, then {@code MultiAuthoritiesFactory}
 * will use the first factory in iteration order for the requested authority regardless of its version number.
 *
 * <div class="note"><b>Example:</b>
 * a {@code MultiAuthoritiesFactory} instance could contain two {@code EPSGFactory} instances:
 * one for version 8.2 of the EPSG dataset and another one for version 7.9 of the EPSG dataset.
 * A specific version can be requested in the URN given to {@code createFoo(String)} methods,
 * for example <code>"urn:ogc:def:crs:EPSG:<b>8.2</b>:4326"</code>.
 * If no version is given as in {@code "urn:ogc:def:crs:EPSG::4326"},
 * then the first EPSG factory in iteration order is used regardless of its version number.
 * </div>
 *
 * <div class="section">Multi-threading</div>
 * This class is thread-safe if all delegate factories are themselves thread-safe.
 * However the factory <em>providers</em>, which are given to the constructor as
 * {@code Iterable<? extends AuthorityFactory>} instances, do not need to be thread-safe.
 * See constructor Javadoc for more information.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public abstract class MultiAuthoritiesFactory extends GeodeticAuthorityFactory implements CRSAuthorityFactory,
        CSAuthorityFactory, DatumAuthorityFactory, CoordinateOperationAuthorityFactory
{
    /**
     * The factory providers given at construction time. Elements in the array are for {@link CRSAuthorityFactory},
     * {@link CSAuthorityFactory}, {@link DatumAuthorityFactory} and {@link CoordinateOperationAuthorityFactory}
     * in that order. That order is defined by the constant values in {@link AuthorityFactoryIdentifier}.
     *
     * <p>Note that this array is shorter than the amount of {@link AuthorityFactoryIdentifier} values.
     * The last {@link AuthorityFactoryIdentifier} values are handled in a special way.</p>
     *
     * <p>The array may contain {@code null} elements when there is no provider for a given type.
     * Content of this array shall be immutable after construction time in order to avoid the need
     * for synchronization when reading the array. However usage of an {@code Iterable} element
     * shall be synchronized on that {@code Iterable}.</p>
     */
    private final Iterable<? extends AuthorityFactory>[] providers;

    /**
     * The factories obtained from the {@link #iterators}.
     */
    private final ConcurrentMap<AuthorityFactoryIdentifier, AuthorityFactory> factories;

    /**
     * A bit masks identifying which providers have given us all their factories.
     */
    private final AtomicInteger iterationCompleted;

    /**
     * Creates a new multi-factory using the given lists of factories.
     * Calls to {@code createFoo(String)} methods will scan the supplied factories in their iteration order when first needed.
     * The first factory having the expected {@linkplain GeodeticAuthorityFactory#getAuthority() authority name} will be used.
     *
     * <div class="section">Requirements</div>
     * {@code MultiAuthoritiesFactory} may iterate over the same {@code Iterable} more than once.
     * Each iteration <strong>shall</strong> return the same instances than previous iterations,
     * unless {@link #reload()} has been invoked.
     *
     * <p>The {@code Iterable}s do not need to be thread-safe.
     * {@code MultiAuthoritiesFactory} will use them only in blocks synchronized on the {@code Iterable} instance.
     * For example all usages of {@code crsFactory} will be done inside a {@code synchronized(crsFactory)} block.</p>
     *
     * <div class="section">Name collision</div>
     * If an {@code Iterable} contains more than one factory for the same namespace and version,
     * then only the first occurrence will be used. All additional factories for the same namespace
     * and version will be ignored, after a warning has been logged.
     *
     * <div class="section">Caching</div>
     * {@code MultiAuthoritiesFactory} caches the factories found from the given {@code Iterable}s,
     * but does not cache the objects created by those factories.
     * This constructor assumes that the given factories already do their own caching.
     *
     * @param crsFactories   The factories for creating {@link CoordinateReferenceSystem} objects, or null if none.
     * @param csFactories    The factories for creating {@link CoordinateSystem} objects, or null if none.
     * @param datumFactories The factories for creating {@link Datum} objects, or null if none.
     * @param copFactories   The factories for creating {@link CoordinateOperation} objects, or null if none.
     */
    @SuppressWarnings({"unchecked", "rawtypes", "empty-statement"})    // Generic array creation.
    public MultiAuthoritiesFactory(final Iterable<? extends CRSAuthorityFactory> crsFactories,
                                   final Iterable<? extends CSAuthorityFactory> csFactories,
                                   final Iterable<? extends DatumAuthorityFactory> datumFactories,
                                   final Iterable<? extends CoordinateOperationAuthorityFactory> copFactories)
    {
        final Iterable<? extends AuthorityFactory>[] p = new Iterable[4];
        p[AuthorityFactoryIdentifier.CRS]       = crsFactories;
        p[AuthorityFactoryIdentifier.CS]        = csFactories;
        p[AuthorityFactoryIdentifier.DATUM]     = datumFactories;
        p[AuthorityFactoryIdentifier.OPERATION] = copFactories;
        /*
         * Mark null Iterables as if we already iterated over all their elements.
         * Opportunistically reduce the array size by trimming trailing null elements.
         * The memory gain is negligible, but this will reduce the number of iterations in loops.
         */
        int length = 0, nullMask = 0;
        for (int i=0; i < p.length; i++) {
            if (p[i] != null) length = i+1;
            else nullMask |= (1 << i);
        }
        providers = ArraysExt.resize(p, length);
        factories = new ConcurrentHashMap<>();
        iterationCompleted = new AtomicInteger(nullMask);
    }

    /**
     * Returns the organization or party responsible for definition and maintenance of the database.
     * The default implementation returns {@code null} since {@code MultiAuthoritiesFactory} is not
     * about a particular authority.
     */
    @Override
    public Citation getAuthority() {
        return null;
    }

    /**
     * Returns the code spaces for the given factory.
     * This method delegates to {@link GeodeticAuthorityFactory#getCodeSpaces()} if possible,
     * or reproduces its default implementation otherwise.
     */
    private static Set<String> getCodeSpaces(final AuthorityFactory factory) {
        if (factory instanceof GeodeticAuthorityFactory) {
            return ((GeodeticAuthorityFactory) factory).getCodeSpaces();
        } else {
            final String authority = Citations.getCodeSpace(factory.getAuthority());
            return (authority != null) ? Collections.singleton(authority) : Collections.emptySet();
        }
    }

    /**
     * Caches the given factory, but without replacing existing instance if any.
     * This method returns the factory that we should use, either the given instance of the cached one.
     *
     * @param  identifier The type, authority and version of the factory to cache.
     * @param  factory The factory to cache.
     * @return The given {@code factory} if no previous instance was cached, or the existing instance otherwise.
     */
    private AuthorityFactory cache(final AuthorityFactoryIdentifier identifier, final AuthorityFactory factory) {
        final AuthorityFactory existing = factories.putIfAbsent(identifier.intern(), factory);
        return (existing != null) ? existing : factory;
    }

    /**
     * Returns the factory identified by the given type, authority and version. If no such factory is found in
     * the cache, then this method iterates on the factories created by the providers given at construction time.
     *
     * @param  request The type, authority and version of the desired factory.
     * @return The factory.
     * @throws NoSuchAuthorityFactoryException if no suitable factory has been found.
     */
    private AuthorityFactory getAuthorityFactory(final AuthorityFactoryIdentifier request)
            throws NoSuchAuthorityFactoryException
    {
        AuthorityFactory factory = factories.get(request);
        if (factory != null) {
            return factory;
        }
        /*
         * If there is no factory in the cache for the given type, authority and version, then check if the
         * default factory (when no version is specified) is actually the factory for the requested version.
         * The reason why we have to do this check is because we do not ask the version of a factory before
         * we really need to do so, since fetching this information is relatively costly for some factories
         * (e.g. EPSGFactory needs to look in the "Version History" table of the dataset) and rarely needed.
         */
        if (request.hasVersion()) {
            factory = factories.get(request.versionOf(null));
            if (factory != null && request.versionOf(factory.getAuthority()) == request) {
                // Default factory is for the version that user requested. Cache that finding.
                return cache(request, factory);
            }
        }
        /*
         * At this point we know that there is no factory in the cache for the requested type, authority and version.
         * Create new factories with the Iterables given at construction time. If we already started an iteration in
         * a previous call to this getAuthorityFactory(…) method, we will continue the search after skipping already
         * cached instances.
         */
        int doneMask = iterationCompleted.get();
        final int type = request.type;
        if ((doneMask & (1 << type)) == 0) {
            if (type >= 0 && type < providers.length) {
                final Iterable<? extends AuthorityFactory> provider = providers[type];
                synchronized (provider) {   // Should never be null because of the 'doneMask' check.
                    /*
                     * Check again in case another thread has found the factory after the check at
                     * the beginning of this method but before we entered in the synchronized block.
                     */
                    factory = factories.get(request);
                    if (factory != null) {
                        return factory;
                    }
                    /*
                     * Search for a factory for the given authority. Caches all factories that we find
                     * during the iteration process. Some factories may be already cached as a result
                     * of a partial iteration in a previous call to getAuthorityFactory(…).
                     */
                    for (final Iterator<? extends AuthorityFactory> it = provider.iterator(); it.hasNext();) {
                        factory = it.next();
                        AuthorityFactory found = null;
                        AuthorityFactory conflict = null;
                        for (final String namespace : getCodeSpaces(factory)) {
                            final AuthorityFactoryIdentifier unversioned = request.unversioned(namespace);
                            AuthorityFactory cached = cache(unversioned, factory);
                            if (request.equals(unversioned)) {
                                found = cached;
                            }
                            /*
                             * Only if we have no choice, ask to the factory what is its version number.
                             * We have no choice when ignoring the version number causes a conflict, or
                             * when the user asked for a specific version.
                             */
                            if (cached != factory || request.hasVersion()) {
                                // Make sure that we took in account the version number of the default factory.
                                cache(unversioned.versionOf(cached.getAuthority()), cached);

                                // Now check the version number of the new factory that we just fetched.
                                final AuthorityFactoryIdentifier versioned = unversioned.versionOf(factory.getAuthority());
                                if (versioned != unversioned) {
                                    cached = cache(versioned, factory);
                                    if (request.equals(versioned)) {
                                        found = cached;
                                    }
                                }
                                if (cached != factory) {
                                    conflict = cached;
                                }
                            }
                        }
                        if (conflict != null) {
                            // TODO: log a warning
                        }
                        if (found != null) {
                            return found;
                        }
                    }
                }
            } else if (type >= AuthorityFactoryIdentifier.GEODETIC) {
                /*
                 * Special cases: if the requested factory is ANY, take the first factory that we can found
                 * regardless of its type. We will try CRS, CS, DATUM and OPERATION factories in that order.
                 * The GEODETIC type is like ANY except for the additional restriction that the factory shall
                 * be an instance of the SIS-specific GeodeticAuthorityFactory class.
                 */
                assert providers.length < Math.min(type, Byte.MAX_VALUE) : type;
                for (byte i=0; i < providers.length; i++) {
                    factory = getAuthorityFactory(request.newType(i));
                    switch (type) {
                        case AuthorityFactoryIdentifier.ANY: {
                            return factory;
                        }
                        case AuthorityFactoryIdentifier.GEODETIC: {
                            if (factory instanceof GeodeticAuthorityFactory) {
                                return factory;
                            }
                        }
                    }
                }
            }
            /*
             * Remember that we have iterated over all elements of this provider, so we will not try again.
             * Note that the mask values may also be modified in other threads for other providers, so we
             * need to atomically verify that the current value has not been modified before to set it.
             */
            while (!iterationCompleted.compareAndSet(doneMask, doneMask | (1 << type))) {
                doneMask = iterationCompleted.get();
            }
        }
        final String authority = request.getAuthority();
        throw new NoSuchAuthorityFactoryException(Errors.format(Errors.Keys.UnknownAuthority_1, authority), authority);
    }

    /**
     * Returns an object from a code using the given proxy.
     *
     * @param  <T>   The type of the object to be returned.
     * @param  proxy The proxy to use for creating the object.
     * @param  code  The code of the object to create.
     * @return The object from one of the authority factory specified at construction time.
     * @throws FactoryException If an error occurred while creating the object.
     */
    private <T> T create(final AuthorityFactoryProxy<T> proxy, final String code) throws FactoryException {
        ArgumentChecks.ensureNonNull("code", code);
        final String authority, version;
        final DefinitionURI uri = DefinitionURI.parse(code);
        if (uri != null) {
            authority = uri.authority;
            version = uri.version;
        } else {
            /*
             * Usages of CharSequences.skipLeadingWhitespaces(…) and skipTrailingWhitespaces(…)
             * below will work even if code.indexOf(…) returned -1.
             */
            int afterAuthority = code.indexOf(DefaultNameSpace.DEFAULT_SEPARATOR);
            int end = CharSequences.skipTrailingWhitespaces(code, 0, afterAuthority);
            int start = CharSequences.skipLeadingWhitespaces(code, 0, end);
            if (start >= end) {
                throw new NoSuchAuthorityFactoryException(Errors.format(Errors.Keys.MissingAuthority_1, code), null);
            }
            authority = code.substring(start, end);
            int afterVersion = code.indexOf(DefaultNameSpace.DEFAULT_SEPARATOR, ++afterAuthority);
            start = CharSequences.skipLeadingWhitespaces(code, afterAuthority, afterVersion);
            end = CharSequences.skipTrailingWhitespaces(code, start, afterVersion);
            if (start < end) {
                version = code.substring(start, end);
                afterVersion++;
            } else {
                version = null;
                afterVersion = afterAuthority;
            }
        }
        return proxy.createFromAPI(getAuthorityFactory(
                AuthorityFactoryIdentifier.create(proxy.factoryType, authority, version)), code);
    }

    /**
     * Clears the cache and notifies all factories need to be fetched again from the providers given at
     * construction time. All providers that are instances of {@link ServiceLoader} will have their
     * {@link ServiceLoader#reload() reload()} method invoked.
     *
     * <p>This method is intended for use in situations in which new factories can be installed into a running
     * Java virtual machine. This method invocation will happen automatically if Apache SIS is running in a
     * servlet or OSGi container.</p>
     */
    public void reload() {
        for (int type=0; type < providers.length; type++) {
            final Iterable<?> provider = providers[type];
            if (provider != null) {
                synchronized (provider) {
                    if (provider instanceof ServiceLoader<?>) {
                        ((ServiceLoader<?>) provider).reload();
                    }
                    /*
                     * Clear the cache on a provider-by-provider basis, not by a call to factories.clear().
                     * This is needed because this MultiAuthoritiesFactory instance may be used concurrently
                     * by other threads, and we have no global lock for the whole factory.
                     */
                    final Iterator<AuthorityFactoryIdentifier> it = factories.keySet().iterator();
                    while (it.hasNext()) {
                        if (it.next().type == type) {
                            it.remove();
                        }
                    }
                    int doneMask;
                    do doneMask = iterationCompleted.get();
                    while (iterationCompleted.compareAndSet(doneMask, doneMask & ~(1 << type)));
                }
            }
        }
    }
}
